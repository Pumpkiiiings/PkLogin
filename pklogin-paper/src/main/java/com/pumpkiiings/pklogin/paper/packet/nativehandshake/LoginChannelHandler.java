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
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.NoSuchElementException;

/**
 * Runs Mojang's encryption handshake for one connection, without a packet
 * library.
 *
 * <p>Four packets are all this needs, and their ids have not moved in years: the
 * handshake (0x00), serverbound login start (0x00), clientbound encryption
 * request (0x01) and serverbound encryption response (0x01).</p>
 *
 * <p>Version differences are handled without configuring anything. The client
 * states its protocol version in the handshake, which arrives first, so the two
 * places the exchange changed shape — the boolean 1.20.5 appended to the
 * encryption request, and the signed nonce 1.19 to 1.19.2 could answer with —
 * are decided per connection. The rest is skipped rather than parsed: only the
 * leading username is read out of the login packet, so the signature data 1.19
 * appended and the UUID 1.20.2 added never have to be understood.</p>
 *
 * <h2>Where this must sit</h2>
 *
 * <p>Directly after the frame splitter and before the vanilla decoder, so it
 * sees one whole packet per read, still as bytes. Outbound writes are framed
 * here rather than left to the vanilla prepender, because a write issued from
 * this position travels toward the head and never reaches it. At login time,
 * before compression or encryption are switched on, nothing else is in the way.
 * </p>
 *
 * <p>One instance per connection: it holds that connection's state.</p>
 */
public class LoginChannelHandler extends ChannelInboundHandlerAdapter {

    private static final int PACKET_HANDSHAKE = 0x00;
    private static final int PACKET_LOGIN_START = 0x00;
    private static final int PACKET_ENCRYPTION_RESPONSE = 0x01;
    private static final int PACKET_ENCRYPTION_REQUEST = 0x01;

    private static final int MAX_USERNAME_CHARS = 16;

    /** Hostname in the handshake; generous because proxies append to it. */
    private static final int MAX_HANDSHAKE_HOST_CHARS = 255;

    private static final int NEXT_STATE_LOGIN = 2;

    /**
     * 1.19 (759) through 1.19.2 (760) could answer the encryption request with a
     * nonce signed by the client's profile key instead of an encrypted one.
     */
    private static final int PROTOCOL_FIRST_SIGNED_NONCE = 759;
    private static final int PROTOCOL_LAST_SIGNED_NONCE = 760;

    /** 1.20.5 (766) appended a boolean to the encryption request. */
    private static final int PROTOCOL_AUTHENTICATE_FLAG = 766;

    /**
     * Our key is RSA-1024, so an encrypted secret or nonce is 128 bytes and a
     * client signature 256. Bounded well above that but nowhere near a size worth
     * allocating on a stranger's say-so.
     */
    private static final int MAX_CRYPTO_FIELD = 512;

    private enum State {
        EXPECT_HANDSHAKE,
        EXPECT_LOGIN_START,
        /** Waiting on the account store, with the login packet held back. */
        DECIDING,
        EXPECT_ENCRYPTION_RESPONSE,
        /** Verification is in flight; nothing else should be read meanwhile. */
        VERIFYING,
        PASSTHROUGH
    }

    private final HandshakePolicy policy;
    private final PublicKey publicKey;
    private final SecureRandom random;

    private State state = State.EXPECT_HANDSHAKE;
    private int protocolVersion;
    private String username;
    private String ip;
    private byte[] verifyToken;
    private ByteBuf loginStart;

    public LoginChannelHandler(HandshakePolicy policy, PublicKey publicKey, SecureRandom random) {
        this.policy = policy;
        this.publicKey = publicKey;
        this.random = random;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (state == State.PASSTHROUGH || !(msg instanceof ByteBuf)) {
            ctx.fireChannelRead(msg);
            return;
        }

        ByteBuf in = (ByteBuf) msg;
        in.markReaderIndex();

        boolean forward;
        try {
            forward = dispatch(ctx, in);
        } catch (ProtocolIO.MalformedPacketException ex) {
            in.release();
            fail(ctx, ex.getMessage());
            return;
        } catch (Exception ex) {
            in.release();
            fail(ctx, "unexpected error reading a login packet: " + ex);
            return;
        }

        if (forward) {
            in.resetReaderIndex();
            ctx.fireChannelRead(in);
        } else {
            in.release();
        }
    }

