package com.pumpkiiings.pklogin.forge.mixin;

import com.mojang.authlib.GameProfile;
import com.pumpkiiings.pklogin.common.settings.Settings;
import com.pumpkiiings.pklogin.forge.manager.UsernameAppenderManager;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Mixin(ServerLoginPacketListenerImpl.class)
public class ServerLoginPacketListenerImplMixin {

    @Shadow @Final public Connection connection;
    @Shadow private GameProfile gameProfile;
    
    private String currentHandlingName;

    @Inject(method = "handleHello", at = @At("HEAD"))
    private void captureName(ServerboundHelloPacket packet, CallbackInfo ci) {
        this.currentHandlingName = packet.name();
    }

    @Redirect(method = "handleHello", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;usesAuthentication()Z"))
    private boolean redirectUsesAuthentication(net.minecraft.server.MinecraftServer server) {
        String originalName = this.currentHandlingName;
        com.pumpkiiings.pklogin.common.manager.AccountManagement accountManagement = com.pumpkiiings.pklogin.forge.PkLoginForge.getInstance().getAccountManagement();
        java.util.Optional<com.pumpkiiings.pklogin.common.model.Account> accountOpt = accountManagement.search(originalName);
        
        if (accountOpt.isPresent()) {
            String uuidType = accountOpt.get().getUuidType();
            if (uuidType != null && (uuidType.equals("REAL") || uuidType.equals("PREMIUM"))) {
                return true;
            } else if (uuidType != null && (uuidType.equals("OFFLINE") || uuidType.equals("RANDOM"))) {
                return false;
            }
        }
        
        // Default to server setting if no account found or REAL
        return server.usesAuthentication();
    }

    @Inject(method = "startClientVerification", at = @At("HEAD"))
    private void onStartClientVerification(GameProfile original, CallbackInfo ci) {
        String originalName = this.currentHandlingName != null ? this.currentHandlingName : original.getName();
        String newName = originalName;

        // 1. Username Appender logic
        if (Settings.APPENDER_ENABLED.asBoolean() && this.connection != null) {
            String host = UsernameAppenderManager.getHost(this.connection.getRemoteAddress());
            if (host != null) {
                String appendix = "";
                String position = "suffix";
                java.util.List<String> premiumDomains = Settings.APPENDER_PREMIUM_DOMAINS.asList();
                java.util.List<String> offlineDomains = Settings.APPENDER_OFFLINE_DOMAINS.asList();
                
                if (offlineDomains.contains(host)) {
                    appendix = Settings.APPENDER_OFFLINE_APPENDIX.asString();
                    position = Settings.APPENDER_OFFLINE_POSITION.asString();
                } else if (premiumDomains.contains(host)) {
                    appendix = Settings.APPENDER_PREMIUM_APPENDIX.asString();
                    position = Settings.APPENDER_PREMIUM_POSITION.asString();
                }
                
                if (!appendix.isEmpty()) {
                    newName = position.equals("prefix") ? appendix + originalName : originalName + appendix;
                }
            }
        }

        // 2. DB Lookup for UUID type
        com.pumpkiiings.pklogin.common.manager.AccountManagement accountManagement = com.pumpkiiings.pklogin.forge.PkLoginForge.getInstance().getAccountManagement();
        java.util.Optional<com.pumpkiiings.pklogin.common.model.Account> accountOpt = accountManagement.search(originalName);
        
        String uuidType = "REAL";
        String randomUuid = null;
        if (accountOpt.isPresent()) {
            uuidType = accountOpt.get().getUuidType();
            randomUuid = accountOpt.get().getRandomUuid();
        }

        // Only modify if it's an offline authentication (i.e., not verified by Mojang)
        // If it was verified by Mojang, we trust the profile they give us unless it's explicitly OFFLINE/RANDOM
        if (!uuidType.equals("REAL") && !uuidType.equals("PREMIUM")) {
            UUID uuid;
            if (uuidType.equals("RANDOM")) {
                if (randomUuid == null || randomUuid.isEmpty()) {
                    uuid = UUID.randomUUID();
                    accountManagement.updateRandomUuid(originalName, uuid.toString());
                } else {
                    uuid = UUID.fromString(randomUuid);
                }
            } else {
                // OFFLINE default
                uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + newName).getBytes(StandardCharsets.UTF_8));
            }
            
            // Override the current game profile since we are in startClientVerification
            this.gameProfile = new GameProfile(uuid, newName);
        } else {
            // Keep original profile but update name if appender changed it
            if (!newName.equals(originalName)) {
                this.gameProfile = new GameProfile(original.getId(), newName);
            } else {
                this.gameProfile = original;
            }
        }
    }
}
