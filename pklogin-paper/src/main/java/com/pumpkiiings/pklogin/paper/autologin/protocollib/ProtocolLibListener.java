package com.pumpkiiings.pklogin.paper.autologin.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
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
import com.pumpkiiings.pklogin.common.model.Account;
import org.bukkit.entity.Player;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Key;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.time.Instant;
import java.security.PublicKey;
import com.comphenix.protocol.wrappers.WrappedProfilePublicKey.WrappedProfileKeyData;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.util.AttributeKey;

import static com.comphenix.protocol.PacketType.Login.Client.ENCRYPTION_BEGIN;
import static com.comphenix.protocol.PacketType.Login.Client.START;
import static com.comphenix.protocol.PacketType.Login.Server.DISCONNECT;

public class ProtocolLibListener extends PacketAdapter {

    private final PkLoginPaper plugin;
    private final KeyPair keyPair;
    private final java.util.Random random = new java.util.Random();
    
    // Map of InetAddress/IP to pending sessions
    private final ConcurrentHashMap<String, AutoLoginSession> pendingSessions = new ConcurrentHashMap<>();

    public void addVerifiedSession(String ip, AutoLoginSession session) {
        plugin.getVerifiedSessions().put(ip, session);
    }

    public ProtocolLibListener(PkLoginPaper plugin) {
        super(params()
                .plugin(plugin)
                .types(START, ENCRYPTION_BEGIN)
                .optionAsync());
        this.plugin = plugin;
        this.keyPair = EncryptionUtil.generateKeyPair();
    }

    public static void register(PkLoginPaper plugin) {
        ProtocolLibrary.getProtocolManager().getAsynchronousManager()
                .registerAsyncHandler(new ProtocolLibListener(plugin)).start();
    }
    
