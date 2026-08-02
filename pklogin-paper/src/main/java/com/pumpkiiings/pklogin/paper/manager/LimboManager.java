package com.pumpkiiings.pklogin.paper.manager;

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

public class LimboManager {

    /** Speed that immobilises a player; not configurable, anything else lets them move. */
    private static final float FROZEN_SPEED = 0.0f;

    private static final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();
    private static final Map<UUID, ItemStack[]> savedInventories = new ConcurrentHashMap<>();
    private static final Map<UUID, ItemStack[]> savedArmor = new ConcurrentHashMap<>();

    public static void teleportToSpawn(JavaPlugin plugin, Player player) {
        if (Settings.TELEPORT_LAST_LOCATION.asBoolean()) {
            lastLocations.put(player.getUniqueId(), player.getLocation());
        }
        
        java.io.File file = new java.io.File(plugin.getDataFolder(), "spawn.yml");
        if (file.exists()) {
            org.bukkit.configuration.file.YamlConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
            if (config.contains("spawn")) {
                player.teleportAsync((org.bukkit.Location) config.get("spawn"));
                return;
            }
        }
        
        // Fallback to world spawn if spawn.yml does not exist or has no 'spawn'
        player.teleportAsync(player.getWorld().getSpawnLocation());
    }

    public static void restoreLastLocation(Player player) {
        if (Settings.TELEPORT_LAST_LOCATION.asBoolean()) {
            Location loc = lastLocations.remove(player.getUniqueId());
            if (loc != null) {
                player.teleportAsync(loc);
            }
        }
    }

    /**
     * Takes a player out of limbo after a successful authentication.
     *
     * <p>Callers must use this instead of calling {@link #removeLimboState} and
     * {@link #restoreLastLocation} themselves: the stored location has to be read
     * before the limbo state is torn down, and getting that order wrong silently
     * drops the player wherever the safe-spawn teleport left them.</p>
     */
    public static void leaveLimbo(JavaPlugin plugin, Player player) {
        restoreLastLocation(player);
        removeLimboState(plugin, player);
    }

    /**
     * Drops any state held for a player who left without authenticating.
     * Inventory contents are handed back first so nothing is lost on disconnect.
     */
    public static void discard(JavaPlugin plugin, Player player) {
        removeLimboState(plugin, player);
        lastLocations.remove(player.getUniqueId());
    }

    public static void applyLimboState(JavaPlugin plugin, Player player) {
        if (Settings.LIMBO_BLINDNESS_EFFECT.asBoolean()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, false, false));
        }

        if (Settings.LIMBO_BLOCK_WALK.asBoolean()) {
            player.setWalkSpeed(FROZEN_SPEED);
            player.setFlySpeed(FROZEN_SPEED);
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

        if (Settings.LIMBO_BLOCK_WALK.asBoolean()) {
            player.setWalkSpeed(Settings.LIMBO_RESTORE_WALK_SPEED.asFloat());
            player.setFlySpeed(Settings.LIMBO_RESTORE_FLY_SPEED.asFloat());
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
