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

package com.pumpkiiings.pklogin.paper;

import com.pumpkiiings.pklogin.paper.packet.nativehandshake.ProtocolIO;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

/**
 * Checks the protocol primitives the native premium handshake reads from
 * unauthenticated clients.
 *
 * <p>Two things are being proven. That the encodings match the wire format, by
 * comparing against byte sequences taken from the protocol specification rather
 * than from this implementation — a round trip alone would pass just as happily
 * on a self-consistent but wrong encoding. And that every hostile length a
 * client can send is refused, since these run before anyone has authenticated.
 * </p>
 */
public final class ProtocolIoTest {

    private static int checks;
    private static int failures;

    public static void main(String[] args) {
        varIntMatchesTheWireFormat();
        varIntRoundTrips();
        varIntSizeAgreesWithWhatIsWritten();
        varIntRejectsOverlongEncodings();
        varIntRejectsTruncatedInput();

        stringRoundTripsIncludingNonAscii();
        stringRejectsAnOverlongDeclaredLength();
        stringRejectsALengthBeyondTheBuffer();

        byteArrayRoundTrips();
        byteArrayRejectsNegativeLength();
        byteArrayRejectsALengthBeyondTheBuffer();
        byteArrayRejectsALengthOverTheCap();

        System.out.println();
        System.out.println(checks + " check(s), " + failures + " failure(s)");
        if (failures > 0) System.exit(1);
        System.out.println("Protocol primitives read and write the wire format safely.");
    }