    public AutoLoginSession getVerifiedSession(String ip) {
        return plugin.getVerifiedSessions().remove(ip);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        PacketType packetType = getOverriddenType(event.getPacketType());

        try {
            if (packetType == START) {
                onLoginStart(event, player);
            } else if (packetType == ENCRYPTION_BEGIN) {
                onEncryptionBegin(event, player);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private PacketType getOverriddenType(PacketType packetType) {
        if (packetType.isDynamic()) {
            String vanillaName = packetType.getPacketClass().getName();
            if (vanillaName.endsWith("ServerboundHelloPacket")) return START;
            if (vanillaName.endsWith("ServerboundKeyPacket")) return ENCRYPTION_BEGIN;
        }
        return packetType;
    }

    private void onLoginStart(PacketEvent event, Player player) {
        PacketContainer packet = event.getPacket();
        String username;
        WrappedGameProfile profile = packet.getGameProfiles().readSafely(0);
        if (profile == null) {
            username = packet.getStrings().read(0);
        } else {
            username = profile.getName();
        }

        String ip = player.getAddress().getAddress().getHostAddress();
        pendingSessions.remove(ip); // Clear old

        // Check if behind proxy
        boolean isBungee = plugin.getServer().spigot().getConfig().getBoolean("settings.bungeecord", false);
        boolean isVelocity = plugin.getServer().spigot().getConfig().getBoolean("settings.velocity-support.enabled", false);
        
        // Modern Paper support (1.19+)
        java.io.File paperGlobal = new java.io.File("config/paper-global.yml");
        if (paperGlobal.exists()) {
            try {
                org.bukkit.configuration.file.YamlConfiguration paperConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(paperGlobal);
                if (paperConfig.getBoolean("proxies.velocity.enabled", false)) {
                    isVelocity = true;
                }
            } catch (Exception ignored) {}
        }

        if (isBungee || isVelocity) {
            return; // Let it pass, proxy handles it
        }

        if (isFloodgatePlayer(player)) {
            processFloodgateTasks(event);
            return; // Bedrock players don't need Java premium encryption
        }

        Optional<ClientPublicKey> clientKey = Optional.empty();
        if (!new MinecraftVersion(1, 19, 3).atOrAbove() && new MinecraftVersion(1, 19, 0).atOrAbove()) {
            Optional<Optional<WrappedProfileKeyData>> profileKey = packet.getOptionals(
                    com.comphenix.protocol.wrappers.BukkitConverters.getWrappedPublicKeyDataConverter()
            ).optionRead(0);
            
            if (profileKey != null && profileKey.isPresent()) {
                clientKey = profileKey.get().map(data -> 
                    new ClientPublicKey(data.getExpireTime(), data.getKey(), data.getSignature())
                );
            }
        }

        AutoLoginSession verified = plugin.getVerifiedSessions().get(ip);
        if (verified != null && verified.getUsername().equalsIgnoreCase(username)) {
            return;
        }

        // Check if player has PREMIUM account
        Optional<Account> accountOpt = plugin.getAccountManagement().retrieveOrLoad(username);
        if (accountOpt.isPresent()) {
            String type = accountOpt.get().getUuidType();
            if ("REAL".equals(type) || "PREMIUM".equals(type)) {
                // Request Premium Login!
                AutoLoginSession session = new AutoLoginSession(username, EncryptionUtil.generateVerifyToken(random), clientKey.orElse(null));
                pendingSessions.put(ip, session);
                
                // Cancel START packet processing so server waits
                synchronized (event.getAsyncMarker().getProcessingLock()) {
                    event.setCancelled(true);
                }

                // Send Encryption Request
                sendEncryptionRequest(player, session.getVerifyToken());
            }
        }
    }

    private boolean isFloodgatePlayer(Player player) {
        try {
            Channel channel = getChannel(player);
            AttributeKey<?> floodgateAttribute = AttributeKey.valueOf("floodgate-player");
            return channel.hasAttr(floodgateAttribute);
        } catch (Exception ignored) { }
        return false;
    }

    private void processFloodgateTasks(PacketEvent packetEvent) {
        try {
            Channel channel = getChannel(packetEvent.getPlayer());
            ChannelHandler floodgateHandler = channel.pipeline().get("floodgate_data_handler");
            if (floodgateHandler != null) {
                channel.pipeline().remove(floodgateHandler);
            }
        } catch (Exception ignored) { }
    }

    private static Channel getChannel(Player player) throws Exception {
        NettyChannelInjector injector = (NettyChannelInjector) Accessors.getMethodAccessorOrNull(
                TemporaryPlayerFactory.class, "getInjectorFromPlayer", Player.class
        ).invoke(null, player);
        return FuzzyReflection.getFieldValue(injector, Channel.class, true);
    }

    private void sendEncryptionRequest(Player player, byte[] verifyToken) {
        PacketContainer newPacket = new PacketContainer(PacketType.Login.Server.ENCRYPTION_BEGIN);
        newPacket.getStrings().write(0, "");

        int verifyField = 0;
        if (newPacket.getSpecificModifier(PublicKey.class).getFields().isEmpty()) {
            // Since 1.16.4 this is now a byte array
            newPacket.getByteArrays().write(0, keyPair.getPublic().getEncoded());
            verifyField++;
        } else {
            newPacket.getSpecificModifier(PublicKey.class).write(0, keyPair.getPublic());
        }

        newPacket.getByteArrays().write(verifyField, verifyToken);
        newPacket.getBooleans().writeSafely(0, true);

        ProtocolLibrary.getProtocolManager().sendServerPacket(player, newPacket);
    }

    private void onEncryptionBegin(PacketEvent event, Player player) {
        String ip = player.getAddress().getAddress().getHostAddress();
        AutoLoginSession session = pendingSessions.remove(ip);

        if (session == null) {
            kickPlayer(player, "Invalid session state.");
            return;
        }

        byte[] sharedSecret = event.getPacket().getByteArrays().read(0);
        byte[] encryptedNonce = null;
        if (new MinecraftVersion(1, 19, 0).atOrAbove() && !new MinecraftVersion(1, 19, 3).atOrAbove()) {
            try {
                Class<?> eitherClass = Class.forName("com.mojang.datafixers.util.Either");
                Object either = event.getPacket().getSpecificModifier(eitherClass).readSafely(0);
                if (either != null) {
                    Optional<?> left = (Optional<?>) eitherClass.getMethod("left").invoke(either);
                    if (left.isPresent()) {
                        encryptedNonce = (byte[]) left.get();
                    }
                }
            } catch (Exception ignored) { }
        } else {
            encryptedNonce = event.getPacket().getByteArrays().readSafely(1);
        }

        if (encryptedNonce == null) {
            kickPlayer(player, "Missing verify token.");
            return;
        }

        event.getAsyncMarker().incrementProcessingDelay();
        
        final byte[] finalEncryptedNonce = encryptedNonce;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, 
            new VerifyResponseTask(plugin, event, player, session, sharedSecret, finalEncryptedNonce, keyPair)
        );
    }

    private void kickPlayer(Player player, String reason) {
        PacketContainer kickPacket = new PacketContainer(DISCONNECT);
        kickPacket.getChatComponents().write(0, WrappedChatComponent.fromText(reason));
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, kickPacket);
        player.kick(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize("Disconnect"));
    }
}
