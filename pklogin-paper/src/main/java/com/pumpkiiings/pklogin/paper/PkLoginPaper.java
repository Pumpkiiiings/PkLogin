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
import com.pumpkiiings.pklogin.paper.command.CommandManagement;
import com.pumpkiiings.pklogin.paper.listener.PlayerAuthenticateListener;
import com.pumpkiiings.pklogin.paper.listener.PlayerGeneralListeners;
import com.pumpkiiings.pklogin.paper.listener.PlayerJoinListeners;
import com.pumpkiiings.pklogin.paper.listener.PlayerKickListeners;
import com.pumpkiiings.pklogin.paper.task.LoginQueue;
import com.pumpkiiings.pklogin.common.PkLogin;
import com.pumpkiiings.pklogin.common.api.PkLoginAPI;
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
    private CommandManagement commandManagement;

    private Database database;
    private PluginSettings pluginSettings;

    private final java.util.concurrent.ConcurrentHashMap<String, com.pumpkiiings.pklogin.paper.protocollib.AutoLoginSession> verifiedSessions = new java.util.concurrent.ConcurrentHashMap<>();

    private String latestVersion;
    private boolean updateAvailable;
    private int registeredUsers;

    public void onEnable() {
        PluginManager pm = getServer().getPluginManager();

        // detect nLogin
        if (pm.getPlugin("nLogin") != null) {
            sendMessage("nLogin was detected, turning off plugin...");
            pm.disablePlugin(this);
            return;
        }



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
        
        String c = "§6";
        String lg = "§7";
        String dg = "§8";
        sendMessage(c + "▄▄▄▄▄▄▄          ▄▄▄                      ");
        sendMessage(c + "███▀▀███▄ ▄▄     ███                  ▀▀ ");
        sendMessage(c + "███▄▄███▀ ██ ▄█▀ ███      ▄███▄ ▄████ ██  ████▄ ");
        sendMessage(c + "███▀▀▀▀   ████   ███      ██ ██ ██ ██ ██  ██ ██ ");
        sendMessage(c + "███       ██ ▀█▄ ████████ ▀███▀ ▀████ ██▄ ██ ██ ");
        sendMessage(c + "                                   ██  ");
        sendMessage(c + "                                 ▀▀▀  ");
        sendMessage(dg + "A fork of OpenLogin but better");
        sendMessage(lg + "Support: " + dg + "https://discord.gg/MVQ5r7X4Qd");
        sendMessage(lg + "Database Type: " + dg + com.pumpkiiings.pklogin.common.settings.Settings.DATABASE_TYPE.asString());
        sendMessage(lg + "Version: " + dg + getDescription().getVersion());
        sendMessage(lg + "Source: " + dg + "github.com/pumpkiiings/pklogin");
        sendMessage("");
        sendMessage(dg + "Thanks for use my plugin!");
        sendMessage("");

        // setup Folia lib

        // setup account management
        accountManagement = new AccountManagement(database);

        // setup login management
        loginManagement = new LoginManagement(accountManagement);

        // setup commands
        commandManagement = new CommandManagement(this);
        commandManagement.register();

        // setup logger filter
        LoggerFilterManager.setup(getLogger());

        // setup listeners
        setupListeners();

        // setup PacketEvents or ProtocolLib auto-login
        if (getServer().getPluginManager().getPlugin("packetevents") != null) {
            sendMessage("PacketEvents detected. Using PacketEvents for Premium Auto-Login.");
            com.pumpkiiings.pklogin.paper.packetevents.PacketEventsHook.init(this);
        } else if (getServer().getPluginManager().getPlugin("ProtocolLib") != null) {
            sendMessage("ProtocolLib detected. Using ProtocolLib for Premium Auto-Login.");
            com.pumpkiiings.pklogin.paper.protocollib.ProtocolLibHook.init(this);
        } else {
            sendMessage("Neither PacketEvents nor ProtocolLib detected. Premium Auto-Login is disabled.");
        }

        // start login queue task
        LoginQueue.startTask(this);

        // setup api
        PkLogin.setApi(new OLBukkitAPI(this));

        getServer().getMessenger().registerOutgoingPluginChannel(this, "pklogin:main");
        getServer().getMessenger().registerIncomingPluginChannel(this, "pklogin:main",
                new com.pumpkiiings.pklogin.paper.listener.ProxyMessageListener(this));

        // updates
        this.getServer().getAsyncScheduler().runNow(this, task -> this.detectUpdates());

        com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorManager.getInstance().init();

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

    public void sendMessage(String message) {
        getServer().getConsoleSender().sendMessage("[" + getName() + "] " + message);
    }

    public void sendMessage(String message, String color) {
        getServer().getConsoleSender().sendMessage(color + "[" + getName() + "] " + message);
    }

    private boolean setupDatabase() {
        File databaseFile = new File(getDataFolder(), "accounts.db");
        database = new SQLite(databaseFile);
        try {
            database.openConnection();
            database.update(
                    "CREATE TABLE IF NOT EXISTS `pklogin` (`name` TEXT, `realname` TEXT, `password` TEXT, `address` TEXT, `lastlogin` INTEGER, `regdate` INTEGER, `totp_secret` TEXT, `uuid_type` TEXT DEFAULT 'REAL', `random_uuid` TEXT, `discord_id` TEXT, `email_address` TEXT)");
            database.update("CREATE TABLE IF NOT EXISTS `settings` (`key` TEXT, `value` TEXT)");

            // Check if columns exist, if not, add them (for existing SQLite databases)
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
            try (Database.Query query = database.query("SELECT COUNT(*) FROM `pklogin`")) {
                ResultSet rs = query.resultSet;
                if (rs.next()) {
                    registeredUsers = rs.getInt("COUNT(*)");
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
            String result = HttpClient.DEFAULT.get("https://api.github.com/repos/pumpkiiings/PkLogin/releases/latest");

            // avoid use Google Gson to avoid problems with older versions.
            if (result.contains("\"tag_name\":\"")) {
                tagName = result.split("\"tag_name\":\"")[1];
                if (tagName.contains("\",")) {
                    tagName = latestVersion = tagName.split("\",")[0];
                }
            }
        } catch (IOException e) {
            sendMessage("§cFailed to find new updates.");
            sendMessage("§cDownload the latest version at: https://github.com/pumpkiiings/pklogin/releases");
        }
        if (tagName == null) {
            sendMessage("§cFailed to find new updates: invalid response.");
            sendMessage("§cDownload the latest version at: https://github.com/pumpkiiings/pklogin/releases");
        } else {
            String currentVersion = "v" + getDescription().getVersion();
            updateAvailable = !currentVersion.equals(tagName);
            if (updateAvailable) {
                sendMessage("A new version of PkLogin is available (" + currentVersion + " -> " + latestVersion + ").",
                        "§e");
            }
        }
    }

    public boolean setupSettings() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists() && !FileUtils.copyFromJar("com/pumpkiiings/pklogin/config/config.yml", configFile)) {
            sendMessage("§cFailed to create 'config.yml' file.");
            return false;
        }

        File twoFaFolder = new File(getDataFolder(), "2fa");
        if (!twoFaFolder.exists()) {
            twoFaFolder.mkdirs();
        }

        File discordFile = new File(twoFaFolder, "discord.yml");
        if (!discordFile.exists()
                && !FileUtils.copyFromJar("com/pumpkiiings/pklogin/config/2fa/discord.yml", discordFile)) {
            sendMessage("§cFailed to create 'discord.yml' file.");
        }

        File emailFile = new File(twoFaFolder, "email.yml");
        if (!emailFile.exists() && !FileUtils.copyFromJar("com/pumpkiiings/pklogin/config/2fa/email.yml", emailFile)) {
            sendMessage("§cFailed to create 'email.yml' file.");
        }

        Settings.clear();
        for (Settings setting : Settings.values()) {
            Settings.define(setting, getConfig().get(setting.getKey()));
        }

        String lang = Settings.LANGUAGE_FILE.asString();
        File messagesFile = new File(getDataFolder() + "/lang", lang);
        if (!messagesFile.exists()
                && !FileUtils.copyFromJar("com/pumpkiiings/pklogin/config/lang/" + lang, messagesFile)
                && !FileUtils.copyFromJar("com/pumpkiiings/pklogin/config/lang/messages_en.yml", messagesFile)) {
            sendMessage("§cFailed to create '" + lang + "' language file.");
            return false;
        }

        YamlConfiguration messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
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
                    Messages.define(message, new Title(title, subtitle, start, duration, end));
                }
            } else if (messagesConfig.isSet(path)) {
                Object obj = messagesConfig.get(path);
                Messages.define(message, obj);
            }
        }
        return true;
    }

    public static PkLoginAPI getApi() {
        return PkLogin.getApi();
    }
}
