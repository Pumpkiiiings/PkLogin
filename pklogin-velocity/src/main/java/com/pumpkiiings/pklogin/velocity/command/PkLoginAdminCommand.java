package com.pumpkiiings.pklogin.velocity.command;

import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.security.hashing.HashStrategyFactory;
import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.velocity.PkLoginVelocity;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

public class PkLoginAdminCommand extends VelocityAbstractCommand {

    public PkLoginAdminCommand(PkLoginVelocity plugin) {
        super(plugin, false, "pklogin.admin");
    }

    @Override
    protected void performPlayer(Player player, String alias, String[] args) {
        performConsole(player, alias, args);
    }

    @Override
    protected void performConsole(CommandSource sender, String alias, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        String subcommand = args[0].toLowerCase();
        AccountManagement accountManagement = plugin.getAccountManagement();

        switch (subcommand) {
            case "unregister": {
                if (!sender.hasPermission("pklogin.admin.unregister")) {
                    sendMessage(sender, Messages.INSUFFICIENT_PERMISSIONS.asString());
                    return;
                }
                if (args.length < 2) {
                    sendMessage(sender, Messages.ADMIN_USAGE_COMMAND.asString().replace("%command%", "/pklogin").replace("%arg%", "unregister <player>"));
                    return;
                }
                String targetName = args[1];
                if (accountManagement.removePassword(targetName)) {
                    accountManagement.invalidateCache(targetName);
                    sendMessage(sender, Messages.ADMIN_UNREGISTER_SUCCESS.asString().replace("{0}", targetName));
                } else {
                    sendMessage(sender, Messages.ADMIN_ACCOUNT_NOT_FOUND.asString());
                }
                return;
            }

            case "forcelogin": {
                if (!sender.hasPermission("pklogin.admin.forcelogin")) {
                    sendMessage(sender, Messages.INSUFFICIENT_PERMISSIONS.asString());
                    return;
                }
                if (args.length < 2) {
                    sendMessage(sender, Messages.ADMIN_USAGE_COMMAND.asString().replace("%command%", "/pklogin").replace("%arg%", "forcelogin <player>"));
                    return;
                }
                String targetName = args[1];
                Optional<Player> targetPlayerOpt = plugin.getServer().getPlayer(targetName);
                
                if (targetPlayerOpt.isPresent()) {
                    Player targetPlayer = targetPlayerOpt.get();
                    plugin.getAuthenticatedPlayers().add(targetPlayer.getUniqueId());
                    
                    com.google.common.io.ByteArrayDataOutput out = com.google.common.io.ByteStreams.newDataOutput();
                    out.writeUTF("PremiumAutoLogin");
                    out.writeUTF(targetName);
                    
                    targetPlayer.getCurrentServer().ifPresent(serverConnection -> {
                        serverConnection.sendPluginMessage(com.pumpkiiings.pklogin.velocity.listener.PluginMessageListener.IDENTIFIER, out.toByteArray());
                    });
                    
                    sendMessage(sender, Messages.ADMIN_FORCELOGIN_SUCCESS.asString().replace("{0}", targetName));
                } else {
                    sendMessage(sender, "§cPlayer is not online on the proxy.");
                }
                return;
            }

            case "delete": {
                if (!sender.hasPermission("pklogin.admin.delete")) {
                    sendMessage(sender, Messages.INSUFFICIENT_PERMISSIONS.asString());
                    return;
                }
                if (args.length < 2) {
                    sendMessage(sender, Messages.ADMIN_USAGE_COMMAND.asString().replace("%command%", "/pklogin").replace("%arg%", "delete <player>"));
                    return;
                }
                String targetName = args[1];
                if (accountManagement.delete(targetName)) {
                    accountManagement.invalidateCache(targetName);
                    sendMessage(sender, Messages.ADMIN_DELETE_SUCCESS.asString().replace("{0}", targetName));
                } else {
                    sendMessage(sender, Messages.ADMIN_ACCOUNT_NOT_FOUND.asString());
                }
                return;
            }

            case "verify": {
                if (!sender.hasPermission("pklogin.admin.verify")) {
                    sendMessage(sender, Messages.INSUFFICIENT_PERMISSIONS.asString());
                    return;
                }
                if (args.length < 2) {
                    sendMessage(sender, Messages.ADMIN_USAGE_COMMAND.asString().replace("%command%", "/pklogin").replace("%arg%", "verify <player>"));
                    return;
                }
                String targetName = args[1];
                Optional<Account> accOpt = accountManagement.search(targetName);
                if (accOpt.isPresent()) {
                    Account acc = accOpt.get();
                    for (String line : Messages.ADMIN_VERIFY_FORMAT.asList()) {
                        sendMessage(sender, line.replace("{0}", targetName)
                                .replace("{1}", acc.getAddress() != null ? acc.getAddress() : "N/A")
                                .replace("{2}", new Date(acc.getRegDate()).toString())
                                .replace("{3}", new Date(acc.getLastLogin()).toString())
                                .replace("{4}", (!"OFFLINE".equals(acc.getUuidType()) ? "Yes" : "No")));
                    }
                } else {
                    sendMessage(sender, Messages.ADMIN_ACCOUNT_NOT_FOUND.asString());
                }
                return;
            }

            case "changepass": {
                if (!sender.hasPermission("pklogin.admin.changepass")) {
                    sendMessage(sender, Messages.INSUFFICIENT_PERMISSIONS.asString());
                    return;
                }
                if (args.length < 3) {
                    sendMessage(sender, Messages.ADMIN_USAGE_COMMAND.asString().replace("%command%", "/pklogin").replace("%arg%", "changepass <player> <newpass>"));
                    return;
                }
                String targetName = args[1];
                String newPass = args[2];
                Optional<Account> targetAccount = accountManagement.search(targetName);
                if (targetAccount.isPresent()) {
                    String hash = HashStrategyFactory.fromSettings().hash(newPass);
                    accountManagement.update(targetName, hash, targetAccount.get().getAddress());
                    sendMessage(sender, Messages.ADMIN_CHANGEPASS_SUCCESS.asString().replace("{0}", targetName));
                    
                    Optional<Player> targetPlayer = plugin.getServer().getPlayer(targetName);
                    if (targetPlayer.isPresent()) {
                        targetPlayer.get().disconnect(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(Messages.ADMIN_CHANGEPASS_KICK.asString()));
                    }
                } else {
                    sendMessage(sender, "§cAccount not found.");
                }
                return;
            }

            case "dupeip": {
                if (!sender.hasPermission("pklogin.admin.dupeip")) {
                    sendMessage(sender, Messages.INSUFFICIENT_PERMISSIONS.asString());
                    return;
                }
                if (args.length < 2) {
                    sendMessage(sender, Messages.ADMIN_USAGE_COMMAND.asString().replace("%command%", "/pklogin").replace("%arg%", "dupeip <ip/player>"));
                    return;
                }
                String target = args[1];
                String ip = target;

                if (!target.contains(".")) {
                    Optional<Account> acc = accountManagement.search(target);
                    if (acc.isPresent() && acc.get().getAddress() != null) {
                        ip = acc.get().getAddress();
                    }
                }

                Map<String, Long> accounts = accountManagement.getAccountsByIp(ip);
                if (accounts.isEmpty()) {
                    sendMessage(sender, Messages.ADMIN_DUPEIP_NONE.asString());
                } else {
                    sendMessage(sender, Messages.ADMIN_DUPEIP_HEADER.asString().replace("{0}", target));
                    accounts.forEach((accName, lastLogin) -> {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
                        String dateStr = sdf.format(new Date(lastLogin));
                        sendMessage(sender, Messages.ADMIN_DUPEIP_FORMAT.asString().replace("{0}", accName).replace("{1}", dateStr));
                    });
                }
                return;
            }

            case "reload": {
                if (!sender.hasPermission("pklogin.admin.reload")) {
                    sendMessage(sender, Messages.INSUFFICIENT_PERMISSIONS.asString());
                    return;
                }
                plugin.reloadConfig();
                sendMessage(sender, "§aConfiguration and messages reloaded successfully.");
                return;
            }

            case "help": {
                if (!sender.hasPermission("pklogin.admin.help")) {
                    sendMessage(sender, Messages.INSUFFICIENT_PERMISSIONS.asString());
                    return;
                }
                sendHelp(sender);
                return;
            }

            default: {
                if (sender instanceof Player) {
                    // Forward unknown commands like forcelogin, setspawn, etc to the backend server
                    Player player = (Player) sender;
                    player.spoofChatInput("/" + alias + " " + String.join(" ", args));
                } else {
                    sendMessage(sender, "§cUnknown subcommand or this subcommand can only be executed by a player forwarded to Bukkit.");
                }
                return;
            }
        }
    }

    private void sendHelp(CommandSource sender) {
        for (String line : Messages.ADMIN_HELP.asList()) {
            sendMessage(sender, line);
        }
    }
}
