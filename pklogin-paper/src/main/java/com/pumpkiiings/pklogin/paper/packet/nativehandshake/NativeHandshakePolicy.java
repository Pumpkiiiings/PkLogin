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

import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import com.pumpkiiings.pklogin.paper.manager.PremiumManager;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.security.KeyPair;
import java.util.function.Consumer;

/**
 * Connects the native handshake to the rest of the plugin.
 *
 * <p>Deliberately the only class here that knows about accounts, Floodgate or
 * Bukkit: the protocol side stays testable because every decision it needs
 * arrives through this interface.</p>
 */
public final class NativeHandshakePolicy implements HandshakePolicy {

    private static final AttributeKey<?> FLOODGATE_PLAYER = AttributeKey.valueOf("floodgate-player");

    private final PkLoginPaper plugin;
    private final KeyPair keyPair;

    public NativeHandshakePolicy(PkLoginPaper plugin, KeyPair keyPair) {
        this.plugin = plugin;
        this.keyPair = keyPair;
    }

    @Override
    public void decide(String username, String ip, Consumer<Boolean> callback) {
        plugin.runAsync(() -> {
            boolean requestEncryption;
            try {
                requestEncryption = PremiumManager.shouldRequestEncryption(plugin, username);
            } catch (Throwable ex) {
                // A lookup that failed is not a reason to challenge someone. Saying
                // yes here and being wrong costs the player their connection, with
                // no password prompt to fall back to; saying no costs a password.
                plugin.getLogger().warning("Native handshake: premium check failed for "
                        + username + " (" + ex + "). Treating as not premium.");
                requestEncryption = false;
            }
            callback.accept(requestEncryption);
        });
    }

    @Override
    public boolean hasVerifiedSession(String ip, String username) {
        return plugin.hasVerifiedSession(ip, username);
    }

    @Override
    public boolean isFloodgate(Channel channel) {
        try {
            return channel.hasAttr(FLOODGATE_PLAYER);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public void onEncryptionResponse(HandshakeExchange exchange) {
        plugin.runAsync(new NativeVerifyTask(plugin, exchange, keyPair, handlerOf(exchange)));
    }

    @Override
    public void onProtocolError(String ip, String reason) {
        plugin.getLogger().fine("Native handshake dropped a connection from " + ip + ": " + reason);
    }

    /**
     * The handler that raised this exchange is the one that has to put the
     * connection back together, and it is still in the pipeline under its own
     * name.
     */
    private LoginChannelHandler handlerOf(HandshakeExchange exchange) {
        return (LoginChannelHandler) exchange.getContext().handler();
    }
}
