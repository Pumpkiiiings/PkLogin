package com.pumpkiiings.pklogin.paper.listener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import com.pumpkiiings.pklogin.paper.task.LoginQueue;
import com.pumpkiiings.pklogin.common.PluginConstants;
import com.pumpkiiings.pklogin.common.security.ProxyMessageSecurity;
import com.pumpkiiings.pklogin.common.security.ProxyVerifyProtocol;
import com.pumpkiiings.pklogin.common.settings.Messages;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * Handles privileged messages coming from the proxy on {@code pklogin:main}.
 *
 * <p>Bukkit hands us every payload the <em>client</em> sends on this channel too,
 * and nothing in the API distinguishes the two. Each message is therefore only
 * acted on once its HMAC signature checks out against the key both ends derive
 * from the proxy's forwarding secret; an unsigned or badly signed payload is
 * dropped.</p>
 */
public class ProxyMessageListener implements PluginMessageListener {

    private final PkLoginPaper plugin;

    private volatile long lastMissingSecretWarning;

    public ProxyMessageListener(PkLoginPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(PluginConstants.CHANNEL_MAIN)) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        final String subChannel;
        try {
            subChannel = in.readUTF();
        } catch (IllegalStateException malformed) {
            // Truncated or hand-crafted payload; nothing to do.
            return;
        }

        if (PluginConstants.SUBCHANNEL_VERIFY.equals(subChannel)) {
            handleVerify(player, in);
            return;
        }

        if (PluginConstants.SUBCHANNEL_PREMIUM_AUTO_LOGIN.equals(subChannel)) {
            handlePremiumAutoLogin(player, in);
        }
    }

    /**
     * Answers the proxy's connection check.
     *
     * <p>Signing the answer is what makes it worth anything: it proves this server
     * resolved the same key, which is precisely what the proxy is asking. The
     * challenge comes back with it so the proxy can tell the answer apart from a
     * stale one.</p>
     */
    private void handleVerify(Player player, ByteArrayDataInput in) {
        final String serverName;
        final long timestamp;
        final String nonce;
        final String signature;
        try {
            serverName = in.readUTF();
            timestamp = in.readLong();
            nonce = in.readUTF();
            signature = in.readUTF();
        } catch (IllegalStateException malformed) {
            return;
        }

        String secret = plugin.getProxySecret();
        if (secret == null || secret.isEmpty()) {
            warnMissingSecret();
            return;
        }

        boolean authentic = ProxyMessageSecurity.verify(secret, signature, timestamp, nonce,
                ProxyVerifyProtocol.requestParts(serverName, timestamp, nonce));
        if (!authentic) {
            // Either a client trying its luck on the channel or a genuine key
            // mismatch. Staying quiet on the first would hide the second, and the
            // proxy reports the mismatch from its own side regardless.
            plugin.getLogger().warning("Rejected an unverifiable connection check on "
                    + PluginConstants.CHANNEL_MAIN + ".");
            return;
        }

        long replyTimestamp = System.currentTimeMillis();
        String replyNonce = ProxyMessageSecurity.newNonce();
        String version = plugin.getDescription().getVersion();
        String replySignature = ProxyMessageSecurity.sign(secret,
                ProxyVerifyProtocol.replyParts(serverName, version, nonce, replyTimestamp, replyNonce));

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(PluginConstants.SUBCHANNEL_VERIFY_REPLY);
        out.writeUTF(serverName);
        out.writeUTF(version);
        out.writeUTF(nonce);
        out.writeLong(replyTimestamp);
        out.writeUTF(replyNonce);
        out.writeUTF(replySignature);

        player.sendPluginMessage(plugin, PluginConstants.CHANNEL_MAIN, out.toByteArray());
    }

    /** Logs a player in because the proxy vouched for their premium account. */
    private void handlePremiumAutoLogin(Player player, ByteArrayDataInput in) {
        final String username;
        final String uuid;
        final long timestamp;
        final String nonce;
        final String signature;
        try {
            username = in.readUTF();
            uuid = in.readUTF();
            timestamp = in.readLong();
            nonce = in.readUTF();
            signature = in.readUTF();
        } catch (IllegalStateException malformed) {
            return;
        }

        String secret = plugin.getProxySecret();
        if (secret == null || secret.isEmpty()) {
            warnMissingSecret();
            return;
        }

        // Bind the message to this specific connection: a signed payload captured
        // for one player is useless when replayed on another player's channel.
        if (!player.getName().equalsIgnoreCase(username)
                || !player.getUniqueId().toString().equals(uuid)) {
            return;
        }

        boolean authentic = ProxyMessageSecurity.verify(secret, signature, timestamp, nonce,
                PluginConstants.SUBCHANNEL_PREMIUM_AUTO_LOGIN,
                username, uuid, Long.toString(timestamp), nonce);
        if (!authentic) {
            plugin.getLogger().warning("Rejected an unauthenticated PremiumAutoLogin message for "
                    + player.getName() + " (bad signature, stale timestamp or replayed nonce).");
            return;
        }

        if (plugin.isAuthenticated(player)) {
            return;
        }

        if (!plugin.authenticate(player)) return;
        player.sendMessage(Messages.PREMIUM_AUTO_LOGIN.asString());
        LoginQueue.removeFromQueue(player.getName());
        plugin.runAsync(() -> new com.pumpkiiings.pklogin.api.event.bukkit.AsyncAuthenticateEvent(player).callEvt());

        player.getScheduler().run(plugin, task ->
                com.pumpkiiings.pklogin.paper.manager.LimboManager.leaveLimbo(plugin, player), null);
    }

    /** Rate-limited so a misconfigured network does not flood the console. */
    private void warnMissingSecret() {
        long now = System.currentTimeMillis();
        if (now - lastMissingSecretWarning < 60_000L) return;
        lastMissingSecretWarning = now;
        plugin.getLogger().severe("Received a message from the proxy but there is no key to verify it with. "
                + "Enable Velocity modern forwarding in config/paper-global.yml so both ends share one.");
    }
}