    /** @return true to pass the packet along untouched */
    private boolean dispatch(ChannelHandlerContext ctx, ByteBuf in) {
        int packetId = ProtocolIO.readVarInt(in);

        if (state == State.EXPECT_HANDSHAKE && packetId == PACKET_HANDSHAKE) {
            return handleHandshake(ctx, in);
        }
        if (state == State.EXPECT_LOGIN_START && packetId == PACKET_LOGIN_START) {
            return handleLoginStart(ctx, in);
        }
        if (state == State.EXPECT_ENCRYPTION_RESPONSE && packetId == PACKET_ENCRYPTION_RESPONSE) {
            return handleEncryptionResponse(ctx, in);
        }

        // Anything else at this stage is the vanilla flow's business, except while
        // the exchange is mid-flight: a client that keeps talking then is out of
        // step with the handshake it was asked for, and letting the packet past
        // would reorder it around the one being held back.
        if (state == State.DECIDING || state == State.VERIFYING) {
            fail(ctx, "packet 0x" + Integer.toHexString(packetId) + " arrived mid-handshake");
            return false;
        }
        return true;
    }

    /**
     * Reads the protocol version out of the handshake and passes it on untouched.
     *
     * <p>This is what makes the two version-dependent bits of the exchange a fact
     * rather than a guess: the client states its protocol version here, before any
     * login packet, so nothing downstream has to be configured or sniffed.</p>
     */
    private boolean handleHandshake(ChannelHandlerContext ctx, ByteBuf in) {
        this.protocolVersion = ProtocolIO.readVarInt(in);
        ProtocolIO.readString(in, MAX_HANDSHAKE_HOST_CHARS);
        in.readUnsignedShort();
        int nextState = ProtocolIO.readVarInt(in);

        if (nextState != NEXT_STATE_LOGIN) {
            // A server list ping. Nothing here concerns it.
            return detachAndForward(ctx);
        }

        state = State.EXPECT_LOGIN_START;
        return true;
    }

    /** 1.20.5 and later expect a trailing boolean on the encryption request. */
    private boolean sendsAuthenticateFlag() {
        return protocolVersion >= PROTOCOL_AUTHENTICATE_FLAG;
    }

    /** 1.19 to 1.19.2 may answer with a signed nonce rather than an encrypted one. */
    private boolean mayAnswerWithSignedNonce() {
        return protocolVersion >= PROTOCOL_FIRST_SIGNED_NONCE
                && protocolVersion <= PROTOCOL_LAST_SIGNED_NONCE;
    }

    private boolean handleLoginStart(ChannelHandlerContext ctx, ByteBuf in) {
        this.username = ProtocolIO.readString(in, MAX_USERNAME_CHARS);
        this.ip = resolveIp(ctx);

        if (policy.isFloodgate(ctx.channel())) {
            return detachAndForward(ctx);
        }
        if (ip == null) {
            // Verified sessions are keyed by address; with none there is nothing to
            // store a result against, so leave the connection to the password path.
            return detachAndForward(ctx);
        }
        if (policy.hasVerifiedSession(ip, username)) {
            return detachAndForward(ctx);
        }

        // Held rather than rebuilt, so the client's own packet is what eventually
        // reaches the server and no version-specific shape has to be reproduced.
        in.resetReaderIndex();
        this.loginStart = in.copy();
        this.state = State.DECIDING;

        policy.decide(username, ip, requestEncryption -> onDecision(ctx, requestEncryption));
        return false;
    }

    /**
     * Acts on the account store's answer, back on the connection's own thread.
     *
     * <p>A no is indistinguishable from never having intercepted: the client's
     * packet goes on exactly as it arrived.</p>
     */
    private void onDecision(ChannelHandlerContext ctx, boolean requestEncryption) {
        if (!ctx.executor().inEventLoop()) {
            ctx.executor().execute(() -> onDecision(ctx, requestEncryption));
            return;
        }

        if (state != State.DECIDING) {
            // The connection went away while the answer was in flight.
            releaseCaptured();
            return;
        }

        if (!requestEncryption) {
            ByteBuf captured = loginStart;
            loginStart = null;
            state = State.PASSTHROUGH;
            removeSelf(ctx);
            ctx.fireChannelRead(captured);
            return;
        }

        this.verifyToken = new byte[4];
        random.nextBytes(verifyToken);

        state = State.EXPECT_ENCRYPTION_RESPONSE;
        sendEncryptionRequest(ctx);
    }

