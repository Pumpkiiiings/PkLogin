package com.pumpkiiings.pklogin.velocity.listener;

import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.security.ProxyMessageSecurity;
import com.pumpkiiings.pklogin.velocity.PkLoginVelocity;
import com.velocitypowered.api.event.EventTask;
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
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public class VelocityListeners {

    /**
     * Grace period before announcing an auto-login to the backend, so the player
     * is fully connected there first. Not configurable: it tracks how the backend
     * sets a connection up, not anything a server owner would tune.
     */
    private static final long AUTO_LOGIN_DELAY_MILLIS = 500L;

    private final PkLoginVelocity plugin;
    private final Random random = new Random();

    public VelocityListeners(PkLoginVelocity plugin) {
        this.plugin = plugin;
    }

    /**
     * Decides whether this connection is negotiated as online or as offline.
     *
     * <p>Runs off the network thread: it reads the database and, for a name never
     * seen before, asks Mojang about it. Doing either on the connection's own
     * thread would stall every other login behind it.</p>
     */
    @Subscribe
    public EventTask onPreLogin(PreLoginEvent event) {
        return EventTask.async(() -> {
            String username = event.getUsername();

            com.pumpkiiings.pklogin.api.event.velocity.auth.VelocityPreLoginEvent apiEvent =
                new com.pumpkiiings.pklogin.api.event.velocity.auth.VelocityPreLoginEvent(username);
            plugin.getServer().getEventManager().fire(apiEvent);

            Optional<Account> accountOpt = plugin.getAccountManagement().search(username);
            if (accountOpt.isPresent()) {
                String uuidType = accountOpt.get().getUuidType();
                if ("REAL".equalsIgnoreCase(uuidType) || "PREMIUM".equalsIgnoreCase(uuidType)) {
                    event.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
                } else {
                    event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
                }
                return;
            }

            event.setResult(isUnseenNamePremium(username)
                    ? PreLoginEvent.PreLoginComponentResult.forceOnlineMode()
                    : PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
        });
    }

    /**
     * Only asked about names with no account, which is what keeps this from
     * changing anything for players who already have one.
     */
    private boolean isUnseenNamePremium(String username) {
        if (!com.pumpkiiings.pklogin.common.manager.PremiumNameLookup.isEnabled()) {
            return false;
        }
        // Anything short of a clear yes goes down the offline path. Being wrong
        // there costs a password; being wrong the other way costs the player their
        // ability to connect at all.
        return com.pumpkiiings.pklogin.common.manager.PremiumNameLookup.lookup(username)
                == com.pumpkiiings.pklogin.common.manager.PremiumNameLookup.Answer.PREMIUM;
    }

    /**
     * Gives a first-time premium player an account, now that Mojang's handshake
     * has proven the connection belongs to them. No password: every later login
     * proves itself the same way this one did.
     */
    @Subscribe
    public EventTask onPostLogin(com.velocitypowered.api.event.connection.PostLoginEvent event) {
        Player player = event.getPlayer();
        plugin.beginConnection(player);
        if (!player.isOnlineMode()) {
            return null;
        }

        return EventTask.async(() -> {
            if (plugin.getAccountManagement().search(player.getUsername()).isPresent()) {
                return;
            }

            String address = player.getRemoteAddress() == null
                    ? null
                    : player.getRemoteAddress().getAddress().getHostAddress();

            if (plugin.getAccountManagement().createPremium(player.getUsername(), address)) {
                plugin.getLogger().info("Created a passwordless premium account for {}.", player.getUsername());
            }
        });
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onServerPreConnect(ServerPreConnectEvent event) {
        if (!plugin.getBackendConfig().isOverrideFirstServer()) return;

        if (plugin.isAuthenticated(event.getPlayer())) return;

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
        plugin.endConnection(event.getPlayer());
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        String username = player.getUsername();

        // Same grace period as the auto-login below: the backend has to have the
        // player set up before it can answer on their channel.
        plugin.getServer().getScheduler()
                .buildTask(plugin, () -> plugin.getBackendVerification().checkIfNeeded(event.getServer()))
                .delay(AUTO_LOGIN_DELAY_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)
                .schedule();

        boolean isBedrock = com.pumpkiiings.pklogin.common.hook.FloodgateHook.isBedrockPlayer(player.getUniqueId());

        Optional<Account> accountOpt = plugin.getAccountManagement().search(username);
        if (isBedrock || (accountOpt.isPresent() && ("REAL".equalsIgnoreCase(accountOpt.get().getUuidType()) || "PREMIUM".equalsIgnoreCase(accountOpt.get().getUuidType())))) {
            // Give the backend a moment to finish setting the player up before
            // telling it they are already authenticated.
            plugin.getServer().getScheduler()
                .buildTask(plugin, () -> com.pumpkiiings.pklogin.velocity.ProxyAuthMessages
                        .sendPremiumAutoLogin(plugin, player))
                .delay(AUTO_LOGIN_DELAY_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)
                .schedule();

            plugin.authenticate(player);
        }
    }

    @Subscribe
    public void onGameProfileRequest(GameProfileRequestEvent event) {
        if (!com.pumpkiiings.pklogin.common.settings.Settings.APPENDER_ENABLED.asBoolean()) return;

        String username = event.getUsername();
        Optional<Account> accountOpt = plugin.getAccountManagement().search(username);

        boolean isPremium = false;
        if (accountOpt.isPresent() && ("REAL".equalsIgnoreCase(accountOpt.get().getUuidType()) || "PREMIUM".equalsIgnoreCase(accountOpt.get().getUuidType()))) {
            isPremium = true;
        }

        String newName = username;
        if (isPremium) {
            String appendix = com.pumpkiiings.pklogin.common.settings.Settings.APPENDER_PREMIUM_APPENDIX.asString();
            String pos = com.pumpkiiings.pklogin.common.settings.Settings.APPENDER_PREMIUM_POSITION.asString();
            if ("PREFIX".equalsIgnoreCase(pos)) {
                newName = appendix + newName;
            } else {
                newName = newName + appendix;
            }
        } else {
            String appendix = com.pumpkiiings.pklogin.common.settings.Settings.APPENDER_OFFLINE_APPENDIX.asString();
            String pos = com.pumpkiiings.pklogin.common.settings.Settings.APPENDER_OFFLINE_POSITION.asString();
            if ("PREFIX".equalsIgnoreCase(pos)) {
                newName = appendix + newName;
            } else {
                newName = newName + appendix;
            }
        }

        if (!newName.equals(username)) {
            if (newName.length() > 16) {
                newName = newName.substring(0, 16);
            }
            event.setGameProfile(event.getOriginalProfile().withName(newName));
        }
    }
}
