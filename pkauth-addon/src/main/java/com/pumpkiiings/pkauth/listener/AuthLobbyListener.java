package com.pumpkiiings.pkauth.listener;

import com.pumpkiiings.pkauth.PkAuthAddon;
import com.pumpkiiings.pkauth.manager.ScoreboardAndTabManager;
import com.pumpkiiings.pklogin.api.event.bukkit.AsyncAuthenticateEvent;
import com.pumpkiiings.pklogin.common.PkLogin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
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

        e.setJoinMessage(null);

        if (plugin.getAuthWorld() != null) {
            org.bukkit.Location spawnLoc = new org.bukkit.Location(plugin.getAuthWorld(), 0.5, 100, 0.5, 0, 0);
            player.teleport(spawnLoc);
        }

        boolean hideUnauthenticated = plugin.getConfig().getBoolean("auth-world.hide-unauthenticated", true);

        if (hideUnauthenticated) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(player)) continue;
                player.hidePlayer(plugin, online);
                online.hidePlayer(plugin, player);
            }
        }

        player.setAllowFlight(true);
        player.setFlying(true);

        if (player.getGameMode() != GameMode.ADVENTURE && player.getGameMode() != GameMode.SURVIVAL) {
            player.setGameMode(GameMode.ADVENTURE);
        }

        if (PkLogin.getApi().isAuthenticated(player.getName())) {
            String rawMsg = plugin.getConfig().getString(
                "messages.already-authenticated",
                "<#FFD700><b>Ups!</b> <#AAAAAA>Aparentemente estas en el eterno vacio otra vez..."
            );
            String parsed = rawMsg.replace("%player%", player.getName());
            for (String line : parsed.split("\n")) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    player.sendMessage(ScoreboardAndTabManager.colorize(trimmed));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent e) {
        if (e.isCancelled()) return;

        org.bukkit.Location from = e.getFrom();
        org.bukkit.Location to = e.getTo();

        if (to == null) return;

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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (!plugin.getConfig().getBoolean("auth-world.cancel-pvp", true)) return;

        boolean victimInAuth = (e.getEntity() instanceof Player)
            && plugin.getAuthWorld() != null
            && e.getEntity().getWorld().getName().equals(plugin.getAuthWorld().getName());

        boolean attackerInAuth = (e.getDamager() instanceof Player)
            && plugin.getAuthWorld() != null
            && e.getDamager().getWorld().getName().equals(plugin.getAuthWorld().getName());

        boolean attackerNotAuth = (e.getDamager() instanceof Player)
            && !PkLogin.getApi().isAuthenticated(((Player) e.getDamager()).getName());

        boolean victimNotAuth = (e.getEntity() instanceof Player)
            && !PkLogin.getApi().isAuthenticated(((Player) e.getEntity()).getName());

        if (victimInAuth || attackerInAuth || victimNotAuth || attackerNotAuth) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        if (plugin.getAuthWorld() == null) return;
        if (!player.getWorld().getName().equals(plugin.getAuthWorld().getName())) return;
        if (e.getCause() == EntityDamageEvent.DamageCause.SUICIDE) return;

        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();

        if (!PkLogin.getApi().isAuthenticated(player.getName())) {
            e.setCancelled(true);
            return;
        }

        e.getRecipients().removeIf(recipient -> !PkLogin.getApi().isAuthenticated(recipient.getName()));
    }

    @EventHandler
    public void onAuthenticate(AsyncAuthenticateEvent e) {
        Player player = e.getPlayer();

        Bukkit.getScheduler().runTask(plugin, () -> {
            player.resetTitle();

            boolean hideUnauthenticated = plugin.getConfig().getBoolean("auth-world.hide-unauthenticated", true);

            if (hideUnauthenticated) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.equals(player)) continue;
                    if (PkLogin.getApi().isAuthenticated(online.getName())) {
                        player.showPlayer(plugin, online);
                        online.showPlayer(plugin, player);
                    }
                }
            }
        });
    }
}