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
import java.security.KeyPair;
import java.security.SecureRandom;
import com.pumpkiiings.pklogin.forge.util.EncryptionUtil;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import net.minecraft.util.Crypt;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginPacketListenerImplMixin {

    @Shadow @Final public Connection connection;
    @Shadow private GameProfile gameProfile;
    @Shadow public abstract void startClientVerification(GameProfile profile);
    
    private String currentHandlingName;
    private final SecureRandom random = new SecureRandom();
    private final KeyPair customKeyPair = EncryptionUtil.generateKeyPair();
    private byte[] verifyToken;

    @Inject(method = "handleHello", at = @At("HEAD"), cancellable = true)
    private void onHandleHello(ServerboundHelloPacket packet, CallbackInfo ci) {
        this.currentHandlingName = packet.name();
        String originalName = packet.name();
        
        com.pumpkiiings.pklogin.common.manager.AccountManagement accountManagement = com.pumpkiiings.pklogin.forge.PkLoginForge.getInstance().getAccountManagement();
        java.util.Optional<com.pumpkiiings.pklogin.common.model.Account> accountOpt = accountManagement.search(originalName);
        
        if (accountOpt.isPresent()) {
            String uuidType = accountOpt.get().getUuidType();
            if (uuidType != null && (uuidType.equals("REAL") || uuidType.equals("PREMIUM"))) {
                this.verifyToken = EncryptionUtil.generateVerifyToken(random);
                this.connection.send(new ClientboundHelloPacket("", this.customKeyPair.getPublic().getEncoded(), this.verifyToken));
                ci.cancel();
            }
        }
    }

    @Inject(method = "handleKey", at = @At("HEAD"), cancellable = true)
    private void onHandleKey(ServerboundKeyPacket packet, CallbackInfo ci) {
        if (this.verifyToken != null && this.currentHandlingName != null) {
            ci.cancel();
            
            Thread authThread = new Thread(() -> {
                try {
                    SecretKey sharedKey = packet.getSecretKey(this.customKeyPair.getPrivate());
                    
                    if (!packet.isChallengeValid(this.verifyToken, this.customKeyPair.getPrivate())) {
                        this.connection.disconnect(net.minecraft.network.chat.Component.literal("Invalid verification token"));
                        return;
                    }
                    
                    String serverId = EncryptionUtil.getServerIdHashString("", sharedKey, this.customKeyPair.getPublic());
                    UUID premiumUUID = fetchMojangProfile(this.currentHandlingName, serverId);
                    
                    if (premiumUUID != null) {
                        Cipher decryptCipher = Crypt.getCipher(2, sharedKey);
                        Cipher encryptCipher = Crypt.getCipher(1, sharedKey);
                        
                        this.connection.setEncryptionKey(decryptCipher, encryptCipher);
                        
                        GameProfile verifiedProfile = new GameProfile(premiumUUID, this.currentHandlingName);
                        String ip = this.connection.getRemoteAddress().toString();
                        if (this.connection.getRemoteAddress() instanceof java.net.InetSocketAddress) {
                            ip = ((java.net.InetSocketAddress) this.connection.getRemoteAddress()).getAddress().getHostAddress();
                        }
                        com.pumpkiiings.pklogin.forge.PkLoginForge.getInstance().getVerifiedSessions().put(ip, this.currentHandlingName);
                        this.startClientVerification(verifiedProfile);
                    } else {
                        this.connection.disconnect(net.minecraft.network.chat.Component.literal("Invalid session (Mojang rejected)."));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    this.connection.disconnect(net.minecraft.network.chat.Component.literal("Authentication error."));
                }
            }, "PkLogin-Authenticator");
            authThread.setDaemon(true);
            authThread.start();
        }
    }

    private UUID fetchMojangProfile(String username, String serverId) {
        try {
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/hasJoined?username=" + username + "&serverId=" + serverId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                JsonObject json = new JsonParser().parse(new InputStreamReader(conn.getInputStream())).getAsJsonObject();
                String idStr = json.get("id").getAsString();
                return UUID.fromString(idStr.replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"
                ));
            }
        } catch (Exception ignored) { }
        return null;
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
