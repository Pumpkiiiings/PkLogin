package com.pumpkiiings.pklogin.paper.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.injector.netty.channel.NettyChannelInjector;
import com.comphenix.protocol.injector.packet.PacketRegistry;
import com.comphenix.protocol.injector.temporary.TemporaryPlayerFactory;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.ConstructorAccessor;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.wrappers.Converters;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import org.bukkit.entity.Player;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Key;
import java.security.KeyPair;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.comphenix.protocol.PacketType.Login.Client.START;
import static com.comphenix.protocol.PacketType.Login.Server.DISCONNECT;

public class VerifyResponseTask implements Runnable {

    private final PkLoginPaper plugin;
    private final PacketEvent packetEvent;
    private final Player player;
    private final AutoLoginSession session;
    private final byte[] sharedSecret;
    private final byte[] encryptedNonce;
    private final KeyPair serverKey;

    private static Method encryptMethod;
    private static Method encryptKeyMethod;
    private static Method cipherMethod;

    public VerifyResponseTask(PkLoginPaper plugin, PacketEvent packetEvent, Player player, AutoLoginSession session, byte[] sharedSecret, byte[] encryptedNonce, KeyPair serverKey) {
        this.plugin = plugin;
        this.packetEvent = packetEvent;
        this.player = player;
        this.session = session;
        this.sharedSecret = sharedSecret;
        this.encryptedNonce = encryptedNonce;
        this.serverKey = serverKey;
    }

    @Override
    public void run() {
        try {
            verifyResponse();
        } catch (Exception ex) {
            ex.printStackTrace();
            kickPlayer(player, "Authentication error.");
        } finally {
            synchronized (packetEvent.getAsyncMarker().getProcessingLock()) {
                packetEvent.setCancelled(true);
            }
            ProtocolLibrary.getProtocolManager().getAsynchronousManager().signalPacketTransmission(packetEvent);
        }
    }

