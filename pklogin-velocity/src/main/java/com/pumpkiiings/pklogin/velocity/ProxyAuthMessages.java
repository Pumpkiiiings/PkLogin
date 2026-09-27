package com.pumpkiiings.pklogin.velocity;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.pumpkiiings.pklogin.common.PluginConstants;
import com.pumpkiiings.pklogin.common.security.ProxyMessageSecurity;
import com.pumpkiiings.pklogin.common.security.ProxyAuthStateProtocol;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.pumpkiiings.pklogin.velocity.listener.PluginMessageListener;

/**
 * Builds the signed messages the proxy sends to backend servers.
 *
 * <p>Every producer of a {@code PremiumAutoLogin} payload has to sign it with the
 * exact field order the backend verifies, so the layout lives here rather than
 * being written out at each call site.</p>
 */
public final class ProxyAuthMessages {

    private ProxyAuthMessages() {}

    /**
     * Tells the player's current backend server to authenticate them.
     *
     * @return false if no shared secret is configured, in which case nothing was
     *         sent and the backend would have rejected the message anyway
     */
    public static boolean sendPremiumAutoLogin(PkLoginVelocity plugin, Player player) {
        String secret = plugin.getProxySecret();
        if (secret == null || secret.isEmpty()) {
            // The reason was already explained in full at startup; keep this short.
            plugin.getLogger().warn("{} cannot be auto-logged in: no proxy signing key available.",
                    player.getUsername());
            return false;
        }

        String username = player.getUsername();
        String uuid = player.getUniqueId().toString();
        long timestamp = System.currentTimeMillis();
        String nonce = ProxyMessageSecurity.newNonce();
        String signature = ProxyMessageSecurity.sign(secret,
                PluginConstants.SUBCHANNEL_PREMIUM_AUTO_LOGIN,
                username, uuid, Long.toString(timestamp), nonce);

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(PluginConstants.SUBCHANNEL_PREMIUM_AUTO_LOGIN);
        out.writeUTF(username);
        out.writeUTF(uuid);
        out.writeLong(timestamp);
        out.writeUTF(nonce);
        out.writeUTF(signature);

        java.util.Optional<ServerConnection> server = player.getCurrentServer();
        if (!server.isPresent()) return false;

        server.get().sendPluginMessage(PluginMessageListener.IDENTIFIER, out.toByteArray());
        return true;
    }

    /** Replies to a ready backend with the proxy's authoritative auth decision. */
    public static boolean sendAuthState(PkLoginVelocity plugin, Player player,
                                        String requestNonce, boolean authenticated) {
        String secret = plugin.getProxySecret();
        if (secret == null || secret.isEmpty()) return false;

        java.util.Optional<ServerConnection> server = player.getCurrentServer();
        if (!server.isPresent()) return false;

        String username = player.getUsername();
        String uuid = player.getUniqueId().toString();
        String decision = authenticated
                ? ProxyAuthStateProtocol.AUTHENTICATED
                : ProxyAuthStateProtocol.PASSWORD_REQUIRED;
        long timestamp = System.currentTimeMillis();
        String responseNonce = ProxyMessageSecurity.newNonce();
        String signature = ProxyMessageSecurity.sign(secret,
                ProxyAuthStateProtocol.responseParts(username, uuid, decision,
                        requestNonce, timestamp, responseNonce));

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(PluginConstants.SUBCHANNEL_AUTH_STATE_RESPONSE);
        out.writeUTF(username);
        out.writeUTF(uuid);
        out.writeUTF(decision);
        out.writeUTF(requestNonce);
        out.writeLong(timestamp);
        out.writeUTF(responseNonce);
        out.writeUTF(signature);

        return server.get().sendPluginMessage(PluginMessageListener.IDENTIFIER, out.toByteArray());
    }
}
