package com.pumpkiiings.pklogin.velocity.listener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.pumpkiiings.pklogin.velocity.PkLoginVelocity;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class PluginMessageListener {

    public static final MinecraftChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.from("pklogin:main");

    private final PkLoginVelocity plugin;
    private final Random random = new Random();

    public PluginMessageListener(PkLoginVelocity plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(IDENTIFIER)) {
            return;
        }

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if (!(event.getTarget() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getTarget();
        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String subChannel = in.readUTF();

        if ("Authenticated".equals(subChannel)) {
            // Player has successfully logged in or registered on the auth server
            plugin.getAuthenticatedPlayers().add(player.getUniqueId());
            
            com.pumpkiiings.pklogin.api.event.velocity.auth.VelocityPlayerAuthLoginEvent apiEvent = 
                new com.pumpkiiings.pklogin.api.event.velocity.auth.VelocityPlayerAuthLoginEvent(player);
            plugin.getServer().getEventManager().fire(apiEvent);
            
            handleAuthenticationSuccess(player);
        }
    }

    private void handleAuthenticationSuccess(Player player) {
        if (!plugin.getBackendConfig().isAfterAuthEnabled()) {
            return;
        }

        List<String> servers = plugin.getBackendConfig().getAfterAuthServers();
        if (servers.isEmpty()) return;

        String targetServer = servers.get(random.nextInt(servers.size()));
        Optional<RegisteredServer> server = plugin.getServer().getServer(targetServer);

        if (server.isPresent()) {
            player.createConnectionRequest(server.get()).fireAndForget();
        } else {
            plugin.getLogger().warn("Target server {} not found for redirection after auth.", targetServer);
        }
    }
}