    /** Values from the protocol specification, not from this implementation. */
    private static void varIntMatchesTheWireFormat() {
        checkVarIntEncodes(0, new byte[]{0x00});
        checkVarIntEncodes(1, new byte[]{0x01});
        checkVarIntEncodes(127, new byte[]{0x7f});
        checkVarIntEncodes(128, new byte[]{(byte) 0x80, 0x01});
        checkVarIntEncodes(255, new byte[]{(byte) 0xff, 0x01});
        checkVarIntEncodes(2097151, new byte[]{(byte) 0xff, (byte) 0xff, 0x7f});
        checkVarIntEncodes(2147483647,
                new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x07});
        checkVarIntEncodes(-1,
                new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x0f});
    }

    private static void checkVarIntEncodes(int value, byte[] expected) {
        ByteBuf buf = Unpooled.buffer();
        try {
            ProtocolIO.writeVarInt(buf, value);

            byte[] actual = new byte[buf.readableBytes()];
            buf.getBytes(0, actual);
            check("VarInt " + value + " encodes to " + hex(expected), java.util.Arrays.equals(expected, actual));
            check("VarInt " + value + " reads back", ProtocolIO.readVarInt(buf) == value);
        } finally {
            buf.release();
        }
    }

    private static void varIntRoundTrips() {
        int[] values = {0, 1, 2, 127, 128, 129, 255, 256, 16383, 16384,
                2097151, 2097152, Integer.MAX_VALUE, Integer.MIN_VALUE, -1, -128};

        boolean allMatch = true;
        for (int value : values) {
            ByteBuf buf = Unpooled.buffer();
            try {
                ProtocolIO.writeVarInt(buf, value);
                if (ProtocolIO.readVarInt(buf) != value) allMatch = false;
                if (buf.isReadable()) allMatch = false;
            } finally {
                buf.release();
            }
        }
        check("every VarInt round trips and consumes exactly its own bytes", allMatch);
    }

    /** The framing header is sized before the body is written, so this must agree. */
    private static void varIntSizeAgreesWithWhatIsWritten() {
        int[] values = {0, 127, 128, 16383, 16384, 2097151, Integer.MAX_VALUE, -1};

        boolean allMatch = true;
        for (int value : values) {
            ByteBuf buf = Unpooled.buffer();
            try {
                ProtocolIO.writeVarInt(buf, value);
                if (buf.readableBytes() != ProtocolIO.varIntSize(value)) allMatch = false;
            } finally {
                buf.release();
            }
        }
        check("varIntSize matches the bytes actually written", allMatch);
    }

    /** Six continuation bytes is a client trying to walk off the end of the buffer. */
    private static void varIntRejectsOverlongEncodings() {
        ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{
                (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, 0x01});
        try {
            check("VarInt longer than five bytes is refused", refuses(() -> ProtocolIO.readVarInt(buf)));
        } finally {
            buf.release();
        }
    }

    private static void varIntRejectsTruncatedInput() {
        ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{(byte) 0x80});
        try {
            check("VarInt cut short is refused", refuses(() -> ProtocolIO.readVarInt(buf)));
        } finally {
            buf.release();
        }
    }

    private static void stringRoundTripsIncludingNonAscii() {
        ByteBuf buf = Unpooled.buffer();
        try {
            ProtocolIO.writeString(buf, "Pumpkiiiings");
            check("ASCII name round trips", "Pumpkiiiings".equals(ProtocolIO.readString(buf, 16)));

            ProtocolIO.writeString(buf, "áé中");
            check("multi-byte characters round trip", "áé中".equals(ProtocolIO.readString(buf, 16)));

            ProtocolIO.writeString(buf, "");
            check("empty string round trips", "".equals(ProtocolIO.readString(buf, 16)));
        } finally {
            buf.release();
        }
    }

    /**
     * A username is capped at 16 characters. Nothing stops a client declaring
     * more, and the read has to be the thing that says no.
     */
    private static void stringRejectsAnOverlongDeclaredLength() {
        ByteBuf buf = Unpooled.buffer();
        try {
            ProtocolIO.writeString(buf, repeat('a', 300));
            check("string past the character limit is refused",
                    refuses(() -> ProtocolIO.readString(buf, 16)));
        } finally {
            buf.release();
        }
    }

    /** The prefix says 4 KB, the packet holds two bytes. Nothing gets allocated. */
    private static void stringRejectsALengthBeyondTheBuffer() {
        ByteBuf buf = Unpooled.buffer();
        try {
            ProtocolIO.writeVarInt(buf, 4096);
            buf.writeBytes("hi".getBytes(StandardCharsets.UTF_8));
            check("string longer than the packet is refused",
                    refuses(() -> ProtocolIO.readString(buf, 16)));
        } finally {
            buf.release();
        }
    }

    private static void byteArrayRoundTrips() {
        byte[] secret = new byte[128];
        for (int i = 0; i < secret.length; i++) secret[i] = (byte) i;

        ByteBuf buf = Unpooled.buffer();
        try {
            ProtocolIO.writeByteArray(buf, secret);
            check("encrypted secret round trips",
                    java.util.Arrays.equals(secret, ProtocolIO.readByteArray(buf, 512)));

            ProtocolIO.writeByteArray(buf, new byte[0]);
            check("empty byte array round trips",
                    ProtocolIO.readByteArray(buf, 512).length == 0);
        } finally {
            buf.release();
        }
    }

    /** A VarInt carries the sign bit, so a length can arrive negative. */
    private static void byteArrayRejectsNegativeLength() {
        ByteBuf buf = Unpooled.buffer();
        try {
            ProtocolIO.writeVarInt(buf, -1);
            buf.writeBytes(new byte[]{1, 2, 3, 4});
            check("negative byte array length is refused",
                    refuses(() -> ProtocolIO.readByteArray(buf, 512)));
        } finally {
            buf.release();
        }
    }

    /** The one that would otherwise be a 2 GB allocation from a tiny packet. */
    private static void byteArrayRejectsALengthBeyondTheBuffer() {
        ByteBuf buf = Unpooled.buffer();
        try {
            ProtocolIO.writeVarInt(buf, Integer.MAX_VALUE);
            buf.writeBytes(new byte[]{1, 2, 3, 4});
            check("byte array longer than the packet is refused",
                    refuses(() -> ProtocolIO.readByteArray(buf, Integer.MAX_VALUE)));
        } finally {
            buf.release();
        }
    }

    private static void byteArrayRejectsALengthOverTheCap() {
        ByteBuf buf = Unpooled.buffer();
        try {
            ProtocolIO.writeByteArray(buf, new byte[600]);
            check("byte array over the caller's cap is refused",
                    refuses(() -> ProtocolIO.readByteArray(buf, 512)));
        } finally {
            buf.release();
        }
    }

    private static boolean refuses(Runnable read) {
        try {
            read.run();
            return false;
        } catch (ProtocolIO.MalformedPacketException expected) {
            return true;
        } catch (RuntimeException other) {
            System.out.println("    (refused, but with " + other.getClass().getSimpleName() + ")");
            return false;
        }
    }

    private static String repeat(char c, int times) {
        StringBuilder sb = new StringBuilder(times);
        for (int i = 0; i < times; i++) sb.append(c);
        return sb.toString();
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x ", b));
        return sb.toString().trim();
    }

    private static void check(String what, boolean ok) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("  FAIL  " + what);
        } else {
            System.out.println("  ok    " + what);
        }
    }
}
