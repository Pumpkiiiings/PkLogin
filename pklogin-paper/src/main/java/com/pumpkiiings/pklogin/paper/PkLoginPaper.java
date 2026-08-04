/*
 * The MIT License (MIT)
 *
 * Copyright © 2020 - 2026 - PkLogin Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.pumpkiiings.pklogin.paper;

import com.pumpkiiings.pklogin.paper.api.OLBukkitAPI;
import com.pumpkiiings.pklogin.paper.listener.PlayerAuthenticateListener;
import com.pumpkiiings.pklogin.paper.listener.PlayerGeneralListeners;
import com.pumpkiiings.pklogin.paper.listener.PlayerJoinListeners;
import com.pumpkiiings.pklogin.paper.listener.PlayerKickListeners;
import com.pumpkiiings.pklogin.paper.task.LoginQueue;
import com.pumpkiiings.pklogin.common.PkLogin;
import com.pumpkiiings.pklogin.api.PkLoginAPI;
import com.pumpkiiings.pklogin.common.database.Database;
import com.pumpkiiings.pklogin.common.database.PluginSettings;
import com.pumpkiiings.pklogin.common.database.SQLite;
import com.pumpkiiings.pklogin.common.http.HttpClient;
import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.manager.LoginManagement;
import com.pumpkiiings.pklogin.common.model.Title;
import com.pumpkiiings.pklogin.common.security.filter.LoggerFilterManager;
import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.common.settings.Settings;
import com.pumpkiiings.pklogin.common.util.FileUtils;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

@Getter
public class PkLoginPaper extends JavaPlugin {

    private LoginManagement loginManagement;
    private AccountManagement accountManagement;

    private Database database;
    private PluginSettings pluginSettings;

    private final java.util.concurrent.ConcurrentHashMap<String, com.pumpkiiings.pklogin.paper.autologin.protocollib.AutoLoginSession> verifiedSessions = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * How long a premium handshake result stays usable. It only has to survive
     * the gap between the login packets and {@code PlayerJoinEvent}.
     */
    private static long verifiedSessionTtlMillis() {
        return Settings.AUTOLOGIN_PREMIUM_SESSION_TIMEOUT.asInt() * 1000L;
    }

    /** Records a completed premium handshake for the given address. */
    public void storeVerifiedSession(String ip, com.pumpkiiings.pklogin.paper.autologin.protocollib.AutoLoginSession session) {
        purgeExpiredSessions();
        verifiedSessions.put(ip, session);
    }

    /**
     * Returns the still-valid verified session for this address and username,
     * removing it. Returns null when there is none, the username does not match,
     * or the session has gone stale — sessions are keyed by IP, so a leftover one
     * must never authenticate a later connection.
     */
    public com.pumpkiiings.pklogin.paper.autologin.protocollib.AutoLoginSession consumeVerifiedSession(String ip, String username) {
        purgeExpiredSessions();
        com.pumpkiiings.pklogin.paper.autologin.protocollib.AutoLoginSession session = verifiedSessions.get(ip);
        if (session == null) return null;
        if (!session.getUsername().equalsIgnoreCase(username)) return null;
        verifiedSessions.remove(ip, session);
        return session;
    }

    /** Same checks as {@link #consumeVerifiedSession} but leaves the session in place. */
    public boolean hasVerifiedSession(String ip, String username) {
        purgeExpiredSessions();
        com.pumpkiiings.pklogin.paper.autologin.protocollib.AutoLoginSession session = verifiedSessions.get(ip);
        return session != null && session.getUsername().equalsIgnoreCase(username);
    }

    private void purgeExpiredSessions() {
        long ttl = verifiedSessionTtlMillis();
        verifiedSessions.values().removeIf(session -> session.isExpired(ttl));
    }

    private String latestVersion;
    private boolean updateAvailable;
    private int registeredUsers;

    /**
     * Key for verifying proxy messages. Resolved once at startup rather than per
     * message, so a plugin message never turns into a disk read.
     */
    private volatile String proxySecret;

    /** @return the signing key, or null when none is configured or detectable */
    public String getProxySecret() {
        return proxySecret;
    }

    /** Re-reads the proxy signing key after a configuration reload. */
    public void reloadProxySecret() {
        resolveProxySecret();
    }

    /**
     * Works out the proxy signing key and reports where it came from.
     *
     * <p>Called at enable and again on reload, so changing the secret does not
     * need a full restart.</p>
     */
    private void resolveProxySecret() {
        com.pumpkiiings.pklogin.common.security.ProxySecretResolver.Resolution resolution =
                com.pumpkiiings.pklogin.common.security.ProxySecretResolver.resolve(
                        PaperProxyConfig.readVelocityForwardingSecret());

        this.proxySecret = resolution.getKey();

        if (!PaperProxyConfig.isBehindProxy()) {
            return;
        }

        if (resolution.isPresent()) {
            sendMessage("Proxy messages authenticated using " + resolution.getSource() + ".");
            return;
        }

        sendMessage("§c=========================================================");
        sendMessage("§cPremium auto-login from the proxy is DISABLED.");
        sendMessage("§cThere is no key to verify the proxy's messages with, and this server");
        sendMessage("§ccannot check premium accounts itself while it is behind a proxy.");
        if (PaperProxyConfig.isBungeeForwardingEnabled() && !PaperProxyConfig.isVelocityForwardingEnabled()) {
            sendMessage("§cThis server uses BungeeCord legacy forwarding ('settings.bungeecord: true'");
            sendMessage("§cin spigot.yml), which shares no secret PkLogin can derive a key from.");
            sendMessage("§cSwitch the network to Velocity with modern forwarding.");
        } else {
            sendMessage("§cSet 'proxies.velocity.enabled: true' and 'proxies.velocity.secret' in");
            sendMessage("§cconfig/paper-global.yml to the secret your Velocity proxy uses.");
        }
        sendMessage("§c=========================================================");
    }

    /**
     * Explains what a standalone server loses without ProtocolLib or PacketEvents.
     *
     * <p>The limitation is Paper's, not PkLogin's. With {@code online-mode=false}
     * the server never asks anyone to authenticate with Mojang, and there is no
     * API to ask just one player to. Sending that request means writing the packet
     * by hand, which is what those two libraries are for. Velocity has the same
     * ability built in, so a proxied network needs neither.</p>
     *
     * <p>Spelled out at length only when the server owner has premium logins
     * switched on, because only then is something they asked for missing.</p>
     */
    private void reportMissingPacketLibrary() {
        if (!com.pumpkiiings.pklogin.common.settings.Settings.AUTOLOGIN_PREMIUM_ENABLE.asBoolean()) {
            sendMessage("Premium Auto-Login is off in config.yml.");
            return;
        }

        sendMessage("§c=========================================================");
        sendMessage("§cPremium Auto-Login is DISABLED: no packet library was found.");
        sendMessage("§cPremium players will have to /register and use a password, and new");
        sendMessage("§cones will not be let in without one either.");
        sendMessage("§c");
        sendMessage("§cInstall §fPacketEvents §cor §fProtocolLib §cand restart. This server runs");
        sendMessage("§cwith online-mode=false, so it never asks anyone to authenticate with");
        sendMessage("§cMojang, and Paper offers no way to ask just one player to. Those");
        sendMessage("§clibraries provide it; behind a Velocity proxy it comes for free.");
        sendMessage("§c");
        sendMessage("§cIf you do not want premium logins at all, set autologin.premium.enable");
        sendMessage("§cto false in config.yml and this message will stop.");
        sendMessage("§c=========================================================");
    }

    public void onEnable() {
        PluginManager pm = getServer().getPluginManager();



        Server server = getServer();

        File newUserfile = new File(getDataFolder(), "new-user");
        if (newUserfile.exists()) {
            newUserfile.delete();
        }

        // setup config
        if (!setupSettings()) {
            server.shutdown();
            return;
        }

        // setup database
        if (!setupDatabase()) {
            server.shutdown();
            return;
        }

        String c = "§b";
        String lg = "§7";
        String dg = "§8";
        String aq = "§b";
        String a = "§e";
        sendMessage(a + "▄▄▄▄▄▄▄          ▄▄▄                      ");
        sendMessage(a + "███▀▀███▄ ▄▄     ███                  ▀▀ ");
        sendMessage(a + "███▄▄███▀ ██ ▄█▀ ███      ▄███▄ ▄████ ██  ████▄ ");
        sendMessage(a + "███▀▀▀▀   ████   ███      ██ ██ ██ ██ ██  ██ ██ ");
        sendMessage(a + "███       ██ ▀█▄ ████████ ▀███▀ ▀████ ██▄ ██ ██ ");
        sendMessage(a + "                                   ██  ");
        sendMessage(a + "                                 ▀▀▀  ");
        sendMessage(dg + "A powerful open source login plugin");
        sendMessage(lg + "Support: " + aq + com.pumpkiiings.pklogin.common.PluginConstants.DISCORD_INVITE);
        sendMessage(lg + "Database Type: " + aq
                + com.pumpkiiings.pklogin.common.settings.Settings.DATABASE_TYPE.asString());
        sendMessage(lg + "Version: " + aq + getDescription().getVersion());
        sendMessage(lg + "Source: " + aq + com.pumpkiiings.pklogin.common.PluginConstants.GITHUB);
        sendMessage("");
        sendMessage("§e" + "Thanks for use my plugin!");
        sendMessage("");

        // setup Folia lib

        // setup account management
        accountManagement = new AccountManagement(database);

        // setup login management
        loginManagement = new LoginManagement(accountManagement);

        com.pumpkiiings.pklogin.common.PkLogin.setAccountManagement(accountManagement);
        com.pumpkiiings.pklogin.common.PkLogin.setLoginManagement(loginManagement);
        
        loginManagement.setAuthCallback(playerName -> {
            org.bukkit.entity.Player player = getServer().getPlayer(playerName);
            if (player != null) {
                if (getServer().isPrimaryThread()) {
                    runAsync(() -> getServer().getPluginManager().callEvent(new com.pumpkiiings.pklogin.api.event.bukkit.auth.PlayerAuthLoginEvent(player)));
                } else {
                    getServer().getPluginManager().callEvent(new com.pumpkiiings.pklogin.api.event.bukkit.auth.PlayerAuthLoginEvent(player));
                }
            }
        });
        
        accountManagement.setPasswordChangeCallback(playerName -> {
            org.bukkit.entity.Player player = getServer().getPlayer(playerName);
            if (player != null) {
                getServer().getPluginManager().callEvent(new com.pumpkiiings.pklogin.api.event.bukkit.auth.PlayerPasswordChangeEvent(player));
            }
        });

        com.pumpkiiings.pklogin.api.service.PkLoginProvider.registerAccountManager(new com.pumpkiiings.pklogin.common.api.CommonAccountManagerAPI());
        com.pumpkiiings.pklogin.api.service.PkLoginProvider.registerSecurityAPI(new com.pumpkiiings.pklogin.common.api.CommonSecurityAPI());
        com.pumpkiiings.pklogin.api.service.PkLoginProvider.registerSessionAPI(
                new com.pumpkiiings.pklogin.paper.api.BukkitSessionAPI(this));


        // setup commands
        this.getLifecycleManager().registerEventHandler(io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents.COMMANDS, event -> {
            com.pumpkiiings.pklogin.paper.command.CommandRegistrar.register(this, event.registrar());
        });

        // setup logger filter
        LoggerFilterManager.setup(getLogger());

        // setup listeners
        setupListeners();

        // setup Captcha Handler
        new com.pumpkiiings.pklogin.paper.captcha.PaperCaptchaHandler(this);

        // setup PacketEvents or ProtocolLib auto-login
        if (PaperProxyConfig.isBehindProxy()) {
            // The proxy owns the login handshake, so the packet-level hooks would
            // have nothing to intercept here.
            sendMessage("This server is behind a proxy. Premium Auto-Login is handled there.");
        } else if (getServer().getPluginManager().getPlugin("packetevents") != null) {
            sendMessage("PacketEvents detected. Using PacketEvents for Premium Auto-Login.");
            com.pumpkiiings.pklogin.paper.autologin.packetevents.PacketEventsHook.init(this);
        } else if (getServer().getPluginManager().getPlugin("ProtocolLib") != null) {
            sendMessage("ProtocolLib detected. Using ProtocolLib for Premium Auto-Login.");
            com.pumpkiiings.pklogin.paper.autologin.protocollib.ProtocolLibHook.init(this);
        } else {
            reportMissingPacketLibrary();
        }

        // start login queue task
        LoginQueue.startTask(this);

        // setup api
        PkLogin.setApi(new OLBukkitAPI(this));
        PkLogin.setAccountManagement(accountManagement);

        resolveProxySecret();

        String channel = com.pumpkiiings.pklogin.common.PluginConstants.CHANNEL_MAIN;
        getServer().getMessenger().registerOutgoingPluginChannel(this, channel);
        getServer().getMessenger().registerIncomingPluginChannel(this, channel,
                new com.pumpkiiings.pklogin.paper.listener.ProxyMessageListener(this));

        // updates
        if (Settings.UPDATES_CHECK.asBoolean()) {
            runAsync(this::detectUpdates);
        }

        com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorManager.getInstance().init(getDataFolder());

        if (getServer().getOnlineMode()) {
            sendMessage("§c=========================================================");
            sendMessage("§cWARNING: online-mode is set to true in server.properties!");
            sendMessage("§cIf this server only allows premium players (no Proxy), you don't need a login plugin!");
            sendMessage("§cIf you are using a proxy (like BungeeCord/Velocity), please set online-mode to false.");
            sendMessage("§c=========================================================");
        }
    }

    @Override
    public void onDisable() {
        com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorManager.getInstance().shutdown();
    }

    public void runAsync(Runnable runnable) {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            getServer().getAsyncScheduler().runNow(this, task -> runnable.run());
        } catch (ClassNotFoundException e) {
            getServer().getScheduler().runTaskAsynchronously(this, runnable);
        }
    }

    public void sendMessage(String message) {
        getServer().getConsoleSender().sendMessage("[" + getName() + "] " + message);
    }

    public void sendMessage(String message, String color) {
        getServer().getConsoleSender().sendMessage(color + "[" + getName() + "] " + message);
    }

    private boolean setupDatabase() {
        database = com.pumpkiiings.pklogin.common.database.DatabaseFactory.create(
                Settings.DATABASE_TYPE.asString(), getDataFolder());

        try {
            database.openConnection();
            com.pumpkiiings.pklogin.common.database.DatabaseSchema.apply(database,
                    warning -> getLogger().warning(warning));

            try (Database.Query query = database.query("SELECT COUNT(*) FROM `pklogin`")) {
                ResultSet rs = query.resultSet;
                if (rs.next()) {
                    // By position: engines label an aggregate column differently
                    // (PostgreSQL calls it "count"), and there is only one here.
                    registeredUsers = rs.getInt(1);
                }
            } catch (Exception e) {
                sendMessage("§cFailed to update the register count.");
            }
            pluginSettings = new PluginSettings(database);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            sendMessage("§cFailed to start database. Shutting down server...");
            return false;
        }
    }

    private void setupListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerGeneralListeners(this), this);
        pm.registerEvents(new PlayerJoinListeners(this), this);
        pm.registerEvents(new PlayerKickListeners(this), this);
        pm.registerEvents(new PlayerAuthenticateListener(this), this);
    }

    public void detectUpdates() {
        String tagName = null;
        try {
            String result = HttpClient.DEFAULT.get(
                    com.pumpkiiings.pklogin.common.PluginConstants.LATEST_RELEASE_API);

            // avoid use Google Gson to avoid problems with older versions.
            if (result.contains("\"tag_name\":\"")) {
                tagName = result.split("\"tag_name\":\"")[1];
                if (tagName.contains("\",")) {
                    tagName = latestVersion = tagName.split("\",")[0];
                }
            }
        } catch (IOException e) {
            sendMessage(Messages.UPDATES_FAILED.asString());
            sendMessage(Messages.UPDATES_DOWNLOAD_LINK.asString());
            return;
        }
        if (tagName == null) {
            sendMessage(Messages.UPDATES_INVALID_RESPONSE.asString());
            sendMessage(Messages.UPDATES_DOWNLOAD_LINK.asString());
        } else {
            String currentVersion = "v" + getDescription().getVersion();
            updateAvailable = !currentVersion.equals(tagName);
            if (updateAvailable) {
                sendMessage(Messages.UPDATES_AVAILABLE.asString().replace("{0}", currentVersion).replace("{1}", latestVersion));
            }
        }
    }

    public boolean setupSettings() {
        try {
            com.pumpkiiings.pklogin.common.config.ConfigurationVersionManager configManager =
                com.pumpkiiings.pklogin.common.config.migrations.ConfigMigrations.applyTo(
                    new com.pumpkiiings.pklogin.common.config.ConfigurationVersionManager(
                        new File(getDataFolder(), "config.yml"),
                        com.pumpkiiings.pklogin.common.util.PluginResources.openConfig(),
                        com.pumpkiiings.pklogin.common.config.ConfigurationVersionManager.VERSION_KEY_CONFIG,
                        com.pumpkiiings.pklogin.common.config.migrations.ConfigMigrations.CURRENT_VERSION,
                        com.pumpkiiings.pklogin.common.config.MigrationLogger.of(getLogger())));

            dev.dejvokep.boostedyaml.YamlDocument configDoc = configManager.load();

            Settings.clear();
            for (Settings setting : Settings.values()) {
                Settings.define(setting, configDoc.get(setting.getKey()));
            }
        } catch (Exception e) {
            getLogger().log(java.util.logging.Level.SEVERE, "Failed to load config.yml", e);
            return false;
        }

        File twoFaFolder = new File(getDataFolder(), "2fa");
        if (!twoFaFolder.exists()) {
            twoFaFolder.mkdirs();
        }

        File discordFile = new File(twoFaFolder, "discord.yml");
        if (!discordFile.exists()
                && !FileUtils.copyFromJar(
                        com.pumpkiiings.pklogin.common.util.PluginResources.TWO_FACTOR_DIR + "discord.yml", discordFile)) {
            sendMessage("§cFailed to create 'discord.yml' file.");
        }

        File emailFile = new File(twoFaFolder, "email.yml");
        if (!emailFile.exists()
                && !FileUtils.copyFromJar(
                        com.pumpkiiings.pklogin.common.util.PluginResources.TWO_FACTOR_DIR + "email.yml", emailFile)) {
            sendMessage("§cFailed to create 'email.yml' file.");
        }

        String lang = Settings.LANGUAGE_FILE.asString();
        File messagesFile = new File(getDataFolder() + "/lang", lang);

        java.io.InputStream defaultResource =
                com.pumpkiiings.pklogin.common.util.PluginResources.openLanguage(lang);

        try {
            com.pumpkiiings.pklogin.common.config.ConfigurationVersionManager messagesManager =
                com.pumpkiiings.pklogin.common.config.migrations.MessagesMigrations.applyTo(
                    new com.pumpkiiings.pklogin.common.config.ConfigurationVersionManager(
                        messagesFile,
                        defaultResource,
                        com.pumpkiiings.pklogin.common.config.ConfigurationVersionManager.VERSION_KEY_MESSAGES,
                        com.pumpkiiings.pklogin.common.config.migrations.MessagesMigrations.CURRENT_VERSION,
                        com.pumpkiiings.pklogin.common.config.MigrationLogger.of(getLogger())));

            dev.dejvokep.boostedyaml.YamlDocument messagesConfig = messagesManager.load();

            for (Messages message : Messages.values()) {
                String path = message.getKey();
                if (path.startsWith("Messages.Title")) {
                    path = path + ".";
                    if (messagesConfig.get(path + "title") != null && messagesConfig.get(path + "subtitle") != null) {
                        String title = messagesConfig.getString(path + "title");
                        String subtitle = messagesConfig.getString(path + "subtitle");
                        int start = messagesConfig.getInt(path + "delays.start", 0);
                        int duration = messagesConfig.getInt(path + "delays.duration", 60);
                        int end = messagesConfig.getInt(path + "delays.end", 6);
                        Messages.define(message, new Title(title, subtitle, start, duration, end));
                    }
                } else if (messagesConfig.get(path) != null) {
                    Object obj = messagesConfig.get(path);
                    if (obj instanceof java.util.List) {
                        Messages.define(message, obj);
                    } else {
                        Messages.define(message, String.valueOf(obj));
                    }
                }
            }
        } catch (Exception e) {
            getLogger().log(java.util.logging.Level.SEVERE, "Failed to load messages file", e);
            return false;
        }
        return true;
    }

    public static PkLoginAPI getApi() {
        return PkLogin.getApi();
    }
}
