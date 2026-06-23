package com.pumpkiiings.pklogin.bukkit.manager;

import com.pumpkiiings.pklogin.common.settings.Settings;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BukkitLimboManager {

    private static final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();

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

    public static void applyLimboState(Player player) {
        if (Settings.LIMBO_BLINDNESS_EFFECT.asBoolean()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, false, false));
        }
    }

    public static void removeLimboState(Player player) {
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        lastLocations.remove(player.getUniqueId());
    }
}
