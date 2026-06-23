/*
 * The MIT License (MIT)
 *
 * Copyright Â© 2020 - 2026 - PkLogin Contributors
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

package com.pumpkiiings.pklogin.bukkit.command.executors;

import com.pumpkiiings.pklogin.bukkit.PkLoginBukkit;
import com.pumpkiiings.pklogin.bukkit.command.BukkitAbstractCommand;
import com.pumpkiiings.pklogin.bukkit.converter.AuthMeConverter;
import com.pumpkiiings.pklogin.bukkit.ui.chat.ActionbarAPI;
import com.pumpkiiings.pklogin.bukkit.ui.title.TitleAPI;
import com.pumpkiiings.pklogin.common.http.HttpClient;
import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.common.util.FileUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PkLoginCommand extends BukkitAbstractCommand {

    private final AtomicBoolean
            downloadLock = new AtomicBoolean(),
            confirmNLogin = new AtomicBoolean(),
            confirmPkLogin = new AtomicBoolean(),
            confirmAd = new AtomicBoolean();

    public PkLoginCommand(PkLoginBukkit plugin) {
        super(plugin, "pklogin");
    }

    protected void perform(CommandSender sender, String lb, String[] args) {
        if (args.length != 0) {
            String subcommand = args[0].toLowerCase();
            switch (subcommand) {
                case "authme-import": {
                    if (!sender.hasPermission("pklogin.admin")) {
                        sender.sendMessage("§cYou don't have permission to use this command.");
                        return;
                    }
                    sender.sendMessage("§7Starting AuthMe import...");
                    new AuthMeConverter(plugin).run(sender);
                    return;
                }

                case "reload":
                case "rl":
                case "r": {
                    if (sender instanceof Player && !plugin.getLoginManagement().isAuthenticated(sender.getName())) {
                        return;
                    }

                    plugin.reloadConfig();
                    plugin.setupSettings();
                    sender.sendMessage(Messages.PLUGIN_RELOAD_MESSAGE.asString());
                    return;
                }

                case "update": {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(Messages.PLAYER_COMMAND_USAGE.asString());
                        return;
                    }

                    Player player = (Player) sender;
                    String name = player.getName();
                    if (!plugin.getLoginManagement().isAuthenticated(name)) {
                        return;
                    }

                    if (!plugin.isUpdateAvailable()) {
                        sender.sendMessage("Â§cYou are already using the latest version.");
                        return;
                    }

                    if (downloadLock.getAndSet(true)) {
                        sender.sendMessage("Â§cDownload in progress...");
                    } else if (!update(player)) {
                        downloadLock.set(false);
                    }
                    return;
                }

                case "nlogin_ad": {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(Messages.PLAYER_COMMAND_USAGE.asString());
                        return;
                    }

                    if (!confirmAd.getAndSet(true)) {
                        sender.sendMessage("");
                        sender.sendMessage(" Â§cnLogin is generally a better solution for most users.");
                        sender.sendMessage(" Â§7If you want to keep Â§fPkLogin Â§7anyway,");
                        sender.sendMessage(" Â§7please click on the message again.");
                        sender.sendMessage("");
                        return;
                    }

                    if (plugin.getPluginSettings().set("nlogin_ad", Long.toString(System.currentTimeMillis()))) {
                        sender.sendMessage("Â§eYou will not be notified again of the migration for a long time.");
                    } else {
                        sender.sendMessage("Â§cDatabase error :C, please try again.");
                    }
                    return;
                }

                case "setup": {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(Messages.PLAYER_COMMAND_USAGE.asString());
                        return;
                    }

                    if (!plugin.isNewUser()) {
                        return;
                    }

                    if (!confirmPkLogin.getAndSet(true)) {
                        sender.sendMessage("");
                        sender.sendMessage(" Â§cnLogin is generally a better solution for most users.");
                        sender.sendMessage(" Â§7If you want to install Â§fPkLogin Â§7anyway,");
                        sender.sendMessage(" Â§7please click on the message again.");
                        sender.sendMessage("");
                        return;
                    }

                    for (Player on : plugin.getServer().getOnlinePlayers()) {
                        plugin.getFoliaLib().runAtEntity(on, task -> on.kickPlayer("Â§aPlease rejoin to complete the plugin installation."));
                    }

                    plugin.setNewUser(false);
                    plugin.getPluginSettings().set("setup_date", Long.toString(System.currentTimeMillis()));

                    File newUserfile = new File(plugin.getDataFolder(), "new-user");
                    if (newUserfile.exists() && !newUserfile.delete()) {
                        newUserfile.deleteOnExit();
                    }
                    return;
                }

                case "nlogin": {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(Messages.PLAYER_COMMAND_USAGE.asString());
                        return;
                    }

                    Player player = (Player) sender;
                    String name = player.getName();
                    if (!plugin.isNewUser() && !plugin.getLoginManagement().isAuthenticated(name)) {
                        return;
                    }

                    if (downloadLock.get()) {
                        sender.sendMessage("Â§cDownload in progress...");
                        return;
                    }

                    boolean skip = args.length == 2 && args[1].equalsIgnoreCase("skip");
                    if (!skip && !confirmNLogin.getAndSet(true)) {
                        sender.sendMessage("");
                        sender.sendMessage(" Â§6nLogin Â§7is a Â§6proprietary Â§7authentication plugin,");
                        sender.sendMessage(" Â§7updated and maintained by Â§cpumpkiiings.comÂ§7. This means that you");
                        sender.sendMessage(" Â§7cannot view and modify the source code of the plugin.");
                        sender.sendMessage("");
                        sender.sendMessage(" Â§eIf you still have questions, please contact us:");
                        sender.sendMessage(" Â§bpumpkiiings.com/discord");
                        sender.sendMessage("");
                        sender.sendMessage(" Â§7To proceed with the download, type Â§b/pklogin nlogin Â§7again.");
                        sender.sendMessage("");
                    } else {
                        if (downloadLock.getAndSet(true)) {
                            sender.sendMessage("Â§cDownload already in progress!");
                            return;
                        }

                        Runnable callback = null;
                        if (skip && plugin.isNewUser()) {
                            callback = () -> {
                                for (Player on : plugin.getServer().getOnlinePlayers()) {
                                    plugin.getFoliaLib().runAtEntity(on, task -> {
                                        on.closeInventory();
                                        on.kickPlayer("Â§anLogin was successfully installed. We are restarting the server to apply the changes.");
                                    });
                                }
                                plugin.getServer().shutdown();
                            };
                            TitleAPI.getApi().reset(player);
                        }
                        if (!downloadNLogin(player, callback)) {
                            downloadLock.set(false);
                        }
                    }
                    return;
                }
            }
        }

        sender.sendMessage("");
        sender.sendMessage(" Â§eThis server is running Â§fPkLogin v " + plugin.getDescription().getVersion() + ".");
        sender.sendMessage(" Â§7Powered by Â§bwww.pumpkiiings.comÂ§7.");
        sender.sendMessage("");
        sender.sendMessage(" Â§7GitHub: Â§fhttps://github.com/pumpkiiings/pklogin");
        sender.sendMessage("");
    }

    private boolean update(Player player) {
        File output = new File(plugin.getDataFolder().getParentFile(), "PkLogin-" + plugin.getLatestVersion() + ".jar");
        return downloadActionbar(player, "https://github.com/pumpkiiings/pklogin/releases/download/" + plugin.getLatestVersion() + "/PkLogin.jar", output, true, null);
    }

    private boolean downloadNLogin(Player player, Runnable callback) {
        File output = new File(plugin.getDataFolder().getParentFile(), "nLogin.jar");
        return downloadActionbar(player, "https://repo.pumpkiiings.com/files/latest/nLogin.jar", output, false, callback);
    }

    private boolean downloadActionbar(Player player, String url, File output, boolean update, Runnable callback) {
        player.sendMessage("Â§eDownloading...");
        ActionbarAPI.getApi().send(player, "Â§eConnecting...");

        final int barsCount = 40;
        final HttpClient.AsyncDownloadResult downloadResult;
        try {
            if ((downloadResult = HttpClient.DEFAULT.download(url, output)) == null) {
                ActionbarAPI.getApi().send(player, "Â§cDownload failed!");
                player.sendMessage("Â§cDownload failed, could not delete old file.");
                return false;
            }
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }

        AtomicBoolean downloadFinished = new AtomicBoolean();
        AtomicBoolean downloadSuccessful = new AtomicBoolean();
        plugin.getFoliaLib().runAtEntityTimer(player, task -> {
            if (downloadFinished.get()) {
                if (downloadSuccessful.get()) {
                    ActionbarAPI.getApi().send(player, "Â§aDownload finished! Â§7(Â§a" + repeatString("|", barsCount) + "Â§7)");
                    player.sendMessage("Â§aDownload finished. Please restart your server.");
                    if (callback != null) {
                        callback.run();
                    }
                } else {
                    ActionbarAPI.getApi().send(player, "Â§cDownload failed! Â§7(Â§a" + repeatString("|", barsCount) + "Â§7)");
                    player.sendMessage("Â§cDownload failed, please try again.");
                }
                task.cancel();
                return;
            }
            int bars = (int) (barsCount * (downloadResult.downloaded() / downloadResult.contentLength()));
            String progressBar = "Â§a" + repeatString("|", bars) + "Â§c" + repeatString("|", barsCount - bars);
            ActionbarAPI.getApi().send(player, "Â§eDownloading... Â§7(" + progressBar + "Â§7)");
        }, 0, 200, TimeUnit.MILLISECONDS);

        try {
            downloadSuccessful.set(downloadResult.startDownload());
            if (downloadSuccessful.get()) {
                File pluginFile = FileUtils.getSelfJarFile();
                pluginFile.deleteOnExit();
            }
        } catch (IOException e) {
            downloadLock.set(false);
            e.printStackTrace();
            String msg = update ?
                    "Â§cFailed to download new version. Update manually at: https://github.com/pumpkiiings/pklogin/releases" :
                    "Â§cFailed to download nLogin :c. Download manually at: pumpkiiings.com";
            plugin.sendMessage(msg);
            player.sendMessage(msg);
        } finally {
            downloadFinished.set(true);
        }
        return downloadSuccessful.get();
    }

    private String repeatString(String str, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(str);
        }
        return builder.toString();
    }
}

