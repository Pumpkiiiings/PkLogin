/*
 * The MIT License (MIT)
 *
 * Copyright © 2020 - 2026 - PkLogin Contributors
 */
package com.pumpkiiings.pklogin.paper;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.pumpkiiings.pklogin.common.PluginConstants;
import com.pumpkiiings.pklogin.common.security.ProxyMessageSecurity;
import com.pumpkiiings.pklogin.paper.listener.PlayerJoinListeners;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Waits for Velocity's connection decision before the backend displays auth UI. */
public final class ProxyAuthCoordinator {

    private static final long RESPONSE_TIMEOUT_TICKS = 40L;

    private static final class Pending {
        private final Player player;
        private final String nonce;

        private Pending(Player player, String nonce) {
            this.player = player;
            this.nonce = nonce;
        }
    }

    private final PkLoginPaper plugin;
    private final Map<String, Pending> pending = new ConcurrentHashMap<>();

    public ProxyAuthCoordinator(PkLoginPaper plugin) {
        this.plugin = plugin;
    }

    public void request(Player player) {
        String requestNonce = ProxyMessageSecurity.newNonce();
        Pending request = new Pending(player, requestNonce);
        pending.put(key(player.getName()), request);

        player.getScheduler().runDelayed(plugin, task -> {
            if (!pending.remove(key(player.getName()), request)) return;
            if (!player.isOnline() || plugin.isAuthenticated(player)) return;

            plugin.getLogger().warning("Velocity did not answer the auth-state request for "
                    + player.getName() + "; falling back to backend authentication.");
            PlayerJoinListeners.beginPasswordAuthentication(plugin, player, true);
        }, () -> pending.remove(key(player.getName()), request), RESPONSE_TIMEOUT_TICKS);

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(PluginConstants.SUBCHANNEL_AUTH_STATE_REQUEST);
        out.writeUTF(player.getName());
        out.writeUTF(player.getUniqueId().toString());
        out.writeUTF(requestNonce);
        try {
            player.sendPluginMessage(plugin, PluginConstants.CHANNEL_MAIN, out.toByteArray());
        } catch (RuntimeException sendFailure) {
            plugin.getLogger().warning("Could not request auth state from Velocity for "
                    + player.getName() + ": " + sendFailure.getMessage());
        }
    }

    /** Consumes the response only when it belongs to this exact connection and request. */
    public boolean accept(Player player, String requestNonce) {
        String key = key(player.getName());
        Pending request = pending.get(key);
        if (request == null || request.player != player
                || !ProxyMessageSecurity.constantTimeEquals(request.nonce, requestNonce)) {
            return false;
        }
        return pending.remove(key, request);
    }

    public void forget(Player player) {
        String key = key(player.getName());
        Pending request = pending.get(key);
        if (request != null && request.player == player) pending.remove(key, request);
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
