package com.pumpkiiings.pklogin.forge.skin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.pumpkiiings.pklogin.common.skin.SkinFetcher;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class ForgeSkinApplier {

    public static void applySkin(ServerPlayer player, SkinFetcher.SkinData skinData, MinecraftServer server) {
        if (skinData == null) return;

        server.execute(() -> {
            try {
                GameProfile profile = player.getGameProfile();
                profile.getProperties().removeAll("textures");
                profile.getProperties().put("textures", new Property("textures", skinData.getValue(), skinData.getSignature()));

                // Refresh Tab List & Player Info
                ClientboundPlayerInfoRemovePacket removePacket = new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID()));
                server.getPlayerList().broadcastAll(removePacket);

                ClientboundPlayerInfoUpdatePacket addPacket = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player));
                server.getPlayerList().broadcastAll(addPacket);

                // Quick teleport to self to force entity re-render for viewers
                player.teleportTo(player.serverLevel(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
