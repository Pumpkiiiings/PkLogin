package com.pumpkiiings.pklogin.forge.manager;

import com.pumpkiiings.pklogin.common.model.Location;
import com.pumpkiiings.pklogin.forge.PkLoginForge;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class ForgeLimboManager {

    private static final java.util.Map<java.util.UUID, com.pumpkiiings.pklogin.common.model.Location> lastLocations = new java.util.concurrent.ConcurrentHashMap<>();

    public static void teleportToSpawn(ServerPlayer player, String spawnType) {
        if (spawnType.equals("join") && com.pumpkiiings.pklogin.common.settings.Settings.TELEPORT_LAST_LOCATION.asBoolean()) {
            lastLocations.put(player.getUUID(), new com.pumpkiiings.pklogin.common.model.Location(
                player.level().dimension().location().toString(),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot()
            ));
        }

        Location loc = PkLoginForge.getInstance().getSpawnManager().getSpawn(spawnType);
        if (loc != null) {
            ResourceKey<net.minecraft.world.level.Level> dimension = ResourceKey.create(
                    Registries.DIMENSION, new ResourceLocation(loc.getWorld())
            );
            ServerLevel serverLevel = player.server.getLevel(dimension);
            if (serverLevel != null) {
                player.teleportTo(serverLevel, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            }
        }
    }

    public static void restoreLastLocation(ServerPlayer player) {
        if (com.pumpkiiings.pklogin.common.settings.Settings.TELEPORT_LAST_LOCATION.asBoolean()) {
            Location loc = lastLocations.remove(player.getUUID());
            if (loc != null) {
                ResourceKey<net.minecraft.world.level.Level> dimension = ResourceKey.create(
                        Registries.DIMENSION, new ResourceLocation(loc.getWorld())
                );
                ServerLevel serverLevel = player.server.getLevel(dimension);
                if (serverLevel != null) {
                    player.teleportTo(serverLevel, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                }
            }
        }
    }

    public static void applyLimboState(ServerPlayer player) {
        if (com.pumpkiiings.pklogin.common.settings.Settings.LIMBO_BLINDNESS_EFFECT.asBoolean()) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Integer.MAX_VALUE, 0, false, false));
        }
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
    }

    public static void removeLimboState(ServerPlayer player) {
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.INVISIBILITY);
        lastLocations.remove(player.getUUID());
    }
}
