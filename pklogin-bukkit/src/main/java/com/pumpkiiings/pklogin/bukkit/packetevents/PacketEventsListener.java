package com.pumpkiiings.pklogin.bukkit.packetevents;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientEncryptionResponse;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientLoginStart;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerEncryptionRequest;
import com.pumpkiiings.pklogin.bukkit.PkLoginBukkit;
import com.pumpkiiings.pklogin.bukkit.protocollib.AutoLoginSession;
import com.pumpkiiings.pklogin.bukkit.protocollib.ClientPublicKey;
import com.pumpkiiings.pklogin.bukkit.protocollib.EncryptionUtil;
import com.pumpkiiings.pklogin.common.model.Account;

import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

public class PacketEventsListener extends PacketListenerAbstract {

    private final PkLoginBukkit plugin;
    private final SecureRandom random = new SecureRandom();
    private final KeyPair keyPair = EncryptionUtil.generateKeyPair();
    
    private final ConcurrentHashMap<String, AutoLoginSession> pendingSessions = new ConcurrentHashMap<>();

    public PacketEventsListener(PkLoginBukkit plugin) {
        super(PacketListenerPriority.HIGHEST);
        this.plugin = plugin;
    }

    public static void register(PkLoginBukkit plugin) {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketEventsListener(plugin));
    }

    public void addVerifiedSession(String ip, AutoLoginSession session) {
        plugin.getVerifiedSessions().put(ip, session);
    }

    public AutoLoginSession getVerifiedSession(String ip) {
        return plugin.getVerifiedSessions().remove(ip);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Login.Client.LOGIN_START) {
            WrapperLoginClientLoginStart start = new WrapperLoginClientLoginStart(event);
            String username = start.getUsername();
            InetSocketAddress address = (InetSocketAddress) event.getUser().getAddress();
            String ip = address.getAddress().getHostAddress();
            
            pendingSessions.remove(ip);

            // Check if behind proxy
            boolean isBungee = plugin.getServer().spigot().getConfig().getBoolean("settings.bungeecord", false);
            boolean isVelocity = plugin.getServer().spigot().getConfig().getBoolean("settings.velocity-support.enabled", false);
            if (isBungee || isVelocity) {
                return; // Let it pass, proxy handles it
            }

            Channel channel = (Channel) event.getUser().getChannel();
            if (isFloodgatePlayer(channel)) {
                return; // Let it pass
            }
            
            ClientPublicKey clientPublicKey = null;

            AutoLoginSession verified = plugin.getVerifiedSessions().get(ip);
            if (verified != null && verified.getUsername().equalsIgnoreCase(username)) {
                return;
            }

            Optional<Account> accountOpt = plugin.getAccountManagement().retrieveOrLoad(username);
            if (accountOpt.isPresent()) {
                String type = accountOpt.get().getUuidType();
                if ("REAL".equals(type) || "PREMIUM".equals(type)) {
                    byte[] verifyToken = EncryptionUtil.generateVerifyToken(random);
                    AutoLoginSession session = new AutoLoginSession(username, verifyToken, clientPublicKey);
                    pendingSessions.put(ip, session);
                    
                    event.setCancelled(true);
                    
                    WrapperLoginServerEncryptionRequest request = new WrapperLoginServerEncryptionRequest(
                            "", keyPair.getPublic(), verifyToken
                    );
                    event.getUser().sendPacket(request);
                }
            }
        } else if (event.getPacketType() == PacketType.Login.Client.ENCRYPTION_RESPONSE) {
            InetSocketAddress address = (InetSocketAddress) event.getUser().getAddress();
            String ip = address.getAddress().getHostAddress();
            AutoLoginSession session = pendingSessions.remove(ip);
            
            if (session != null) {
                event.setCancelled(true);
                WrapperLoginClientEncryptionResponse response = new WrapperLoginClientEncryptionResponse(event);
                
                byte[] encryptedSecret = response.getEncryptedSharedSecret();
                byte[] encryptedNonce = response.getEncryptedVerifyToken().orElse(null); 
                
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, 
                    new PacketEventsVerifyTask(plugin, event.getUser(), session, encryptedSecret, encryptedNonce, keyPair, this)
                );
            }
        }
    }

    private boolean isFloodgatePlayer(Channel channel) {
        try {
            AttributeKey<?> floodgateAttribute = AttributeKey.valueOf("floodgate-player");
            return channel.hasAttr(floodgateAttribute);
        } catch (Exception ignored) { }
        return false;
    }
}
