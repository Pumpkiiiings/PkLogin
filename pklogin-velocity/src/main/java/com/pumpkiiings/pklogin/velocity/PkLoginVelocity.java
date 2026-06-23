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
import com.pumpkiiings.pklogin.velocity.config.YamlConfig;

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
    private YamlConfig yamlConfig;
    private Database database;
    private AccountManagement accountManagement;

    private final java.util.Set<java.util.UUID> authenticatedPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        instance = this;

        File dataFolder = dataDirectory.toFile();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        this.yamlConfig = new YamlConfig(new File(dataFolder, "config.yml"));
        // Need to save default config.yml if it doesn't exist
        saveDefaultConfig();
        
        for (Settings setting : Settings.values()) {
            Object val = yamlConfig.get(setting.getKey());
            if (val != null) {
                Settings.define(setting, val);
            }
        }

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
                File databaseFile = new File(dataFolder, "accounts.db");
                database = new com.pumpkiiings.pklogin.common.database.SQLite(databaseFile);
            }
            database.openConnection();
            this.accountManagement = new AccountManagement(this.database);
            logger.info("Database connected successfully.");
        } catch (Exception e) {
            logger.error("Failed to connect to database!", e);
        }

        String c = "§b";
        String lg = "§7";
        String dg = "§8";
        String aq = "§b";
        logger.info(c + "   ___                __  __             _ ");
        logger.info(c + "  /___\\_ __   ___  /\\ \\ \\/ /  ___   __ _(_)_ __");
        logger.info(c + " //  // '_ \\ / _ \\/  \\/ / /  / _ \\ / _` | | '_ \\");
        logger.info(c + "/ \\_//| |_) |  __/ /\\  / /__| (_) | (_| | | | | |");
        logger.info(c + "\\___/ | .__/ \\___\\_\\ \\/\\____/\\___/ \\__, |_|_| |_|");
        logger.info(c + "      |_|                          |___/         ");
        logger.info(dg + "A fork of OpenLogin but better");
        logger.info(lg + "Support: " + aq + "https://discord.gg/MVQ5r7X4Qd");
        logger.info(lg + "Database Type: " + aq + com.pumpkiiings.pklogin.common.settings.Settings.DATABASE_TYPE.asString());
        logger.info(lg + "Version: " + aq + "2.0.0");
        logger.info(lg + "Source: " + aq + "github.com/pumpkiiings/pklogin");
        logger.info("");
        logger.info("§e" + "Thanks for use my plugin!");
        logger.info("");

        // Register channel
        server.getChannelRegistrar().register(PluginMessageListener.IDENTIFIER);

        // Register listeners
        server.getEventManager().register(this, new VelocityListeners(this));
        server.getEventManager().register(this, new PluginMessageListener(this));
    }

    private void saveDefaultConfig() {
        File file = new File(dataDirectory.toFile(), "config.yml");
        if (!file.exists()) {
            try (java.io.InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                if (in != null) {
                    java.nio.file.Files.copy(in, file.toPath());
                } else {
                    file.createNewFile();
                }
            } catch (Exception e) {
                logger.error("Failed to save default config.yml", e);
            }
        }
        
        // Re-load to get the values just saved
        this.yamlConfig = new YamlConfig(file);
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
}
