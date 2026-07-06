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

package com.pumpkiiings.pklogin.paper.command.executors;

import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import com.pumpkiiings.pklogin.paper.command.BukkitAbstractCommand;
import com.pumpkiiings.pklogin.paper.converter.AuthMeConverter;


import com.pumpkiiings.pklogin.common.http.HttpClient;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.security.hashing.BCryptStrategy;
import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.common.util.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PkLoginCommand extends BukkitAbstractCommand {

    private final AtomicBoolean
            downloadLock = new AtomicBoolean();

    public PkLoginCommand(PkLoginPaper plugin) {
        super(plugin, "pklogin");
    }

    protected void perform(CommandSender sender, String lb, String[] args) {
        if (args.length != 0) {
            String subcommand = args[0].toLowerCase();
            switch (subcommand) {
                case "authme-import": {
                    if (!sender.hasPermission("pklogin.admin.authme-import")) {
                        sender.sendMessage(Messages.INSUFFICIENT_PERMISSIONS.asString());
                        return;
                    }
                    sender.sendMessage(Messages.ADMIN_AUTHME_IMPORT_START.asString());
                    new AuthMeConverter(plugin).run(sender);
                    return;
                }

                case "forcelogin": {
                    if (!sender.hasPermission("pklogin.admin.forcelogin")) {
                        sender.sendMessage(Messages.INSUFFICIENT_PERMISSIONS.asString());
                        return;
                    }
                    if (args.length < 2) {
                        sender.sendMessage("§eUsage: /pklogin forcelogin <player>");
                        return;
                    }
                    String targetName = args[1];
                    Player target = Bukkit.getPlayer(targetName);
                    if (target != null && target.isOnline()) {
                        plugin.getLoginManagement().setAuthenticated(target.getName());
                        com.pumpkiiings.pklogin.paper.util.AdventureAPI.clearTitle(target);
                        target.getScheduler().run(plugin, task -> target.sendMessage(Messages.SUCCESSFUL_LOGIN.asString()), null);
                        sender.sendMessage(Messages.ADMIN_FORCELOGIN_SUCCESS.asString().replace("{0}", target.getName()));
                    } else {
                        sender.sendMessage("§cPlayer not found or not online.");
                    }
                    return;
                }

                case "unregister": {
                    if (!sender.hasPermission("pklogin.admin.unregister")) {
                        sender.sendMessage(Messages.INSUFFICIENT_PERMISSIONS.asString());
                        return;
                    }
                    if (args.length < 2) {
                        sender.sendMessage("§eUsage: /pklogin unregister <player>");
                        return;
                    }
                    String targetName = args[1];
                    if (plugin.getAccountManagement().removePassword(targetName)) {
                        plugin.getLoginManagement().cleanup(targetName);
                        sender.sendMessage(Messages.ADMIN_UNREGISTER_SUCCESS.asString().replace("{0}", targetName));
                    } else {
                        sender.sendMessage(Messages.ADMIN_ACCOUNT_NOT_FOUND.asString());
                    }
                    return;
                }

                case "delete": {
                    if (!sender.hasPermission("pklogin.admin.delete")) {
                        sender.sendMessage(Messages.INSUFFICIENT_PERMISSIONS.asString());
                        return;
                    }
                    if (args.length < 2) {
                        sender.sendMessage("§eUsage: /pklogin delete <player>");
                        return;
                    }
                    String targetName = args[1];
                    if (plugin.getAccountManagement().delete(targetName)) {
                        plugin.getLoginManagement().cleanup(targetName);
                        sender.sendMessage(Messages.ADMIN_DELETE_SUCCESS.asString().replace("{0}", targetName));
                    } else {
                        sender.sendMessage(Messages.ADMIN_ACCOUNT_NOT_FOUND.asString());
                    }
                    return;
                }

                case "verify": {
                    if (!sender.hasPermission("pklogin.admin.verify")) {
                        sender.sendMessage(Messages.INSUFFICIENT_PERMISSIONS.asString());
                        return;
                    }
                    if (args.length < 2) {
                        sender.sendMessage("§eUsage: /pklogin verify <player>");
                        return;
                    }
                    String targetName = args[1];
                    Optional<Account> accOpt = plugin.getAccountManagement().search(targetName);
                    if (accOpt.isPresent()) {
                        Account acc = accOpt.get();
                        java.util.List<String> verifyFormat = Messages.ADMIN_VERIFY_FORMAT.asList();
                        for (String line : verifyFormat) {
                            sender.sendMessage(line.replace("{0}", targetName)
                                    .replace("{1}", acc.getAddress())
                                    .replace("{2}", new java.util.Date(acc.getRegDate()).toString())
                                    .replace("{3}", new java.util.Date(acc.getLastLogin()).toString())
                                    .replace("{4}", (!"OFFLINE".equals(acc.getUuidType()) ? "Yes" : "No")));
                        }
                    } else {
                        sender.sendMessage(Messages.ADMIN_ACCOUNT_NOT_FOUND.asString());
                    }
                    return;
                }

                case "setspawn": {
                    if (!sender.hasPermission("pklogin.admin.setspawn")) {
                        sender.sendMessage(Messages.INSUFFICIENT_PERMISSIONS.asString());
                        return;
                    }
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(Messages.PLAYER_COMMAND_USAGE.asString());
                        return;
                    }
                    Player player = (Player) sender;
                    File file = new File(plugin.getDataFolder(), "spawn.yml");
                    org.bukkit.configuration.file.YamlConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
                    config.set("spawn", player.getLocation());
                    try {
                        config.save(file);
                        sender.sendMessage(Messages.ADMIN_SETSPAWN_SUCCESS.asString());
                    } catch (IOException e) {
                        e.printStackTrace();
                        sender.sendMessage("§cFailed to save spawn location.");
                    }
                    return;
                }

                case "changepass": {
                    if (!sender.hasPermission("pklogin.admin.changepass")) {
                        sender.sendMessage(Messages.INSUFFICIENT_PERMISSIONS.asString());
                        return;
                    }
                    if (args.length < 3) {
                        sender.sendMessage("§eUsage: /pklogin changepass <player> <newpass>");
                        return;
                    }
                    String targetName = args[1];
                    String newPass = args[2];
                    Optional<Account> targetAccount = plugin.getAccountManagement().search(targetName);
                    if (targetAccount.isPresent()) {
                        String hash = new BCryptStrategy().hash(newPass);
                        plugin.getAccountManagement().update(targetName, hash, targetAccount.get().getAddress());
                        sender.sendMessage(Messages.ADMIN_CHANGEPASS_SUCCESS.asString().replace("{0}", targetName));
                        Player target = Bukkit.getPlayer(targetName);
                        if (target != null && target.isOnline()) {
                            target.getScheduler().run(plugin, task -> target.kick(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(Messages.ADMIN_CHANGEPASS_KICK.asString())), null);
                        }
                    } else {
                        sender.sendMessage("§cAccount not found.");
                    }
                    return;
                }

                case "dupeip": {
                    if (!sender.hasPermission("pklogin.admin.dupeip")) {
                        sender.sendMessage(Messages.INSUFFICIENT_PERMISSIONS.asString());
                        return;
                    }
                    if (args.length < 2) {
                        sender.sendMessage("§eUsage: /pklogin dupeip <ip/player>");
                        return;
                    }
                    String target = args[1];
                    String ip = target;
                    
                    if (!target.contains(".")) {
                        Optional<Account> acc = plugin.getAccountManagement().search(target);
                        if (acc.isPresent()) {
                            ip = acc.get().getAddress();
                        }
                    }
                    
                    Map<String, Long> accounts = plugin.getAccountManagement().getAccountsByIp(ip);
                    if (accounts.isEmpty()) {
                        sender.sendMessage(Messages.ADMIN_DUPEIP_NONE.asString());
                    } else {
                        sender.sendMessage(Messages.ADMIN_DUPEIP_HEADER.asString().replace("{0}", target));
                        accounts.forEach((name, lastLogin) -> {
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
                            String dateStr = sdf.format(new java.util.Date(lastLogin));
                            sender.sendMessage(Messages.ADMIN_DUPEIP_FORMAT.asString().replace("{0}", name).replace("{1}", dateStr));
                        });
                    }
                    return;
                }


                case "reload":
                case "rl":
                case "r": {
                    if (!sender.hasPermission("pklogin.admin.reload")) {
                        sender.sendMessage(Messages.INSUFFICIENT_PERMISSIONS.asString());
                        return;
                    }
                    if (sender instanceof Player && !plugin.getLoginManagement().isAuthenticated(sender.getName())) {
                        return;
                    }

                    plugin.reloadConfig();
                    plugin.setupSettings();
                    sender.sendMessage(Messages.PLUGIN_RELOAD_MESSAGE.asString());
                    return;
                }

                case "update": {
                    if (!sender.hasPermission("pklogin.admin.update")) {
                        sender.sendMessage(Messages.INSUFFICIENT_PERMISSIONS.asString());
                        return;
                    }
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
                        sender.sendMessage("§cYou are already using the latest version.");
                        return;
                    }

                    if (downloadLock.getAndSet(true)) {
                        sender.sendMessage("§cDownload in progress...");
                    } else if (!update(player)) {
                        downloadLock.set(false);
                    }
                    return;
                }


            }
        }

        sender.sendMessage("");
        sender.sendMessage(" §eThis server is running §fPkLogin v " + plugin.getDescription().getVersion() + ".");
        sender.sendMessage(" §7Powered by §bwww.pumpkiiings.com§7.");
        sender.sendMessage("");
        sender.sendMessage(" §7GitHub: §fhttps://github.com/Pumpkiiiings/PkLogin");
        sender.sendMessage("");
    }

    private boolean update(Player player) {
        File output = new File(plugin.getDataFolder().getParentFile(), "PkLogin-" + plugin.getLatestVersion() + ".jar");
        return downloadActionbar(player, "https://github.com/Pumpkiiiings/PkLogin/releases/download/" + plugin.getLatestVersion() + "/PkLogin.jar", output, true, null);
    }



    private boolean downloadActionbar(Player player, String url, File output, boolean update, Runnable callback) {
        player.sendMessage("§eDownloading...");
        com.pumpkiiings.pklogin.paper.util.AdventureAPI.sendActionBar(player, "§eConnecting...");

        final int barsCount = 40;
        final HttpClient.AsyncDownloadResult downloadResult;
        try {
            if ((downloadResult = HttpClient.DEFAULT.download(url, output)) == null) {
                com.pumpkiiings.pklogin.paper.util.AdventureAPI.sendActionBar(player, "§cDownload failed!");
                player.sendMessage("§cDownload failed, could not delete old file.");
                return false;
            }
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }

        AtomicBoolean downloadFinished = new AtomicBoolean();
        AtomicBoolean downloadSuccessful = new AtomicBoolean();
        player.getScheduler().runAtFixedRate(plugin, task -> {
            if (downloadFinished.get()) {
                if (downloadSuccessful.get()) {
                    com.pumpkiiings.pklogin.paper.util.AdventureAPI.sendActionBar(player, "§aDownload finished! §7(§a" + repeatString("|", barsCount) + "§7)");
                    player.sendMessage("§aDownload finished. Please restart your server.");
                    if (callback != null) {
                        callback.run();
                    }
                } else {
                    com.pumpkiiings.pklogin.paper.util.AdventureAPI.sendActionBar(player, "§cDownload failed! §7(§a" + repeatString("|", barsCount) + "§7)");
                    player.sendMessage("§cDownload failed, please try again.");
                }
                task.cancel();
                return;
            }
            int bars = (int) (barsCount * (downloadResult.downloaded() / downloadResult.contentLength()));
            String progressBar = "§a" + repeatString("|", bars) + "§c" + repeatString("|", barsCount - bars);
            com.pumpkiiings.pklogin.paper.util.AdventureAPI.sendActionBar(player, "§eDownloading... §7(" + progressBar + "§7)");
        }, null, 0L, 10L);

        try {
            downloadSuccessful.set(downloadResult.startDownload());
            if (downloadSuccessful.get()) {
                File pluginFile = FileUtils.getSelfJarFile();
                pluginFile.deleteOnExit();
            }
        } catch (IOException e) {
            downloadLock.set(false);
            e.printStackTrace();
            String msg = "§cFailed to download new version. Update manually at: https://github.com/Pumpkiiiings/PkLogin/releases";
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

