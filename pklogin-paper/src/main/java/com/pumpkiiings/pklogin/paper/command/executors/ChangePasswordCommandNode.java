package com.pumpkiiings.pklogin.paper.command.executors;

import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.security.hashing.HashStrategyFactory;
import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.common.settings.Settings;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;

public class ChangePasswordCommandNode {

    public static LiteralCommandNode<CommandSourceStack> build(PkLoginPaper plugin) {
        return Commands.literal("changepassword")
            .executes(context -> {
                context.getSource().getSender().sendMessage(Messages.MESSAGE_CHANGEPASSWORD.asString());
                return 1;
            })
            .then(Commands.argument("arg1", StringArgumentType.string())
                .executes(context -> {
                    context.getSource().getSender().sendMessage(Messages.MESSAGE_CHANGEPASSWORD.asString());
                    return 1;
                })
                .then(Commands.argument("arg2", StringArgumentType.string())
                    .executes(context -> {
                        CommandSender sender = context.getSource().getSender();
                        String arg1 = context.getArgument("arg1", String.class);
                        String arg2 = context.getArgument("arg2", String.class);
                        plugin.runAsync(() -> {
                            if (sender instanceof Player) {
                                performPlayer((Player) sender, plugin, arg1, arg2);
                            } else {
                                performConsole(sender, plugin, arg1, arg2);
                            }
                        });
                        return 1;
                    })
                )
            ).build();
    }

    private static void performPlayer(Player sender, PkLoginPaper plugin, String currentPassword, String newPassword) {
        int passwordLength = newPassword.length();

        if (passwordLength <= Settings.PASSWORD_SMALL.asInt()) {
            sender.sendMessage(Messages.PASSWORD_TOO_SMALL.asString());
            return;
        }

        if (passwordLength >= Settings.PASSWORD_LARGE.asInt()) {
            sender.sendMessage(Messages.PASSWORD_TOO_LARGE.asString());
            return;
        }

        if (currentPassword.equals(newPassword)) {
            sender.sendMessage(Messages.PASSWORD_SAME_AS_OLD.asString());
            return;
        }

        if (com.pumpkiiings.pklogin.common.settings.Settings.SECURE_PASSWORDS_ENABLE.asBoolean()) {
            if (!newPassword.matches(com.pumpkiiings.pklogin.common.settings.Settings.SECURE_PASSWORDS_REGEX.asString())) {
                sender.sendMessage(Messages.INSECURE_PASSWORD.asString());
                return;
            }
        }

        AccountManagement accountManagement = plugin.getAccountManagement();
        String name = sender.getName();
        Optional<Account> accountOpt = accountManagement.retrieveOrLoad(name);
        if (!accountOpt.isPresent()) {
            sender.sendMessage(Messages.NOT_REGISTERED.asString());
            return;
        }

        Account account = accountOpt.get();
        if (!accountManagement.comparePassword(account, currentPassword)) {
            sender.sendMessage(Messages.PASSWORDS_DONT_MATCH.asString());
            return;
        }

        String hashedPassword = HashStrategyFactory.fromSettings().hash(newPassword);
        String address = Objects.requireNonNull(sender.getAddress()).getAddress().getHostAddress();
        if (!accountManagement.update(name, hashedPassword, address, true)) {
            sender.sendMessage(Messages.DATABASE_ERROR.asString());
            return;
        }

        sender.sendMessage(Messages.PASSWORD_CHANGED.asString());

        com.pumpkiiings.pklogin.common.manager.LoginManagement loginManagement = plugin.getLoginManagement();
        if (loginManagement.mustChangePassword(name)) {
            loginManagement.removeMustChangePassword(name);
            loginManagement.setAuthenticated(name);
        }
    }

    private static void performConsole(CommandSender sender, PkLoginPaper plugin, String playerName, String newPassword) {
        if (!sender.hasPermission("pklogin.admin")) {
            sender.sendMessage(Messages.INSUFFICIENT_PERMISSIONS.asString());
            return;
        }

        int passwordLength = newPassword.length();

        if (passwordLength <= Settings.PASSWORD_SMALL.asInt()) {
            sender.sendMessage(Messages.PASSWORD_TOO_SMALL.asString());
            return;
        }

        if (passwordLength >= Settings.PASSWORD_LARGE.asInt()) {
            sender.sendMessage(Messages.PASSWORD_TOO_LARGE.asString());
            return;
        }

        if (com.pumpkiiings.pklogin.common.settings.Settings.SECURE_PASSWORDS_ENABLE.asBoolean()) {
            if (!newPassword.matches(com.pumpkiiings.pklogin.common.settings.Settings.SECURE_PASSWORDS_REGEX.asString())) {
                sender.sendMessage(Messages.INSECURE_PASSWORD.asString());
                return;
            }
        }

        Player playerIfOnline = plugin.getServer().getPlayerExact(playerName);
        if (playerIfOnline != null) {
            playerName = playerIfOnline.getName();
        }

        AccountManagement accountManagement = plugin.getAccountManagement();
        Optional<Account> accountOpt = accountManagement.retrieveOrLoad(playerName);
        if (!accountOpt.isPresent()) {
            sender.sendMessage(Messages.NOT_REGISTERED.asString());
            return;
        }

        Account account = accountOpt.get();
        if (accountManagement.comparePassword(account, newPassword)) {
            sender.sendMessage(Messages.PASSWORD_SAME_AS_OLD.asString());
            return;
        }

        String hashedPassword = HashStrategyFactory.fromSettings().hash(newPassword);
        String address = playerIfOnline != null ?
                Objects.requireNonNull(playerIfOnline.getAddress()).getAddress().getHostAddress() : null;
        if (!accountManagement.update(playerName, hashedPassword, address, true)) {
            sender.sendMessage(Messages.DATABASE_ERROR.asString());
            return;
        }

        sender.sendMessage(Messages.PASSWORD_CHANGED.asString());

        if (playerIfOnline != null) {
            playerIfOnline.sendMessage(Messages.PASSWORD_CHANGED.asString());
        }
    }
}
