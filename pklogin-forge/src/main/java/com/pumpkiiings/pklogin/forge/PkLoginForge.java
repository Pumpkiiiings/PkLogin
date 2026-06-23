package com.pumpkiiings.pklogin.forge;

import com.pumpkiiings.pklogin.common.database.Database;
import com.pumpkiiings.pklogin.common.database.PluginSettings;
import com.pumpkiiings.pklogin.common.database.SQLite;
import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.manager.LoginManagement;
import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.common.settings.Settings;
import com.pumpkiiings.pklogin.common.util.FileUtils;
import lombok.Getter;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

@Mod("pklogin")
@Getter
public class PkLoginForge {

    private static final Logger LOGGER = LogManager.getLogger();
    private static PkLoginForge instance;

    private LoginManagement loginManagement;
    private AccountManagement accountManagement;
    private Database database;
    private PluginSettings pluginSettings;
    private com.pumpkiiings.pklogin.forge.command.CommandManagement commandManagement;
    private com.pumpkiiings.pklogin.forge.manager.ForgeSpawnManager spawnManager;
    
    private int registeredUsers;

    private final ConcurrentHashMap<String, Boolean> verifiedSessions = new ConcurrentHashMap<>();

    public PkLoginForge() {
        instance = this;
        MinecraftForge.EVENT_BUS.register(this);
        commandManagement = new com.pumpkiiings.pklogin.forge.command.CommandManagement();
        MinecraftForge.EVENT_BUS.register(commandManagement);
        new com.pumpkiiings.pklogin.forge.lunar.ForgeLunarManager();
    }

    public static PkLoginForge getInstance() {
        return instance;
    }

    @SubscribeEvent
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        LOGGER.info("[PkLogin] Starting PkLogin Forge...");
        
        File dataFolder = new File("config/pklogin");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File libsFolder = new File("mods");
        if (com.pumpkiiings.pklogin.common.util.LibraryDownloader.checkAndDownloadForge(libsFolder)) {
            LOGGER.fatal("=========================================================");
            LOGGER.fatal("[PkLogin] Librerías de Discord y Email descargadas exitosamente.");
            LOGGER.fatal("[PkLogin] POR FAVOR REINICIA EL SERVIDOR PARA APLICAR LOS CAMBIOS.");
            LOGGER.fatal("=========================================================");
            net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().toFile().deleteOnExit();
            System.exit(0);
            return;
        }

        // Setup config
        if (!setupSettings()) {
            LOGGER.error("Failed to setup settings.");
            return;
        }

        // Setup database
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

        try {
            database.openConnection();
            // Adding totp_secret, uuid_type, random_uuid, discord_id, email_address
            database.update("CREATE TABLE IF NOT EXISTS `pklogin` (`name` TEXT, `realname` TEXT, `password` TEXT, `address` TEXT, `lastlogin` INTEGER, `regdate` INTEGER, `totp_secret` TEXT, `uuid_type` TEXT DEFAULT 'REAL', `random_uuid` TEXT, `discord_id` TEXT, `email_address` TEXT)");
            database.update("CREATE TABLE IF NOT EXISTS `settings` (`key` TEXT, `value` TEXT)");
            
            // Check if columns exist, if not, add them (for existing SQLite databases)
            try {
                database.update("ALTER TABLE `pklogin` ADD COLUMN `totp_secret` TEXT");
            } catch (Exception ignored) { }
            try {
                database.update("ALTER TABLE `pklogin` ADD COLUMN `uuid_type` TEXT DEFAULT 'REAL'");
            } catch (Exception ignored) { }
            try {
                database.update("ALTER TABLE `pklogin` ADD COLUMN `random_uuid` TEXT");
            } catch (Exception ignored) { }
            try {
                database.update("ALTER TABLE `pklogin` ADD COLUMN `discord_id` TEXT");
            } catch (Exception ignored) { }
            try {
                database.update("ALTER TABLE `pklogin` ADD COLUMN `email_address` TEXT");
            } catch (Exception ignored) { }
            
            try (Database.Query query = database.query("SELECT COUNT(*) FROM `pklogin`")) {
                ResultSet rs = query.resultSet;
                if (rs.next()) {
                    registeredUsers = rs.getInt("COUNT(*)");
                }
            } catch (Exception e) {
                LOGGER.error("Failed to update the register count.");
            }
            pluginSettings = new PluginSettings(database);
        } catch (SQLException e) {
            LOGGER.error("Failed to start database.", e);
        }
        
        // Setup Account and Login Management
        accountManagement = new AccountManagement(database);
        loginManagement = new LoginManagement(accountManagement);
        
        com.pumpkiiings.pklogin.common.PkLogin.setApi(new com.pumpkiiings.pklogin.forge.api.OLForgeAPI(this));
        
