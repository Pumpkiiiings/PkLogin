package com.pumpkiiings.pklogin.bukkit.skin;


import com.pumpkiiings.pklogin.bukkit.PkLoginBukkit;
import com.pumpkiiings.pklogin.common.skin.SkinFetcher;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class BukkitSkinApplier {

    public static void applySkin(Player player, SkinFetcher.SkinData skinData, PkLoginBukkit plugin) {
        if (skinData == null) return;

        try {
            Method getHandleMethod = player.getClass().getMethod("getHandle");
            Object entityPlayer = getHandleMethod.invoke(player);

            Method getProfileMethod = null;
            for (Method m : entityPlayer.getClass().getMethods()) {
                if (m.getName().equals("getProfile") || m.getReturnType().getSimpleName().equals("GameProfile")) {
                    getProfileMethod = m;
                    break;
                }
            }

            if (getProfileMethod != null) {
                Object profile = getProfileMethod.invoke(entityPlayer);
                
                // Get Property map
                Method getPropertiesMethod = profile.getClass().getMethod("getProperties");
                Object propertyMap = getPropertiesMethod.invoke(profile);
                
                // Remove existing textures
                Method removeAllMethod = propertyMap.getClass().getMethod("removeAll", Object.class);
                removeAllMethod.invoke(propertyMap, "textures");
                
                // Create new Property("textures", value, signature)
                Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
                Object newProperty = propertyClass.getConstructor(String.class, String.class, String.class)
                        .newInstance("textures", skinData.getValue(), skinData.getSignature());
                
                // Add to map
                Method putMethod = propertyMap.getClass().getMethod("put", Object.class, Object.class);
                putMethod.invoke(propertyMap, "textures", newProperty);

                // Reload player in main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (!online.equals(player) && online.canSee(player)) {
                            try {
                                // Try modern method
                                online.getClass().getMethod("hidePlayer", Plugin.class, Player.class).invoke(online, plugin, player);
                                online.getClass().getMethod("showPlayer", Plugin.class, Player.class).invoke(online, plugin, player);
                            } catch (Exception ignored) {
                                // Fallback to legacy method for 1.8
                                try {
                                    online.getClass().getMethod("hidePlayer", Player.class).invoke(online, player);
                                    online.getClass().getMethod("showPlayer", Player.class).invoke(online, player);
                                } catch (Exception ignored2) {}
                            }
                        }
                    }
                });
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply native skin via reflection.");
        }
    }
}
