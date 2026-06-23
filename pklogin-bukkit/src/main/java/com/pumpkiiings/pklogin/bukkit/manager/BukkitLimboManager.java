package com.pumpkiiings.pklogin.bukkit.manager;

import com.pumpkiiings.pklogin.common.settings.Settings;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BukkitLimboManager {

    private static final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();
    private static final Map<UUID, ItemStack[]> savedInventories = new ConcurrentHashMap<>();
    private static final Map<UUID, ItemStack[]> savedArmor = new ConcurrentHashMap<>();

    public static void teleportToSpawn(Player player) {
        if (Settings.TELEPORT_LAST_LOCATION.asBoolean()) {
            lastLocations.put(player.getUniqueId(), player.getLocation());
        }
        
        // Normally teleport to spawn. In a real plugin, this would read from spawn location.
        // For now, we will just teleport to the world's spawn location.
        player.teleport(player.getWorld().getSpawnLocation());
    }

    public static void restoreLastLocation(Player player) {
        if (Settings.TELEPORT_LAST_LOCATION.asBoolean()) {
            Location loc = lastLocations.remove(player.getUniqueId());
            if (loc != null) {
                player.teleport(loc);
            }
        }
    }

    public static void applyLimboState(JavaPlugin plugin, Player player) {
        if (Settings.LIMBO_BLINDNESS_EFFECT.asBoolean()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, false, false));
        }

        if (Settings.LIMBO_BLOCK_WALK.asBoolean()) {
            player.setWalkSpeed(0.0f);
            player.setFlySpeed(0.0f);
        }

        if (Settings.LIMBO_HIDE_INVENTORY.asBoolean()) {
            savedInventories.put(player.getUniqueId(), player.getInventory().getContents());
            savedArmor.put(player.getUniqueId(), player.getInventory().getArmorContents());
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
        }

        if (Settings.LIMBO_HIDE_PLAYERS.asBoolean()) {
            for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                player.hidePlayer(plugin, p);
                p.hidePlayer(plugin, player);
            }
        }
    }

    public static void removeLimboState(JavaPlugin plugin, Player player) {
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        lastLocations.remove(player.getUniqueId());

        if (Settings.LIMBO_BLOCK_WALK.asBoolean()) {
            player.setWalkSpeed(0.2f);
            player.setFlySpeed(0.1f);
        }

        if (Settings.LIMBO_HIDE_INVENTORY.asBoolean()) {
            ItemStack[] contents = savedInventories.remove(player.getUniqueId());
            ItemStack[] armor = savedArmor.remove(player.getUniqueId());
            if (contents != null) player.getInventory().setContents(contents);
            if (armor != null) player.getInventory().setArmorContents(armor);
        }

        if (Settings.LIMBO_HIDE_PLAYERS.asBoolean()) {
            for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                player.showPlayer(plugin, p);
                p.showPlayer(plugin, player);
            }
        }
    }
}
