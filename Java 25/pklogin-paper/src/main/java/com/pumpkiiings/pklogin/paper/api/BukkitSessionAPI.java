package com.pumpkiiings.pklogin.paper.api;

import com.pumpkiiings.pklogin.api.service.SessionAPI;
import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Session state for the Bukkit platform.
 *
 * <p>Authentication is tracked by player name, so the UUID overload resolves the
 * online player first. The shared implementation could not do that — it had no
 * way to map a UUID to a name and simply answered {@code false}.</p>
 */
public class BukkitSessionAPI implements SessionAPI {

    private final PkLoginPaper plugin;

    public BukkitSessionAPI(PkLoginPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isAuthenticated(String name) {
        return plugin.getLoginManagement().isAuthenticated(name);
    }

    @Override
    public boolean isAuthenticated(UUID uuid) {
        Player player = plugin.getServer().getPlayer(uuid);
        return player != null && plugin.getLoginManagement().isAuthenticated(player.getName());
    }

    @Override
    public CompletableFuture<Boolean> forceLogin(String name) {
        return CompletableFuture.supplyAsync(() -> {
            Player player = plugin.getServer().getPlayerExact(name);
            if (player == null) return false;

            plugin.getLoginManagement().setAuthenticated(player.getName());
            player.getScheduler().run(plugin, task ->
                    com.pumpkiiings.pklogin.paper.manager.LimboManager.leaveLimbo(plugin, player), null);
            com.pumpkiiings.pklogin.paper.task.LoginQueue.removeFromQueue(player.getName());
            return true;
        });
    }
}
