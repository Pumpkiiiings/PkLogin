/*
 * The MIT License (MIT)
 *
 * Copyright © 2020 - 2026 - PkLogin Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.pumpkiiings.pklogin.paper.packet.nativehandshake;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * The handful of Minecraft protocol primitives the login handshake needs.
 *
 * <p>Every read here parses bytes an unauthenticated client sent, so each one is
 * bounded twice: against a caller-supplied maximum, and against what the buffer
 * actually holds. A length prefix is attacker-controlled, and without the second
 * check a five-byte packet could ask for a 2 GB allocation.</p>
 *
 * <p>No dependency on Bukkit or on any packet library, so this is unit testable
 * against fixed byte sequences.</p>
 */
public final class ProtocolIO {

    /** A VarInt is 7 bits per byte, so 32 bits never needs more than 5. */
    private static final int MAX_VARINT_BYTES = 5;

    private static final int CONTINUATION_BIT = 0x80;
    private static final int VALUE_MASK = 0x7F;

    /** UTF-8 encodes a char as at most 4 bytes; used to size string reads. */
    private static final int MAX_BYTES_PER_CHAR = 4;

    private ProtocolIO() {}

    /** Thrown for anything malformed; the caller drops the connection. */
    public static final class MalformedPacketException extends RuntimeException {
        public MalformedPacketException(String message) {
            super(message);
        }
    }

    public static int readVarInt(ByteBuf buf) {
        int result = 0;
        for (int i = 0; i < MAX_VARINT_BYTES; i++) {
            if (!buf.isReadable()) {
                throw new MalformedPacketException("VarInt truncated after " + i + " byte(s)");
            }

            byte read = buf.readByte();
            result |= (read & VALUE_MASK) << (i * 7);

            if ((read & CONTINUATION_BIT) == 0) {
                return result;
            }
        }
        throw new MalformedPacketException("VarInt longer than " + MAX_VARINT_BYTES + " bytes");
    }

    public static void writeVarInt(ByteBuf buf, int value) {
        // >>> rather than >>: a negative value must still terminate.
        while ((value & ~VALUE_MASK) != 0) {
            buf.writeByte((value & VALUE_MASK) | CONTINUATION_BIT);
            value >>>= 7;
        }
        buf.writeByte(value);
    }

    public static int varIntSize(int value) {
        int bytes = 1;
        while ((value & ~VALUE_MASK) != 0) {
            bytes++;
            value >>>= 7;
        }
        return bytes;
    }

    /**
     * @param maxChars the protocol's declared limit for this field, in characters
     */
    public static String readString(ByteBuf buf, int maxChars) {
        int length = readLength(buf, maxChars * MAX_BYTES_PER_CHAR, "string");

        String value = buf.toString(buf.readerIndex(), length, StandardCharsets.UTF_8);
        buf.skipBytes(length);

        // Checked after decoding as well: the byte budget above is the worst case,
        // and a string of cheap characters could still exceed the character limit.
        if (value.length() > maxChars) {
            throw new MalformedPacketException(
                    "string of " + value.length() + " chars exceeds limit " + maxChars);
        }
        return value;
    }

    public static void writeString(ByteBuf buf, String value) {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(buf, encoded.length);
        buf.writeBytes(encoded);
    }

    public static byte[] readByteArray(ByteBuf buf, int maxLength) {
        int length = readLength(buf, maxLength, "byte array");

        byte[] value = new byte[length];
        buf.readBytes(value);
        return value;
    }

    public static void writeByteArray(ByteBuf buf, byte[] value) {
        writeVarInt(buf, value.length);
        buf.writeBytes(value);
    }

    /**
     * Reads a length prefix and refuses it unless the buffer can actually satisfy
     * it. Allocating from the prefix alone is what turns a malformed packet into
     * an out-of-memory condition.
     */
    private static int readLength(ByteBuf buf, int maxLength, String what) {
        int length = readVarInt(buf);

        if (length < 0) {
            throw new MalformedPacketException("negative " + what + " length: " + length);
        }
        if (length > maxLength) {
            throw new MalformedPacketException(
                    what + " length " + length + " exceeds limit " + maxLength);
        }
        if (length > buf.readableBytes()) {
            throw new MalformedPacketException(
                    what + " length " + length + " exceeds the " + buf.readableBytes()
                            + " byte(s) left in the packet");
        }
        return length;
    }
}
