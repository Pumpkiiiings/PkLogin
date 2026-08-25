package com.pumpkiiings.pklogin.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.io.File;

import com.pumpkiiings.pklogin.common.database.Database;
import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.settings.Settings;
import com.pumpkiiings.pklogin.velocity.listener.PluginMessageListener;
import com.pumpkiiings.pklogin.velocity.listener.VelocityListeners;
import com.pumpkiiings.pklogin.velocity.config.BackendConfig;
import com.pumpkiiings.pklogin.common.config.ConfigurationVersionManager;
import com.pumpkiiings.pklogin.common.util.PluginResources;
import dev.dejvokep.boostedyaml.YamlDocument;

@Plugin(id = "pklogin", name = "PkLogin", version = "2.1", authors = {"Pumpkiiiings"})
public class PkLoginVelocity {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public PkLoginVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }



    private static PkLoginVelocity instance;
    private BackendConfig backendConfig;
    private BackendVerification backendVerification;
    private YamlDocument yamlConfig;
    private Database database;
    private AccountManagement accountManagement;

    private final java.util.Set<java.util.UUID> authenticatedPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Key for signing messages to the backends; resolved once, refreshed on reload. */
    private volatile String proxySecret;

    /** @return the signing key, or null when none is configured or detectable */
    public String getProxySecret() {
        return proxySecret;
    }

    /**
     * Works out the signing key and reports where it came from.
     *
     * <p>With Velocity modern forwarding the proxy and its backends already share
     * a secret, so nothing needs configuring. Without it there is no key at all,
     * and premium auto-login stays off rather than running unauthenticated.</p>
     */
    public void resolveProxySecret() {
        String forwardingSecret = VelocityProxyConfig.readForwardingSecret(dataDirectory);
        com.pumpkiiings.pklogin.common.security.ProxySecretResolver.Resolution resolution =
                com.pumpkiiings.pklogin.common.security.ProxySecretResolver.resolve(forwardingSecret);

        this.proxySecret = resolution.getKey();

        if (resolution.isPresent()) {
            logger.info("Backend messages will be authenticated using {}.", resolution.getSource());
            return;
        }

        logger.error("=========================================================");
        logger.error("Premium auto-login is DISABLED: there is no key to sign backend messages with.");
        if (!VelocityProxyConfig.isModernForwarding(dataDirectory)) {
            logger.error("This proxy is not using modern player info forwarding, so there is no");
            logger.error("shared secret to derive one from. Set player-info-forwarding-mode = \"modern\"");
            logger.error("in velocity.toml, and point every backend's config/paper-global.yml at the");
            logger.error("same secret under 'proxies.velocity'.");
        } else {
            logger.error("Modern forwarding is on but its secret could not be read. Check that");
            logger.error("the file named by 'forwarding-secret-file' in velocity.toml exists and is readable.");
        }
        logger.error("=========================================================");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        instance = this;

        // Download and inject dependencies first
        DependencyDownloader downloader = new DependencyDownloader(this, server.getPluginManager(), logger, dataDirectory.toFile());
        downloader.loadDependencies();

        File dataFolder = dataDirectory.toFile();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        // Load config.yml using ConfigurationVersionManager
        try {
            this.yamlConfig = newConfigManager(dataFolder).load();

            for (Settings setting : Settings.values()) {
                Object val = yamlConfig.get(setting.getKey());
                if (val != null) {
                    Settings.define(setting, val);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load config.yml", e);
        }

        resolveProxySecret();

        // Setup messages
        setupMessages(dataFolder);

        this.backendConfig = new BackendConfig(dataDirectory.resolve("backend.yml"), migrationLogger());
        try {
            this.backendConfig.load();
        } catch (Exception e) {
            logger.error("Failed to load backend.yml", e);
        }

        this.backendVerification = new BackendVerification(this);
        this.backendVerification.reportUnknownAuthServers();

        try {
            database = com.pumpkiiings.pklogin.common.database.DatabaseFactory.create(
                    com.pumpkiiings.pklogin.common.settings.Settings.DATABASE_TYPE.asString(), dataFolder);
            database.openConnection();
            com.pumpkiiings.pklogin.common.database.DatabaseSchema.apply(database, logger::warn);

            this.accountManagement = new AccountManagement(this.database);
            this.accountManagement.setPasswordChangeCallback(playerName -> {
                this.getServer().getPlayer(playerName).ifPresent(player -> {
                    this.getServer().getEventManager().fire(new com.pumpkiiings.pklogin.api.event.velocity.auth.VelocityPlayerPasswordChangeEvent(player));
                });
            });
            com.pumpkiiings.pklogin.common.PkLogin.setAccountManagement(this.accountManagement);
            com.pumpkiiings.pklogin.api.service.PkLoginProvider.registerAccountManager(new com.pumpkiiings.pklogin.common.api.CommonAccountManagerAPI());
            com.pumpkiiings.pklogin.api.service.PkLoginProvider.registerSecurityAPI(new com.pumpkiiings.pklogin.common.api.CommonSecurityAPI());
            com.pumpkiiings.pklogin.api.service.PkLoginProvider.registerSessionAPI(new com.pumpkiiings.pklogin.velocity.api.VelocitySessionAPI(this));
            
            logger.info("Database connected successfully.");
        } catch (Exception e) {
            logger.error("Failed to connect to database!", e);
        }

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
        sendMessage(lg + "Database Type: " + aq + com.pumpkiiings.pklogin.common.settings.Settings.DATABASE_TYPE.asString());
        sendMessage(lg + "Version: " + aq + "2.1");
        sendMessage(lg + "Source: " + aq + com.pumpkiiings.pklogin.common.PluginConstants.GITHUB);
        sendMessage("");
        sendMessage("§e" + "Thanks for use my plugin!");
        sendMessage("");

        // Register channel
        server.getChannelRegistrar().register(PluginMessageListener.IDENTIFIER);

        // Register listeners
        server.getEventManager().register(this, new VelocityListeners(this));
        server.getEventManager().register(this, new PluginMessageListener(this));

        // Register global proxy commands
        new com.pumpkiiings.pklogin.velocity.command.VelocityCommandManager(this).registerCommands();
    }

    private void setupMessages(File dataFolder) {
        String lang = Settings.LANGUAGE_FILE.asString();
        File langFolder = new File(dataFolder, "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        File messagesFile = new File(langFolder, lang);
        if (!messagesFile.exists()) {
            try (java.io.InputStream langResource = PluginResources.openLanguage(lang)) {
                if (langResource != null) {
                    java.nio.file.Files.copy(langResource, messagesFile.toPath());
                }
            } catch (Exception e) {
                logger.error("Failed to copy default language file", e);
            }
        }

        try {
            YamlDocument messagesConfig = com.pumpkiiings.pklogin.common.config.migrations.MessagesMigrations
                    .applyTo(new ConfigurationVersionManager(
                            messagesFile,
                            PluginResources.openLanguage(lang),
                            ConfigurationVersionManager.VERSION_KEY_MESSAGES,
                            com.pumpkiiings.pklogin.common.config.migrations.MessagesMigrations.CURRENT_VERSION,
                            migrationLogger()))
                    .load();

            for (com.pumpkiiings.pklogin.common.settings.Messages message : com.pumpkiiings.pklogin.common.settings.Messages.values()) {
                String path = message.getKey();
                if (path.startsWith("Messages.Title")) {
                    path = path + ".";
                    if (messagesConfig.get(path + "title") != null && messagesConfig.get(path + "subtitle") != null) {
                        String title = messagesConfig.getString(path + "title");
                        String subtitle = messagesConfig.getString(path + "subtitle");
                        int start = messagesConfig.getInt(path + "delays.start", 0);
                        int duration = messagesConfig.getInt(path + "delays.duration", 60);
                        int end = messagesConfig.getInt(path + "delays.end", 6);
                        
                        com.pumpkiiings.pklogin.common.settings.Messages.define(message, new com.pumpkiiings.pklogin.common.model.Title(title, subtitle, start, duration, end));
                    }
                } else if (messagesConfig.get(path) != null) {
                    Object obj = messagesConfig.get(path);
                    if (obj instanceof java.util.List) {
                        com.pumpkiiings.pklogin.common.settings.Messages.define(message, obj);
                    } else {
                        com.pumpkiiings.pklogin.common.settings.Messages.define(message, String.valueOf(obj));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load messages file", e);
        }
    }

    public void reloadConfig() {
        File dataFolder = dataDirectory.toFile();
        try {
            this.yamlConfig = newConfigManager(dataFolder).load();
            
            for (Settings setting : Settings.values()) {
                Object val = yamlConfig.get(setting.getKey());
                if (val != null) {
                    Settings.define(setting, val);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to reload config.yml", e);
        }
        resolveProxySecret();
        setupMessages(dataFolder);
        try {
            this.backendConfig.load();
        } catch (Exception e) {
            logger.error("Failed to load backend.yml", e);
        }
        // The signing key and the server list may both have just changed, so
        // every earlier result is stale.
        this.backendVerification.reset();
        this.backendVerification.reportUnknownAuthServers();
    }

    /** Bridges the migration system's log output onto Velocity's SLF4J logger. */
    private com.pumpkiiings.pklogin.common.config.MigrationLogger migrationLogger() {
        return new com.pumpkiiings.pklogin.common.config.MigrationLogger() {
            @Override
            public void info(String message) {
                logger.info(message);
            }

            @Override
            public void warn(String message) {
                logger.warn(message);
            }

            @Override
            public void error(String message, Throwable cause) {
                logger.error(message, cause);
            }
        };
    }

    /** The config.yml manager, shared by startup and {@code /pklogin reload}. */
    private ConfigurationVersionManager newConfigManager(File dataFolder) {
        return com.pumpkiiings.pklogin.common.config.migrations.ConfigMigrations.applyTo(
                new ConfigurationVersionManager(
                        new File(dataFolder, "config.yml"),
                        PluginResources.openConfig(),
                        ConfigurationVersionManager.VERSION_KEY_CONFIG,
                        com.pumpkiiings.pklogin.common.config.migrations.ConfigMigrations.CURRENT_VERSION,
                        migrationLogger()));
    }

    public static PkLoginVelocity getInstance() {
        return instance;
    }

    public BackendConfig getBackendConfig() {
        return backendConfig;
    }

    public BackendVerification getBackendVerification() {
        return backendVerification;
    }

    public AccountManagement getAccountManagement() {
        return accountManagement;
    }

    public java.util.Set<java.util.UUID> getAuthenticatedPlayers() {
        return authenticatedPlayers;
    }

    /** Returns true when the named player is currently authenticated on this proxy. */
    public boolean isAuthenticated(String playerName) {
        return server.getPlayer(playerName)
                .map(p -> authenticatedPlayers.contains(p.getUniqueId()))
                .orElse(false);
    }

    /** Returns the total number of accounts registered in the database. */
    public int getRegisteredUsers() {
        try {
            try (com.pumpkiiings.pklogin.common.database.Database.Query q =
                    database.query("SELECT COUNT(*) FROM `pklogin`")) {
                if (q.resultSet.next()) return q.resultSet.getInt(1);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    public void sendMessage(String message) {
        server.getConsoleCommandSource().sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize("§b[PkLogin] §r" + message));
    }
}
