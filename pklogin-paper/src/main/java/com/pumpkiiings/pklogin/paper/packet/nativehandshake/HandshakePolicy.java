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

import io.netty.channel.Channel;

/**
 * Everything {@link LoginChannelHandler} needs from the rest of the plugin.
 *
 * <p>Kept as an interface so the handler can be exercised against fixed byte
 * sequences without a running server: the protocol work is deterministic, while
 * the decisions behind it involve the database, Mojang and Bukkit.</p>
 */
public interface HandshakePolicy {

    /**
     * Works out whether to send the encryption request for this name, and reports
     * back through {@code callback}.
     *
     * <p>Asynchronous because the answer needs the account store and sometimes a
     * call to Mojang. Doing that inline would block a Netty event loop, and one
     * loop carries many connections — a slow lookup for one player would stall
     * everyone who happened to land on the same thread.</p>
     *
     * <p>The connection is held meanwhile: the client's login packet is kept back
     * and replayed once the answer arrives, so nothing reaches the server in the
     * wrong order. The callback may fire on any thread.</p>
     */
    void decide(String username, String ip, java.util.function.Consumer<Boolean> callback);

    /**
     * Whether this address already holds a verified session for the name, in
     * which case the handshake has been done and the connection passes straight
     * through.
     */
    boolean hasVerifiedSession(String ip, String username);

    /** Bedrock players are authenticated by Floodgate and never see this flow. */
    boolean isFloodgate(Channel channel);

    /**
     * Hands a completed encryption response off for verification.
     *
     * <p>The implementation owns everything after this point: talking to Mojang,
     * enabling encryption on the connection, and replaying {@code loginStart} once
     * the player checks out. {@code loginStart} is the client's own login packet,
     * captured verbatim, and ownership of it transfers with this call — the
     * implementation must release it.</p>
     */
    void onEncryptionResponse(HandshakeExchange exchange);

    /**
     * Reports a connection dropped for a malformed or unexpected packet. Only for
     * visibility; the handler has already closed the channel.
     */
    void onProtocolError(String ip, String reason);
}