    private boolean handleEncryptionResponse(ChannelHandlerContext ctx, ByteBuf in) {
        byte[] sharedSecret = ProtocolIO.readByteArray(in, MAX_CRYPTO_FIELD);
        byte[] encryptedToken = readResponseNonce(in);

        this.state = State.VERIFYING;

        ByteBuf captured = this.loginStart;
        this.loginStart = null;

        policy.onEncryptionResponse(new HandshakeExchange(
                ctx, username, ip, verifyToken, sharedSecret, encryptedToken, captured));
        return false;
    }

    /**
     * Reads whichever proof of the nonce this client version sends.
     *
     * <p>1.19 through 1.19.2 could sign the nonce with the client's own profile
     * key instead of encrypting it. That path is not accepted: verifying it needs
     * the profile key, which arrives in the part of the login packet deliberately
     * skipped here. Those clients fall through to a password.</p>
     */
    private byte[] readResponseNonce(ByteBuf in) {
        if (!mayAnswerWithSignedNonce()) {
            return ProtocolIO.readByteArray(in, MAX_CRYPTO_FIELD);
        }

        boolean hasVerifyToken = in.readBoolean();
        if (!hasVerifyToken) {
            throw new ProtocolIO.MalformedPacketException(
                    "client answered with a signed nonce, which this handshake does not accept");
        }
        return ProtocolIO.readByteArray(in, MAX_CRYPTO_FIELD);
    }

    private void sendEncryptionRequest(ChannelHandlerContext ctx) {
        byte[] encodedKey = publicKey.getEncoded();

        ByteBuf body = ctx.alloc().buffer();
        try {
            ProtocolIO.writeVarInt(body, PACKET_ENCRYPTION_REQUEST);
            // Empty server id, matching what an online-mode server sends; the
            // session hash is computed over it on both sides.
            ProtocolIO.writeString(body, "");
            ProtocolIO.writeByteArray(body, encodedKey);
            ProtocolIO.writeByteArray(body, verifyToken);
            if (sendsAuthenticateFlag()) {
                body.writeBoolean(true);
            }

            ByteBuf framed = ctx.alloc().buffer(ProtocolIO.varIntSize(body.readableBytes())
                    + body.readableBytes());
            ProtocolIO.writeVarInt(framed, body.readableBytes());
            framed.writeBytes(body);

            ctx.writeAndFlush(framed);
        } finally {
            body.release();
        }
    }

    /**
     * Puts the captured login packet back into the pipeline and steps out of the
     * way. Called once verification has succeeded.
     */
    public void resume(ChannelHandlerContext ctx, ByteBuf capturedLoginStart) {
        if (ctx.executor().inEventLoop()) {
            doResume(ctx, capturedLoginStart);
        } else {
            ctx.executor().execute(() -> doResume(ctx, capturedLoginStart));
        }
    }

    private void doResume(ChannelHandlerContext ctx, ByteBuf capturedLoginStart) {
        state = State.PASSTHROUGH;
        removeSelf(ctx);
        ctx.fireChannelRead(capturedLoginStart);
    }

    private boolean detachAndForward(ChannelHandlerContext ctx) {
        state = State.PASSTHROUGH;
        removeSelf(ctx);
        return true;
    }

    private void removeSelf(ChannelHandlerContext ctx) {
        try {
            ctx.pipeline().remove(this);
        } catch (NoSuchElementException already) {
            // Removed by a channel that closed underneath us; nothing to undo.
        }
    }

    private void fail(ChannelHandlerContext ctx, String reason) {
        state = State.PASSTHROUGH;
        releaseCaptured();
        policy.onProtocolError(ip, reason);
        ctx.close();
    }

    private String resolveIp(ChannelHandlerContext ctx) {
        if (!(ctx.channel().remoteAddress() instanceof InetSocketAddress)) {
            return null;
        }
        InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
        return address.getAddress() == null ? null : address.getAddress().getHostAddress();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        releaseCaptured();
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        releaseCaptured();
        policy.onProtocolError(ip, "connection error: " + cause);
        ctx.close();
    }

    /** A connection dropped mid-handshake still holds the packet it was sent. */
    private void releaseCaptured() {
        if (loginStart != null) {
            ReferenceCountUtil.release(loginStart);
            loginStart = null;
        }
    }
}
