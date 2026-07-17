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
import dev.dejvokep.boostedyaml.YamlDocument;

@Plugin(id = "pklogin", name = "PkLogin", version = "2.0.0", authors = {"Pumpkiiiings"})
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
    private YamlDocument yamlConfig;
    private Database database;
    private AccountManagement accountManagement;

    private final java.util.Set<java.util.UUID> authenticatedPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();

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
            ConfigurationVersionManager configManager = new ConfigurationVersionManager(
                new File(dataFolder, "config.yml"), 
                getClass().getClassLoader().getResourceAsStream("com/Pumpkiiiings/PkLogin/config/config.yml")
            );
            
            configManager.registerMigration(new com.pumpkiiings.pklogin.common.config.migrations.config.ConfigMigration_1_to_1_1());
            this.yamlConfig = configManager.loadAndMigrate("1.1");

            for (Settings setting : Settings.values()) {
                Object val = yamlConfig.get(setting.getKey());
                if (val != null) {
                    Settings.define(setting, val);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load config.yml", e);
        }

        // Setup messages
        setupMessages(dataFolder);

        this.backendConfig = new BackendConfig(dataDirectory.resolve("backend.yml"));
        try {
            this.backendConfig.load();
        } catch (Exception e) {
            logger.error("Failed to load backend.yml", e);
        }

        try {
            String dbType = com.pumpkiiings.pklogin.common.settings.Settings.DATABASE_TYPE.asString().toLowerCase();
            if (dbType.equals("mariadb") || dbType.equals("mysql")) {
                database = new com.pumpkiiings.pklogin.common.database.MariaDB(
                    com.pumpkiiings.pklogin.common.settings.Settings.DATABASE_HOST.asString(),
                    com.pumpkiiings.pklogin.common.settings.Settings.DATABASE_PORT.asInt(),
                    com.pumpkiiings.pklogin.common.settings.Settings.DATABASE_NAME.asString(),
                    com.pumpkiiings.pklogin.common.settings.Settings.DATABASE_USERNAME.asString(),
                    com.pumpkiiings.pklogin.common.settings.Settings.DATABASE_PASSWORD.asString()
                );
            } else if (dbType.equals("h2")) {
                File databaseFile = new File(dataFolder, "accounts");
                database = new com.pumpkiiings.pklogin.common.database.H2(databaseFile);
            } else {
                File databaseFile;
                String customPath = com.pumpkiiings.pklogin.common.settings.Settings.DATABASE_SQLITE_FILE_PATH.asString("");
                if (!customPath.isEmpty()) {
                    databaseFile = new File(customPath);
                } else {
                    databaseFile = new File(dataFolder, "accounts.db");
                }
                database = new com.pumpkiiings.pklogin.common.database.SQLite(databaseFile);
            }
            database.openConnection();
            database.update(
                    "CREATE TABLE IF NOT EXISTS `pklogin` (`name` TEXT, `realname` TEXT, `password` TEXT, `address` TEXT, `lastlogin` BIGINT, `regdate` BIGINT, `totp_secret` TEXT, `uuid_type` TEXT DEFAULT 'REAL', `random_uuid` TEXT, `discord_id` TEXT, `email_address` TEXT)");
            database.update("CREATE TABLE IF NOT EXISTS `settings` (`key` TEXT, `value` TEXT)");

            try {
                database.update("ALTER TABLE `pklogin` MODIFY COLUMN `lastlogin` BIGINT");
                database.update("ALTER TABLE `pklogin` MODIFY COLUMN `regdate` BIGINT");
            } catch (Exception ignored) {
            }

            try {
                database.update("ALTER TABLE `pklogin` ADD COLUMN `totp_secret` TEXT");
            } catch (Exception ignored) {
            }
            try {
                database.update("ALTER TABLE `pklogin` ADD COLUMN `uuid_type` TEXT DEFAULT 'REAL'");
            } catch (Exception ignored) {
            }
            try {
                database.update("ALTER TABLE `pklogin` ADD COLUMN `random_uuid` TEXT");
            } catch (Exception ignored) {
            }
            try {
                database.update("ALTER TABLE `pklogin` ADD COLUMN `discord_id` TEXT");
            } catch (Exception ignored) {
            }
            try {
                database.update("ALTER TABLE `pklogin` ADD COLUMN `email_address` TEXT");
            } catch (Exception ignored) {
            }

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
        sendMessage(lg + "Support: " + aq + "https://discord.gg/MVQ5r7X4Qd");
        sendMessage(lg + "Database Type: " + aq + com.pumpkiiings.pklogin.common.settings.Settings.DATABASE_TYPE.asString());
        sendMessage(lg + "Version: " + aq + "2.0.0");
        sendMessage(lg + "Source: " + aq + "github.com/Pumpkiiiings/PkLogin");
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
        
        java.io.InputStream defaultResource = getClass().getClassLoader().getResourceAsStream("com/Pumpkiiiings/PkLogin/config/lang/" + lang);
        if (defaultResource == null) {
            defaultResource = getClass().getClassLoader().getResourceAsStream("com/Pumpkiiiings/PkLogin/config/lang/messages_en.yml");
        }

        try {
            ConfigurationVersionManager messagesManager = new ConfigurationVersionManager(
                messagesFile, 
                defaultResource
            );
            
            YamlDocument messagesConfig = messagesManager.loadAndMigrate("1.0");

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
            ConfigurationVersionManager configManager = new ConfigurationVersionManager(
                new File(dataFolder, "config.yml"), 
                getClass().getClassLoader().getResourceAsStream("com/Pumpkiiiings/PkLogin/config/config.yml")
            );
            configManager.registerMigration(new com.pumpkiiings.pklogin.common.config.migrations.config.ConfigMigration_1_to_1_1());
            this.yamlConfig = configManager.loadAndMigrate("1.1");
            
            for (Settings setting : Settings.values()) {
                Object val = yamlConfig.get(setting.getKey());
                if (val != null) {
                    Settings.define(setting, val);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to reload config.yml", e);
        }
        setupMessages(dataFolder);
        try {
            this.backendConfig.load();
        } catch (Exception e) {
            logger.error("Failed to load backend.yml", e);
        }
    }

    public static PkLoginVelocity getInstance() {
        return instance;
    }

    public BackendConfig getBackendConfig() {
        return backendConfig;
    }

    public AccountManagement getAccountManagement() {
        return accountManagement;
    }

    public java.util.Set<java.util.UUID> getAuthenticatedPlayers() {
        return authenticatedPlayers;
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
