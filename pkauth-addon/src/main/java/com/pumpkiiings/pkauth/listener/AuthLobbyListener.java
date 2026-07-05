package com.pumpkiiings.pkauth.listener;

import com.pumpkiiings.pkauth.PkAuthAddon;
import com.pumpkiiings.pklogin.api.PkLoginAPI;
import com.pumpkiiings.pklogin.api.event.bukkit.AsyncAuthenticateEvent;
import com.pumpkiiings.pklogin.common.PkLogin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class AuthLobbyListener implements Listener {

    private final PkAuthAddon plugin;

    public AuthLobbyListener(PkAuthAddon plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        
        // Hide join message
        e.setJoinMessage(null);

        // Teleport to the empty auth world
        if (plugin.getAuthWorld() != null) {
            org.bukkit.Location spawnLoc = new org.bukkit.Location(plugin.getAuthWorld(), 0.5, 100, 0.5, 0, 0);
            player.teleport(spawnLoc);
        }

        // Hide all players from the joined player, and hide the joined player from all others
        for (Player online : Bukkit.getOnlinePlayers()) {
            player.hidePlayer(plugin, online);
            online.hidePlayer(plugin, player);
        }

        // Give them flight so they don't fall into the void
        player.setAllowFlight(true);
        player.setFlying(true);
        
        // Make sure they are in survival/adventure mode so they can't break blocks
        if (player.getGameMode() != GameMode.ADVENTURE && player.getGameMode() != GameMode.SURVIVAL) {
            player.setGameMode(GameMode.ADVENTURE);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent e) {
        if (e.isCancelled()) return;

        org.bukkit.Location from = e.getFrom();
        org.bukkit.Location to = e.getTo();
        
        if (to == null) return;

        // Cancel XYZ movement but allow Pitch/Yaw rotation
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            org.bukkit.Location newLoc = from.clone();
            newLoc.setPitch(to.getPitch());
            newLoc.setYaw(to.getYaw());
            e.setTo(newLoc);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        e.setQuitMessage(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        
        // If not authenticated, cancel chat completely
        if (!PkLogin.getApi().isAuthenticated(player.getName())) {
            e.setCancelled(true);
            return;
        }
        
        // Remove recipients who are not authenticated
        e.getRecipients().removeIf(recipient -> !PkLogin.getApi().isAuthenticated(recipient.getName()));
    }

    @EventHandler
    public void onAuthenticate(AsyncAuthenticateEvent e) {
        Player player = e.getPlayer();
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            // Player is authenticated, but they still shouldn't be able to move or lose fly.
            // (Velocity handles the teleportation out of this server)

            // Show all authenticated players to this player, and show this player to all authenticated players
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (PkLogin.getApi().isAuthenticated(online.getName())) {
                    player.showPlayer(plugin, online);
                    online.showPlayer(plugin, player);
                }
            }
        });
    }
}
