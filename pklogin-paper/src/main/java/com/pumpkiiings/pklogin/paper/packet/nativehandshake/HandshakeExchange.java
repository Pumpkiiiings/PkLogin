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
import io.netty.channel.ChannelHandlerContext;

/**
 * One client's completed encryption response, with everything needed to verify
 * it and let the connection carry on.
 */
public final class HandshakeExchange {

    private final ChannelHandlerContext context;
    private final String username;
    private final String ip;
    private final byte[] verifyToken;
    private final byte[] encryptedSharedSecret;
    private final byte[] encryptedVerifyToken;
    private final ByteBuf loginStart;

    public HandshakeExchange(ChannelHandlerContext context, String username, String ip,
                             byte[] verifyToken, byte[] encryptedSharedSecret,
                             byte[] encryptedVerifyToken, ByteBuf loginStart) {
        this.context = context;
        this.username = username;
        this.ip = ip;
        this.verifyToken = verifyToken;
        this.encryptedSharedSecret = encryptedSharedSecret;
        this.encryptedVerifyToken = encryptedVerifyToken;
        this.loginStart = loginStart;
    }

    public ChannelHandlerContext getContext() {
        return context;
    }

    public String getUsername() {
        return username;
    }

    public String getIp() {
        return ip;
    }

    /** The token this server generated, to compare against what came back. */
    public byte[] getVerifyToken() {
        return verifyToken;
    }

    public byte[] getEncryptedSharedSecret() {
        return encryptedSharedSecret;
    }

    public byte[] getEncryptedVerifyToken() {
        return encryptedVerifyToken;
    }

    /**
     * The client's own login packet, held back rather than rebuilt.
     *
     * <p>Replaying the original is what keeps this version independent: the shape
     * of a login packet has changed repeatedly — signature data in 1.19, a UUID in
     * 1.20.2 — and none of that matters if the bytes are never regenerated. The
     * premium UUID is carried in the verified session instead, which is all the
     * join listener reads.</p>
     *
     * <p>Owned by whoever receives the exchange, and must be released.</p>
     */
    public ByteBuf getLoginStart() {
        return loginStart;
    }
}