    private void verifyResponse() throws Exception {
        if (!EncryptionUtil.verifyNonce(session.getVerifyToken(), serverKey.getPrivate(), encryptedNonce)) {
            kickPlayer(player, "Invalid verify token.");
            return;
        }

        SecretKey loginKey = EncryptionUtil.decryptSharedKey(serverKey.getPrivate(), sharedSecret);
        if (!enableEncryption(player, loginKey)) {
            return;
        }

        String serverId = EncryptionUtil.getServerIdHashString("", loginKey, serverKey.getPublic());
        String ip = player.getAddress().getAddress().getHostAddress();
        UUID premiumUUID = fetchMojangProfile(session.getUsername(), serverId, ip);

        if (premiumUUID != null) {
            if (session.getClientKey() != null) {
                if (!EncryptionUtil.verifyClientKey(session.getClientKey(), Instant.now(), premiumUUID)) {
                    kickPlayer(player, "Invalid profile public key signature.");
                    return;
                }
            }

            session.setPremiumUUID(premiumUUID);
            session.setVerified(true);
            setPremiumUUID(player, premiumUUID);
            
            plugin.getVerifiedSessions().put(ip, session);
            
            receiveFakeStartPacket(player, session.getUsername(), premiumUUID, session.getClientKey());
        } else {
            kickPlayer(player, "Invalid session (Mojang rejected).");
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

    private void setPremiumUUID(Player player, UUID premiumUUID) {
        try {
            Object networkManager = getNetworkManager(player);
            Class<?> managerClass = networkManager.getClass();
            FieldAccessor accessor = Accessors.getFieldAccessorOrNull(managerClass, "spoofedUUID", UUID.class);
            if (accessor != null) {
                accessor.set(networkManager, premiumUUID);
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    private Object getNetworkManager(Player player) throws Exception {
        NettyChannelInjector injectorContainer = (NettyChannelInjector) Accessors.getMethodAccessorOrNull(
                TemporaryPlayerFactory.class, "getInjectorFromPlayer", Player.class
        ).invoke(null, player);

        FieldAccessor accessor = Accessors.getFieldAccessorOrNull(NettyChannelInjector.class, "networkManager", Object.class);
        return accessor.get(injectorContainer);
    }

    private boolean enableEncryption(Player player, SecretKey loginKey) {
        if (encryptKeyMethod == null || encryptMethod == null) {
            Class<?> networkManagerClass = MinecraftReflection.getNetworkManagerClass();
            try {
                encryptKeyMethod = FuzzyReflection.fromClass(networkManagerClass).getMethodByParameters("a", SecretKey.class);
            } catch (IllegalArgumentException exception) {
                encryptMethod = FuzzyReflection.fromClass(networkManagerClass).getMethodByParameters("a", Cipher.class, Cipher.class);
                Class<?> encryptionClass = MinecraftReflection.getMinecraftClass("util.MinecraftEncryption", "MinecraftEncryption");
                cipherMethod = FuzzyReflection.fromClass(encryptionClass).getMethodByParameters("a", int.class, Key.class);
            }
        }

        try {
            Object networkManager = getNetworkManager(player);
            if (encryptKeyMethod != null) {
                encryptKeyMethod.invoke(networkManager, loginKey);
            } else {
                Object decryptionCipher = cipherMethod.invoke(null, Cipher.DECRYPT_MODE, loginKey);
                Object encryptionCipher = cipherMethod.invoke(null, Cipher.ENCRYPT_MODE, loginKey);
                encryptMethod.invoke(networkManager, decryptionCipher, encryptionCipher);
            }
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            kickPlayer(player, "Couldn't enable encryption");
            return false;
        }
    }

    private void kickPlayer(Player player, String reason) {
        PacketContainer kickPacket = new PacketContainer(DISCONNECT);
        kickPacket.getChatComponents().write(0, WrappedChatComponent.fromText(reason));
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, kickPacket);
        player.kick(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize("Disconnect"));
    }

    private void receiveFakeStartPacket(Player player, String username, UUID uuid, ClientPublicKey clientKey) {
        PacketContainer startPacket;
        if (new MinecraftVersion(1, 20, 2).atOrAbove()) {
            startPacket = new PacketContainer(START);
            startPacket.getStrings().write(0, username);
            startPacket.getUUIDs().write(0, uuid);
        } else if (new MinecraftVersion(1, 19, 3).atOrAbove()) {
            startPacket = new PacketContainer(START);
            startPacket.getStrings().write(0, username);
            startPacket.getOptionals(Converters.passthrough(UUID.class)).write(0, Optional.of(uuid));
        } else if (new MinecraftVersion(1, 19, 0).atOrAbove()) {
            startPacket = new PacketContainer(START);
            startPacket.getStrings().write(0, username);
            
            com.comphenix.protocol.reflect.EquivalentConverter<com.comphenix.protocol.wrappers.WrappedProfilePublicKey.WrappedProfileKeyData> converter = 
                com.comphenix.protocol.wrappers.BukkitConverters.getWrappedPublicKeyDataConverter();
            
            Optional<com.comphenix.protocol.wrappers.WrappedProfilePublicKey.WrappedProfileKeyData> wrappedKey = Optional.ofNullable(clientKey).map(key ->
                new com.comphenix.protocol.wrappers.WrappedProfilePublicKey.WrappedProfileKeyData(clientKey.getExpiry(), clientKey.getKey(), clientKey.getSignature())
            );
            
            startPacket.getOptionals(converter).write(0, wrappedKey);
        } else {
            WrappedGameProfile fakeProfile = new WrappedGameProfile(uuid, username);
            Class<?> profileHandleType = fakeProfile.getHandleType();
            Class<?> packetHandleType = PacketRegistry.getPacketClassFromType(START);
            ConstructorAccessor startCons = Accessors.getConstructorAccessorOrNull(packetHandleType, profileHandleType);
            if (startCons != null) {
                startPacket = new PacketContainer(START, startCons.invoke(fakeProfile.getHandle()));
            } else {
                startPacket = new PacketContainer(START);
                startPacket.getGameProfiles().write(0, fakeProfile);
            }
        }
        ProtocolLibrary.getProtocolManager().receiveClientPacket(player, startPacket, false);
    }
}
