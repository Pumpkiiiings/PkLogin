package com.pumpkiiings.pklogin.paper.command.executors;

import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import com.pumpkiiings.pklogin.paper.converter.AuthMeConverter;
import com.pumpkiiings.pklogin.common.http.HttpClient;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.security.hashing.BCryptStrategy;
import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.common.util.FileUtils;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class PkLoginCommandNode {

    private static final AtomicBoolean downloadLock = new AtomicBoolean();

    public static LiteralCommandNode<CommandSourceStack> build(PkLoginPaper plugin) {
        return Commands.literal("pklogin")
            .executes(context -> {
                CommandSender sender = context.getSource().getSender();
                plugin.runAsync(() -> sendInfo(plugin, sender));
                return 1;
            })
            .then(Commands.literal("authme-import")
                .requires(source -> source.getSender().hasPermission("pklogin.admin.authme-import"))
                .executes(context -> {
                    CommandSender sender = context.getSource().getSender();
                    plugin.runAsync(() -> {
                        sender.sendMessage(Messages.ADMIN_AUTHME_IMPORT_START.asString());
                        new AuthMeConverter(plugin).run(sender);
                    });
                    return 1;
                })
            )
            .then(Commands.literal("forcelogin")
                .requires(source -> source.getSender().hasPermission("pklogin.admin.forcelogin"))
                .then(Commands.argument("player", StringArgumentType.word())
                    .executes(context -> {
                        CommandSender sender = context.getSource().getSender();
                        String targetName = context.getArgument("player", String.class);
                        plugin.runAsync(() -> {
                            Player target = Bukkit.getPlayer(targetName);
                            if (target != null && target.isOnline()) {
                                plugin.getLoginManagement().setAuthenticated(target.getName());
                                com.pumpkiiings.pklogin.paper.util.AdventureAPI.clearTitle(target);
                                target.getScheduler().run(plugin, task -> target.sendMessage(Messages.SUCCESSFUL_LOGIN.asString()), null);
                                sender.sendMessage(Messages.ADMIN_FORCELOGIN_SUCCESS.asString().replace("{0}", target.getName()));
                            } else {
                                sender.sendMessage("§cPlayer not found or not online.");
                            }
                        });
                        return 1;
                    })
                )
            )
            .then(Commands.literal("unregister")
                .requires(source -> source.getSender().hasPermission("pklogin.admin.unregister"))
                .then(Commands.argument("player", StringArgumentType.word())
                    .executes(context -> {
                        CommandSender sender = context.getSource().getSender();
                        String targetName = context.getArgument("player", String.class);
                        plugin.runAsync(() -> {
                            if (plugin.getAccountManagement().removePassword(targetName)) {
                                plugin.getLoginManagement().cleanup(targetName);
                                sender.sendMessage(Messages.ADMIN_UNREGISTER_SUCCESS.asString().replace("{0}", targetName));
                            } else {
                                sender.sendMessage(Messages.ADMIN_ACCOUNT_NOT_FOUND.asString());
                            }
                        });
                        return 1;
                    })
                )
            )
            .then(Commands.literal("delete")
                .requires(source -> source.getSender().hasPermission("pklogin.admin.delete"))
                .then(Commands.argument("player", StringArgumentType.word())
                    .executes(context -> {
                        CommandSender sender = context.getSource().getSender();
                        String targetName = context.getArgument("player", String.class);
                        plugin.runAsync(() -> {
                            if (plugin.getAccountManagement().delete(targetName)) {
                                plugin.getLoginManagement().cleanup(targetName);
                                sender.sendMessage(Messages.ADMIN_DELETE_SUCCESS.asString().replace("{0}", targetName));
                            } else {
                                sender.sendMessage(Messages.ADMIN_ACCOUNT_NOT_FOUND.asString());
                            }
                        });
                        return 1;
                    })
                )
            )
            .then(Commands.literal("verify")
                .requires(source -> source.getSender().hasPermission("pklogin.admin.verify"))
                .then(Commands.argument("player", StringArgumentType.word())
                    .executes(context -> {
                        CommandSender sender = context.getSource().getSender();
                        String targetName = context.getArgument("player", String.class);
                        plugin.runAsync(() -> {
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
                        });
                        return 1;
                    })
                )
            )
            .then(Commands.literal("setspawn")
                .requires(source -> source.getSender().hasPermission("pklogin.admin.setspawn") && source.getSender() instanceof Player)
                .executes(context -> {
                    Player player = (Player) context.getSource().getSender();
                    plugin.runAsync(() -> {
                        File file = new File(plugin.getDataFolder(), "spawn.yml");
                        org.bukkit.configuration.file.YamlConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
                        config.set("spawn", player.getLocation());
                        try {
                            config.save(file);
                            player.sendMessage(Messages.ADMIN_SETSPAWN_SUCCESS.asString());
                        } catch (IOException e) {
                            e.printStackTrace();
                            player.sendMessage("§cFailed to save spawn location.");
                        }
                    });
                    return 1;
                })
            )
            .then(Commands.literal("changepass")
                .requires(source -> source.getSender().hasPermission("pklogin.admin.changepass"))
                .then(Commands.argument("player", StringArgumentType.word())
                    .then(Commands.argument("newpass", StringArgumentType.word())
                        .executes(context -> {
                            CommandSender sender = context.getSource().getSender();
                            String targetName = context.getArgument("player", String.class);
                            String newPass = context.getArgument("newpass", String.class);
                            plugin.runAsync(() -> {
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
                            });
                            return 1;
                        })
                    )
                )
            )
            .then(Commands.literal("dupeip")
                .requires(source -> source.getSender().hasPermission("pklogin.admin.dupeip"))
                .then(Commands.argument("target", StringArgumentType.word())
                    .executes(context -> {
                        CommandSender sender = context.getSource().getSender();
                        String target = context.getArgument("target", String.class);
                        plugin.runAsync(() -> {
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
                        });
                        return 1;
                    })
                )
            )
            .then(Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("pklogin.admin.reload"))
                .executes(context -> {
                    CommandSender sender = context.getSource().getSender();
                    plugin.runAsync(() -> reloadLogic(plugin, sender));
                    return 1;
                })
            )
            .then(Commands.literal("update")
                .requires(source -> source.getSender().hasPermission("pklogin.admin.update") && source.getSender() instanceof Player)
                .executes(context -> {
                    Player player = (Player) context.getSource().getSender();
                    plugin.runAsync(() -> {
                        String name = player.getName();
                        if (!plugin.getLoginManagement().isAuthenticated(name)) {
                            return;
                        }

                        if (!plugin.isUpdateAvailable()) {
                            player.sendMessage("§cYou are already using the latest version.");
                            return;
                        }

                        if (downloadLock.getAndSet(true)) {
                            player.sendMessage("§cDownload in progress...");
                        } else if (!update(plugin, player)) {
                            downloadLock.set(false);
                        }
                    });
                    return 1;
                })
            )
            .then(Commands.literal("help")
                .requires(source -> source.getSender().hasPermission("pklogin.admin.help"))
                .executes(context -> {
                    CommandSender sender = context.getSource().getSender();
                    plugin.runAsync(() -> sendHelp(sender));
                    return 1;
                })
            ).build();
    }

    private static void sendInfo(PkLoginPaper plugin, CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(" §eThis server is running §fPkLogin v " + plugin.getDescription().getVersion() + ".");
        sender.sendMessage(" §7Powered by §bwww.pumpkiiings.com§7.");
        sender.sendMessage("");
        sender.sendMessage(" §7GitHub: §fhttps://github.com/Pumpkiiiings/PkLogin");
        sender.sendMessage("");
    }

    private static void sendHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(" §ePkLogin Paper Admin Commands:");
        sender.sendMessage(" §7/pklogin help §f- Show this help message");
        sender.sendMessage(" §7/pklogin authme-import §f- Import AuthMe database");
        sender.sendMessage(" §7/pklogin forcelogin <player> §f- Force a player to login");
        sender.sendMessage(" §7/pklogin unregister <player> §f- Unregister a player's account");
        sender.sendMessage(" §7/pklogin delete <player> §f- Delete a player's account data");
        sender.sendMessage(" §7/pklogin changepass <player> <newpass> §f- Force change a password");
        sender.sendMessage(" §7/pklogin verify <player> §f- Check player's account details");
        sender.sendMessage(" §7/pklogin dupeip <player/ip> §f- Check accounts by IP");
        sender.sendMessage(" §7/pklogin setspawn §f- Set the authentication spawn point");
        sender.sendMessage(" §7/pklogin reload §f- Reload configuration");
        sender.sendMessage(" §7/pklogin update §f- Update plugin to the latest version");
        sender.sendMessage("");
    }

    private static void reloadLogic(PkLoginPaper plugin, CommandSender sender) {
        if (sender instanceof Player && !plugin.getLoginManagement().isAuthenticated(sender.getName())) {
            return;
        }
        plugin.reloadConfig();
        plugin.setupSettings();
        sender.sendMessage(Messages.PLUGIN_RELOAD_MESSAGE.asString());
    }

    private static boolean update(PkLoginPaper plugin, Player player) {
        File output = new File(plugin.getDataFolder().getParentFile(), "PkLogin-" + plugin.getLatestVersion() + ".jar");
        return downloadActionbar(plugin, player, "https://github.com/Pumpkiiiings/PkLogin/releases/download/" + plugin.getLatestVersion() + "/PkLogin.jar", output, true, null);
    }

    private static boolean downloadActionbar(PkLoginPaper plugin, Player player, String url, File output, boolean update, Runnable callback) {
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

    private static String repeatString(String str, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(str);
        }
        return builder.toString();
    }
}
