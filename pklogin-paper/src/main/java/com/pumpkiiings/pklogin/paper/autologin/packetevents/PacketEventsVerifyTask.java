package com.pumpkiiings.pklogin.paper.autologin.packetevents;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientLoginStart;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import com.pumpkiiings.pklogin.paper.autologin.protocollib.AutoLoginSession;
import com.pumpkiiings.pklogin.paper.autologin.protocollib.EncryptionUtil;
import javax.crypto.spec.IvParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Key;
import java.security.KeyPair;
import java.util.UUID;
import java.net.InetSocketAddress;

public class PacketEventsVerifyTask implements Runnable {

    private final PkLoginPaper plugin;
    private final User user;
    private final AutoLoginSession session;
    private final byte[] sharedSecret;
    private final byte[] encryptedNonce;
    private final KeyPair keyPair;
    private final PacketEventsListener listener;


    public PacketEventsVerifyTask(PkLoginPaper plugin, User user, AutoLoginSession session, byte[] sharedSecret, byte[] encryptedNonce, KeyPair keyPair, PacketEventsListener listener) {
        this.plugin = plugin;
        this.user = user;
        this.session = session;
        this.sharedSecret = sharedSecret;
        this.encryptedNonce = encryptedNonce;
        this.keyPair = keyPair;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            if (encryptedNonce == null || !EncryptionUtil.verifyNonce(session.getVerifyToken(), keyPair.getPrivate(), encryptedNonce)) {
                kickPlayer("Invalid verify token.");
                return;
            }

            SecretKey loginKey = EncryptionUtil.decryptSharedKey(keyPair.getPrivate(), sharedSecret);
            if (!enableEncryption(loginKey)) {
                return;
            }

            String serverId = EncryptionUtil.getServerIdHashString("", loginKey, keyPair.getPublic());
            InetSocketAddress address = (InetSocketAddress) user.getAddress();
            String ip = address.getAddress().getHostAddress();
            UUID premiumUUID = fetchMojangProfile(session.getUsername(), serverId, ip);

            if (premiumUUID != null) {
                // Verify signature if exists
                if (session.getClientKey() != null) {
                    if (!EncryptionUtil.verifyClientKey(session.getClientKey(), java.time.Instant.now(), premiumUUID)) {
                        kickPlayer("Invalid profile public key signature.");
                        return;
                    }
                }

                session.setPremiumUUID(premiumUUID);
                session.setVerified(true);
                
                listener.addVerifiedSession(ip, session);
                
                // Send fake START
                WrapperLoginClientLoginStart fakeStart = new WrapperLoginClientLoginStart(user.getClientVersion(), session.getUsername(), null, premiumUUID);
                PacketEvents.getAPI().getProtocolManager().receivePacket(user.getChannel(), fakeStart);
            } else {
                kickPlayer("Invalid session (Mojang rejected).");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            kickPlayer("Authentication error.");
        }
    }

    private UUID fetchMojangProfile(String username, String serverId, String ip) {
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

    private boolean enableEncryption(SecretKey loginKey) {
        try {
            io.netty.channel.Channel channel = (io.netty.channel.Channel) user.getChannel();
            Object networkManager = channel.pipeline().get("packet_handler");
            if (networkManager == null) {
                kickPlayer("Network manager not found.");
                return false;
            }

            Class<?> networkManagerClass = networkManager.getClass();

            Method targetMethod = null;
            for (Method m : networkManagerClass.getMethods()) {
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == SecretKey.class) {
                    targetMethod = m;
                    break;
                }
            }

            if (targetMethod != null) {
                targetMethod.setAccessible(true);
                targetMethod.invoke(networkManager, loginKey);
                return true;
            }

            for (Method m : networkManagerClass.getMethods()) {
                if (m.getParameterCount() == 2 && m.getParameterTypes()[0] == Cipher.class && m.getParameterTypes()[1] == Cipher.class) {
                    targetMethod = m;
                    break;
                }
            }

            if (targetMethod != null) {
                Cipher decryptionCipher = Cipher.getInstance("AES/CFB8/NoPadding");
                decryptionCipher.init(Cipher.DECRYPT_MODE, loginKey, new IvParameterSpec(loginKey.getEncoded()));
                
                Cipher encryptionCipher = Cipher.getInstance("AES/CFB8/NoPadding");
                encryptionCipher.init(Cipher.ENCRYPT_MODE, loginKey, new IvParameterSpec(loginKey.getEncoded()));

                targetMethod.setAccessible(true);
                targetMethod.invoke(networkManager, decryptionCipher, encryptionCipher);
                return true;
            }

            kickPlayer("Encryption method not found.");
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            kickPlayer("Couldn't enable encryption");
            return false;
        }
    }

    private void kickPlayer(String reason) {
        user.closeConnection(); // Simplified kick using PacketEvents
    }
}
