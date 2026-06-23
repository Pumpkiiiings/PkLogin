package com.pumpkiiings.pklogin.velocity.listener;

import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.velocity.PkLoginVelocity;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public class VelocityListeners {

    private final PkLoginVelocity plugin;
    private final Random random = new Random();

    public VelocityListeners(PkLoginVelocity plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        String username = event.getUsername();
        
        Optional<Account> accountOpt = plugin.getAccountManagement().search(username);
        if (accountOpt.isPresent()) {
            String uuidType = accountOpt.get().getUuidType();
            if ("REAL".equalsIgnoreCase(uuidType) || "PREMIUM".equalsIgnoreCase(uuidType)) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
            } else {
                event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
            }
        } else {
            // New user, determine based on config if we want them offline by default
            event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
        }
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onServerPreConnect(ServerPreConnectEvent event) {
        if (!plugin.getBackendConfig().isOverrideFirstServer()) return;

        UUID uuid = event.getPlayer().getUniqueId();
        if (plugin.getAuthenticatedPlayers().contains(uuid)) return; // already authenticated

        Optional<RegisteredServer> targetOpt = event.getResult().getServer();
        if (targetOpt.isEmpty()) return;

        String targetName = targetOpt.get().getServerInfo().getName();
        List<String> authServers = plugin.getBackendConfig().getAuthServers();
        
        // If they're already heading to a configured auth server, let it through.
        if (authServers.contains(targetName)) return;

        if (authServers.isEmpty()) return;

        // Redirect to a random auth server
        String selectedAuthServer = authServers.get(random.nextInt(authServers.size()));
        Optional<RegisteredServer> server = plugin.getServer().getServer(selectedAuthServer);

        if (server.isPresent()) {
            event.setResult(ServerPreConnectEvent.ServerResult.allowed(server.get()));
        } else {
            plugin.getLogger().warn("Auth server '{}' not found in proxy config.", selectedAuthServer);
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public void onDisconnect(DisconnectEvent event) {
        plugin.getAuthenticatedPlayers().remove(event.getPlayer().getUniqueId());
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        String username = player.getUsername();

        boolean isBedrock = com.pumpkiiings.pklogin.common.hook.FloodgateHook.isBedrockPlayer(player.getUniqueId());

        Optional<Account> accountOpt = plugin.getAccountManagement().search(username);
        if (isBedrock || (accountOpt.isPresent() && ("REAL".equalsIgnoreCase(accountOpt.get().getUuidType()) || "PREMIUM".equalsIgnoreCase(accountOpt.get().getUuidType())))) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("PremiumAutoLogin");
            out.writeUTF(username);

            event.getServer().sendPluginMessage(PluginMessageListener.IDENTIFIER, out.toByteArray());
            
            plugin.getAuthenticatedPlayers().add(player.getUniqueId());
        }
    }
}
