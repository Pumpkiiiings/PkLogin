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

package com.pumpkiiings.pklogin.velocity;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.pumpkiiings.pklogin.common.PluginConstants;
import com.pumpkiiings.pklogin.common.security.ProxyMessageSecurity;
import com.pumpkiiings.pklogin.common.security.ProxyVerifyProtocol;
import com.pumpkiiings.pklogin.velocity.listener.PluginMessageListener;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Confirms that the authentication servers really run PkLogin, and that they
 * resolved the same signing key as the proxy.
 *
 * <p>Without it a key mismatch fails silently: the proxy sends an auto-login the
 * backend drops, and the only trace is a line in the backend's console that the
 * proxy's administrator never reads.</p>
 *
 * <p>Each server is checked the first time a player reaches it — plugin messages
 * need a live connection to travel over — and the result stands until reload.</p>
 */
public final class BackendVerification {

    private static final class Pending {
        private final String challenge;
        private final long sentAt;

        private Pending(String challenge, long sentAt) {
            this.challenge = challenge;
            this.sentAt = sentAt;
        }
    }

    private final PkLoginVelocity plugin;

    /** Servers with a check in flight, by the name the proxy knows them as. */
    private final Map<String, Pending> pending = new ConcurrentHashMap<>();

    /** Servers that have already answered correctly; not checked again. */
    private final Set<String> verified = ConcurrentHashMap.newKeySet();

    public BackendVerification(PkLoginVelocity plugin) {
        this.plugin = plugin;
    }

    /** Forgets every result: a reload is when the key or the server list may have changed. */
    public void reset() {
        pending.clear();
        verified.clear();
    }

    /**
     * Warns about names in {@code auth-servers} the proxy does not know. A typo
     * there silently disables the redirect to the login server.
     */
    public void reportUnknownAuthServers() {
        for (String name : plugin.getBackendConfig().getAuthServers()) {
            if (!plugin.getServer().getServer(name).isPresent()) {
                plugin.getLogger().error("'{}' is listed under backend.auth-servers but no server with "
                        + "that name exists in velocity.toml. Players cannot be sent there to log in.", name);
            }
        }
    }

    /** Sends a check, unless switched off, already answered or still waiting on one. */
    public void checkIfNeeded(RegisteredServer server) {
        if (!plugin.getBackendConfig().isVerifyConnection()) return;

        String name = server.getServerInfo().getName();
        if (!plugin.getBackendConfig().getAuthServers().contains(name)) return;
        if (verified.contains(name) || pending.containsKey(name)) return;

        String secret = plugin.getProxySecret();
        if (secret == null || secret.isEmpty()) {
            // Startup already explained this at length; a second copy per backend
            // would only bury it.
            return;
        }

        long timestamp = System.currentTimeMillis();
        String nonce = ProxyMessageSecurity.newNonce();
        String signature = ProxyMessageSecurity.sign(secret,
                ProxyVerifyProtocol.requestParts(name, timestamp, nonce));

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(PluginConstants.SUBCHANNEL_VERIFY);
        out.writeUTF(name);
        out.writeLong(timestamp);
        out.writeUTF(nonce);
        out.writeUTF(signature);

        if (!server.sendPluginMessage(PluginMessageListener.IDENTIFIER, out.toByteArray())) {
            // The player disconnected between arriving and this running. Nothing
            // is in flight, so the next arrival tries again.
            return;
        }

        pending.put(name, new Pending(nonce, timestamp));

        plugin.getServer().getScheduler()
                .buildTask(plugin, () -> reportTimeout(name, nonce))
                .delay(ProxyVerifyProtocol.REPLY_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .schedule();
    }

    /**
     * Handles a backend's answer.
     *
     * @param in the payload, positioned just after the sub-channel
     */
    public void handleReply(String serverName, ByteArrayDataInput in) {
        final String echoedName;
        final String version;
        final String challenge;
        final long timestamp;
        final String nonce;
        final String signature;
        try {
            echoedName = in.readUTF();
            version = in.readUTF();
            challenge = in.readUTF();
            timestamp = in.readLong();
            nonce = in.readUTF();
            signature = in.readUTF();
        } catch (IllegalStateException malformed) {
            return;
        }

        // Nothing pending means a duplicate answer, or one that arrived after the
        // check had been given up on.
        Pending expected = pending.get(serverName);
        if (expected == null) return;

        String secret = plugin.getProxySecret();
        boolean authentic = ProxyMessageSecurity.verify(secret, signature, timestamp, nonce,
                ProxyVerifyProtocol.replyParts(echoedName, version, challenge, timestamp, nonce));

        if (!authentic || !serverName.equals(echoedName)
                || !ProxyMessageSecurity.constantTimeEquals(expected.challenge, challenge)) {
            pending.remove(serverName);
            plugin.getLogger().error("=========================================================");
            plugin.getLogger().error("'{}' answered the connection check with a signature this proxy", serverName);
            plugin.getLogger().error("cannot verify. The two ends are not using the same signing key.");
            plugin.getLogger().error("Both must derive it from the same Velocity forwarding secret:");
            plugin.getLogger().error("check that the file named by 'forwarding-secret-file' in velocity.toml");
            plugin.getLogger().error("matches 'proxies.velocity.secret' in that server's config/paper-global.yml.");
            plugin.getLogger().error("Premium auto-login will not work on '{}' until it does.", serverName);
            plugin.getLogger().error("=========================================================");
            return;
        }

        pending.remove(serverName);
        verified.add(serverName);

        long roundTrip = System.currentTimeMillis() - expected.sentAt;
        plugin.getLogger().info("Backend '{}' verified: PkLogin {}, matching signing key ({} ms).",
                serverName, version, roundTrip);

        String proxyVersion = proxyVersion();
        if (proxyVersion != null && !proxyVersion.equals(version)) {
            plugin.getLogger().warn("'{}' runs PkLogin {} while this proxy runs {}. Run the same version "
                    + "on both ends; the messages they exchange are not guaranteed to survive a version gap.",
                    serverName, version, proxyVersion);
        }
    }

    /** Reports a check that was never answered, if it is still the current one. */
    private void reportTimeout(String serverName, String challenge) {
        Pending stillWaiting = pending.get(serverName);
        if (stillWaiting == null || !stillWaiting.challenge.equals(challenge)) {
            return;
        }
        pending.remove(serverName);

        plugin.getLogger().error("=========================================================");
        plugin.getLogger().error("'{}' did not answer PkLogin's connection check.", serverName);
        plugin.getLogger().error("It is listed under backend.auth-servers, so players are sent there to log in,");
        plugin.getLogger().error("but nothing there replied. Usually this means PkLogin is not installed on it.");
        plugin.getLogger().error("If that server intentionally runs without PkLogin (a limbo, for instance),");
        plugin.getLogger().error("remove it from backend.auth-servers or set backend.verify-connection to false.");
        plugin.getLogger().error("=========================================================");
    }

    /** @return the proxy's own plugin version, or null if it cannot be read */
    private String proxyVersion() {
        return plugin.getServer().getPluginManager().getPlugin("pklogin")
                .flatMap(container -> container.getDescription().getVersion())
                .orElse(null);
    }
}
