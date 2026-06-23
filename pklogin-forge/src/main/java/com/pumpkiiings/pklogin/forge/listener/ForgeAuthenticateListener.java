package com.pumpkiiings.pklogin.forge.listener;

import com.pumpkiiings.pklogin.common.model.Title;
import com.pumpkiiings.pklogin.common.settings.Messages;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

public class ForgeAuthenticateListener {

    public static void onAuthenticate(ServerPlayer player, boolean isRegister) {
        // Send success message
        String msg = isRegister ? Messages.SUCCESSFUL_REGISTER.asString("§aSuccessfully registered and logged in!") 
                                : Messages.SUCCESSFUL_LOGIN.asString("§aSuccessfully logged in!");
        player.sendSystemMessage(Component.literal(msg));

        // Send title
        Title title = isRegister ? Messages.TITLE_AFTER_REGISTER.asTitle() : Messages.TITLE_AFTER_LOGIN.asTitle();
        com.pumpkiiings.pklogin.forge.ui.title.TitleAPI.getApi().send(player, title);

        // Restore Limbo state (this restores location if enabled)
        com.pumpkiiings.pklogin.forge.manager.ForgeLimboManager.restoreLastLocation(player);
        com.pumpkiiings.pklogin.forge.manager.ForgeLimboManager.removeLimboState(player);
        
        // Also teleport to spawn if a specific auth spawn is set
        com.pumpkiiings.pklogin.forge.manager.ForgeLimboManager.teleportToSpawn(player, isRegister ? "register" : "login");

        // Native Skins for premium players
        java.util.Optional<com.pumpkiiings.pklogin.common.model.Account> accountOpt = com.pumpkiiings.pklogin.forge.PkLoginForge.getInstance().getAccountManagement().retrieveOrLoad(player.getGameProfile().getName());
        if (accountOpt.isPresent() && accountOpt.get().getUuidType() != null && accountOpt.get().getUuidType().equals("REAL")) {
            new Thread(() -> {
                com.pumpkiiings.pklogin.common.skin.SkinFetcher.SkinData data = com.pumpkiiings.pklogin.common.skin.SkinFetcher.fetchSkin(player.getUUID());
                if (data != null) {
                    com.pumpkiiings.pklogin.forge.skin.ForgeSkinApplier.applySkin(player, data, player.server);
                }
            }).start();
        }
    }
}
