package com.pumpkiiings.pklogin.velocity.command;

import com.pumpkiiings.pklogin.common.Permissions;

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
        super(plugin, false, Permissions.ADMIN);
    }

    @Override
    public java.util.concurrent.CompletableFuture<java.util.List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            String prefix = args.length == 1 ? args[0].toLowerCase() : "";
            java.util.List<String> subcommands = java.util.Arrays.asList(
                "help", "forcelogin", "unregister", "delete", "changepass", "verify", "dupeip", "cracked", "premium", "reload"
            );
            return java.util.concurrent.CompletableFuture.completedFuture(
                subcommands.stream().filter(cmd -> cmd.startsWith(prefix)).collect(java.util.stream.Collectors.toList())
            );
        } else if (args.length == 2) {
            String subcommand = args[0].toLowerCase();
            String prefix = args[1].toLowerCase();
            if (java.util.Arrays.asList("forcelogin", "unregister", "delete", "changepass", "verify", "dupeip", "cracked", "premium").contains(subcommand)) {
                return java.util.concurrent.CompletableFuture.completedFuture(
                    plugin.getServer().getAllPlayers().stream()
                        .map(Player::getUsername)
                        .filter(name -> name.toLowerCase().startsWith(prefix))
                        .collect(java.util.stream.Collectors.toList())
                );
            }
        }
        return java.util.concurrent.CompletableFuture.completedFuture(java.util.List.of());
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
                if (!sender.hasPermission(Permissions.ADMIN_UNREGISTER)) {
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
                if (!sender.hasPermission(Permissions.ADMIN_FORCELOGIN)) {
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

                    // Goes through the shared builder so the payload is signed the
                    // same way the backend verifies it.
                    com.pumpkiiings.pklogin.velocity.ProxyAuthMessages
                            .sendPremiumAutoLogin(plugin, targetPlayer);

                    sendMessage(sender, Messages.ADMIN_FORCELOGIN_SUCCESS.asString().replace("{0}", targetName));
                } else {
                    sendMessage(sender, Messages.PLAYER_NOT_ONLINE.asString("§cThat player is not online."));
                }
                return;
            }

            case "delete": {
                if (!sender.hasPermission(Permissions.ADMIN_DELETE)) {
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
                if (!sender.hasPermission(Permissions.ADMIN_VERIFY)) {
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
                if (!sender.hasPermission(Permissions.ADMIN_CHANGEPASS)) {
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
                if (!sender.hasPermission(Permissions.ADMIN_DUPEIP)) {
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

            case "cracked": {
                if (!sender.hasPermission(Permissions.ADMIN_CRACKED)) {
                    sendMessage(sender, Messages.INSUFFICIENT_PERMISSIONS.asString());
                    return;
                }
                if (args.length < 2) {
                    sendMessage(sender, Messages.ADMIN_USAGE_COMMAND.asString().replace("%command%", "/pklogin").replace("%arg%", "cracked <player>"));
                    return;
                }
                String targetName = args[1];
                Optional<Account> accOpt = accountManagement.search(targetName);
                if (accOpt.isPresent()) {
                    accountManagement.updateUuidType(targetName, "OFFLINE");
                    accountManagement.invalidateCache(targetName);
                    sendMessage(sender, Messages.ADMIN_CRACKED_SUCCESS.asString().replace("{0}", targetName));
                    Optional<Player> targetPlayer = plugin.getServer().getPlayer(targetName);
                    if (targetPlayer.isPresent()) {
                        targetPlayer.get().disconnect(
                            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                                .deserialize(Messages.OFFLINE_SWITCH_KICK.asString(
                                    "§aYou have been set to cracked mode.\n§ePlease reconnect to the server.")));
                    }
                } else {
                    sendMessage(sender, Messages.ADMIN_ACCOUNT_NOT_FOUND.asString());
                }
                return;
            }

            case "premium": {
                if (!sender.hasPermission(Permissions.ADMIN_PREMIUM)) {
                    sendMessage(sender, Messages.INSUFFICIENT_PERMISSIONS.asString());
                    return;
                }
                if (args.length < 2) {
                    sendMessage(sender, Messages.ADMIN_USAGE_COMMAND.asString().replace("%command%", "/pklogin").replace("%arg%", "premium <player>"));
                    return;
                }
                String targetName = args[1];
                Optional<Account> accOptP = accountManagement.search(targetName);
                if (accOptP.isPresent()) {
                    accountManagement.updateUuidType(targetName, "REAL");
                    accountManagement.invalidateCache(targetName);
                    sendMessage(sender, Messages.ADMIN_PREMIUM_SUCCESS.asString().replace("{0}", targetName));
                    Optional<Player> targetPlayer = plugin.getServer().getPlayer(targetName);
                    if (targetPlayer.isPresent()) {
                        targetPlayer.get().disconnect(
                            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                                .deserialize(Messages.PREMIUM_SWITCH_KICK.asString(
                                    "§aYou have been set to Premium mode.\n§ePlease reconnect to the server.")));
                    }
                } else {
                    sendMessage(sender, Messages.ADMIN_ACCOUNT_NOT_FOUND.asString());
                }
                return;
            }

            case "reload": {
                if (!sender.hasPermission(Permissions.ADMIN_RELOAD)) {
                    sendMessage(sender, Messages.INSUFFICIENT_PERMISSIONS.asString());
                    return;
                }
                plugin.reloadConfig();
                sendMessage(sender, "§aConfiguration and messages reloaded successfully.");
                return;
            }

            case "help": {
                if (!sender.hasPermission(Permissions.ADMIN_HELP)) {
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