        spawnManager = new com.pumpkiiings.pklogin.forge.manager.ForgeSpawnManager(dataFolder);
        spawnManager.loadSpawns();
        
        // Setup logger filter
        com.pumpkiiings.pklogin.common.security.filter.LoggerFilterManager.setup(java.util.logging.Logger.getLogger("pklogin"));
        com.pumpkiiings.pklogin.common.security.filter.LoggerFilterManager.addPkLoginCommand("/login");
        com.pumpkiiings.pklogin.common.security.filter.LoggerFilterManager.addPkLoginCommand("/register");
        com.pumpkiiings.pklogin.common.security.filter.LoggerFilterManager.addPkLoginCommand("/changepassword");
        com.pumpkiiings.pklogin.common.security.filter.LoggerFilterManager.addPkLoginCommand("/unregister");
        
        // Register Listeners
        MinecraftForge.EVENT_BUS.register(new com.pumpkiiings.pklogin.forge.listener.ForgeGeneralListeners());
        MinecraftForge.EVENT_BUS.register(new com.pumpkiiings.pklogin.forge.task.ForgeLoginQueue());
        
        com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorManager.getInstance().init();
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        if (event.getServer().usesAuthentication()) {
            LOGGER.warn("=========================================================");
            LOGGER.warn("[PkLogin] WARNING: online-mode is set to true in server.properties!");
            LOGGER.warn("[PkLogin] If this server only allows premium players (no Proxy), you don't need a login mod!");
            LOGGER.warn("[PkLogin] If you are using a proxy (like Velocity), please set online-mode to false.");
            LOGGER.warn("=========================================================");
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("[PkLogin] Stopping PkLogin Forge...");
        com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorManager.getInstance().shutdown();
    }

    public boolean setupSettings() {
        File dataFolder = new File("config/pklogin");
        File configFile = new File(dataFolder, "config.yml");
        if (!configFile.exists() && !FileUtils.copyFromJar("com/pumpkiiings/pklogin/config/config.yml", configFile)) {
            LOGGER.error("Failed to create 'config.yml' file.");
            return false;
        }

        File twoFaFolder = new File(dataFolder, "2fa");
        if (!twoFaFolder.exists()) {
            twoFaFolder.mkdirs();
        }

        File discordFile = new File(twoFaFolder, "discord.yml");
        if (!discordFile.exists() && !FileUtils.copyFromJar("com/pumpkiiings/pklogin/config/2fa/discord.yml", discordFile)) {
            LOGGER.error("Failed to create 'discord.yml' file.");
        }

        File emailFile = new File(twoFaFolder, "email.yml");
        if (!emailFile.exists() && !FileUtils.copyFromJar("com/pumpkiiings/pklogin/config/2fa/email.yml", emailFile)) {
            LOGGER.error("Failed to create 'email.yml' file.");
        }

        Settings.clear();
        com.pumpkiiings.pklogin.forge.config.YamlConfig config = new com.pumpkiiings.pklogin.forge.config.YamlConfig(configFile);
        for (Settings setting : Settings.values()) {
            if (setting.getDef() instanceof java.util.List) {
                Settings.define(setting, config.getStringList(setting.getKey()));
            } else {
                Settings.define(setting, config.get(setting.getKey()));
            }
        }

        String lang = Settings.LANGUAGE_FILE.asString();
        File messagesFile = new File(dataFolder + "/lang", lang);
        if (!messagesFile.exists() && !FileUtils.copyFromJar("com/pumpkiiings/pklogin/config/lang/" + lang, messagesFile) && !FileUtils.copyFromJar("com/pumpkiiings/pklogin/config/lang/messages_en.yml", messagesFile)) {
            LOGGER.error("Failed to create '" + lang + "' language file.");
            return false;
        }

        com.pumpkiiings.pklogin.forge.config.YamlConfig messagesConfig = new com.pumpkiiings.pklogin.forge.config.YamlConfig(messagesFile);
        for (Messages message : Messages.values()) {
            String path = message.getKey();
            if (path.startsWith("Messages.Title")) {
                String title = "", subtitle = "";
                int start = 0, duration = 0, end = 0;

                path = path + ".";
                if (messagesConfig.isSet(path + "title") && messagesConfig.isSet(path + "subtitle")) {
                    title = messagesConfig.getString(path + "title");
                    subtitle = messagesConfig.getString(path + "subtitle");
                    start = messagesConfig.getInt(path + "delays.start", 0);
                    duration = messagesConfig.getInt(path + "delays.duration", 60);
                    end = messagesConfig.getInt(path + "delays.end", 6);
                    Messages.define(message, new com.pumpkiiings.pklogin.common.model.Title(title, subtitle, start, duration, end));
                }
            } else if (messagesConfig.isSet(path)) {
                Object obj = messagesConfig.get(path);
                Messages.define(message, obj);
            }
        }
        return true;
    }
}
