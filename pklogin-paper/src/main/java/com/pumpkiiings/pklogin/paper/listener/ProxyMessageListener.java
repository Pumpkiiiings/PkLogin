package com.pumpkiiings.pklogin.paper.listener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import com.pumpkiiings.pklogin.paper.task.LoginQueue;
import com.pumpkiiings.pklogin.common.security.ProxyMessageSecurity;
import com.pumpkiiings.pklogin.common.settings.Messages;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * Handles privileged messages coming from the proxy on {@code pklogin:main}.
 *
 * <p>Bukkit hands us every payload the <em>client</em> sends on this channel too,
 * and nothing in the API distinguishes the two. Each message is therefore only
 * acted on once its HMAC signature checks out against the shared
 * {@code proxy-secret}; an unsigned or badly signed payload is dropped.</p>
 */
public class ProxyMessageListener implements PluginMessageListener {

    private final PkLoginPaper plugin;

    private volatile long lastMissingSecretWarning;

    public ProxyMessageListener(PkLoginPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(com.pumpkiiings.pklogin.common.PluginConstants.CHANNEL_MAIN)) {
            return;
        }

        final String subChannel;
        final String username;
        final String uuid;
        final long timestamp;
        final String nonce;
        final String signature;
        try {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            subChannel = in.readUTF();
            username = in.readUTF();
            uuid = in.readUTF();
            timestamp = in.readLong();
            nonce = in.readUTF();
            signature = in.readUTF();
        } catch (IllegalStateException malformed) {
            // Truncated or hand-crafted payload; nothing to do.
            return;
        }

        if (!com.pumpkiiings.pklogin.common.PluginConstants.SUBCHANNEL_PREMIUM_AUTO_LOGIN.equals(subChannel)) {
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
                com.pumpkiiings.pklogin.common.PluginConstants.SUBCHANNEL_PREMIUM_AUTO_LOGIN,
                username, uuid, Long.toString(timestamp), nonce);
        if (!authentic) {
            plugin.getLogger().warning("Rejected an unauthenticated PremiumAutoLogin message for "
                    + player.getName() + " (bad signature, stale timestamp or replayed nonce).");
            return;
        }

        if (plugin.getLoginManagement().isAuthenticated(player.getName())) {
            return;
        }

        plugin.getLoginManagement().setAuthenticated(player.getName());
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
        plugin.getLogger().severe("Received a proxy auto-login message but there is no key to verify it with. "
                + "Enable Velocity modern forwarding (config/paper-global.yml) or set 'proxy-secret' in config.yml.");
    }
}
