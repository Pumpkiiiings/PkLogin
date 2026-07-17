package com.pumpkiiings.pklogin.paper.command.executors;

import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import com.pumpkiiings.pklogin.api.event.bukkit.AsyncAuthenticateEvent;
import com.pumpkiiings.pklogin.api.event.bukkit.AsyncRegisterEvent;
import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.manager.LoginManagement;
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

public class RegisterCommandNode {

    public static LiteralCommandNode<CommandSourceStack> build(PkLoginPaper plugin) {
        return Commands.literal("register")
            .executes(context -> {
                context.getSource().getSender().sendMessage(Messages.MESSAGE_REGISTER.asString());
                return 1;
            })
            .then(Commands.argument("arg1", StringArgumentType.string())
                .executes(context -> {
                    context.getSource().getSender().sendMessage(Messages.MESSAGE_REGISTER.asString());
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

    private static void performPlayer(Player sender, PkLoginPaper plugin, String password, String repeatPassword) {
        String name = sender.getName();
        LoginManagement loginManagement = plugin.getLoginManagement();
        if (loginManagement.isAuthenticated(name)) {
            sender.sendMessage(Messages.ALREADY_LOGIN.asString());
            return;
        }

        if (com.pumpkiiings.pklogin.common.settings.Settings.SECURITY_IP_LIMIT_ENABLE.asBoolean()) {
            int ipLimit = com.pumpkiiings.pklogin.common.settings.Settings.SECURITY_IP_LIMIT.asInt();
            String addressStr = sender.getAddress().getAddress().getHostAddress();
            if (plugin.getAccountManagement().countAccountsByIp(addressStr) >= ipLimit) {
                sender.sendMessage(Messages.MAX_IPS_REACHED.asString());
                return;
            }
        }

        int passwordLength = password.length();

        if (passwordLength <= Settings.PASSWORD_SMALL.asInt()) {
            sender.sendMessage(Messages.PASSWORD_TOO_SMALL.asString());
            return;
        }

        if (passwordLength >= Settings.PASSWORD_LARGE.asInt()) {
            sender.sendMessage(Messages.PASSWORD_TOO_LARGE.asString());
            return;
        }

        if (!password.equals(repeatPassword)) {
            sender.sendMessage(Messages.PASSWORDS_DONT_MATCH.asString());
            return;
        }

        if (com.pumpkiiings.pklogin.common.settings.Settings.SECURE_PASSWORDS_ENABLE.asBoolean()) {
            if (!password.matches(com.pumpkiiings.pklogin.common.settings.Settings.SECURE_PASSWORDS_REGEX.asString())) {
                sender.sendMessage(Messages.INSECURE_PASSWORD.asString());
                return;
            }
        }

        AccountManagement accountManagement = plugin.getAccountManagement();
        boolean exists = accountManagement.retrieveOrLoad(name).isPresent();
        if (exists) {
            sender.sendMessage(Messages.ALREADY_REGISTERED.asString());
            return;
        }

        String hashedPassword = HashStrategyFactory.fromSettings().hash(password);
        String address = sender.getAddress().getAddress().getHostAddress();
        if (!accountManagement.update(name, hashedPassword, address, false)) {
            sender.sendMessage(Messages.DATABASE_ERROR.asString());
            return;
        }

        AsyncRegisterEvent registerEvent = new AsyncRegisterEvent(sender);
        if (registerEvent.callEvt()) {
            plugin.getLoginManagement().setAuthenticated(name);

            com.pumpkiiings.pklogin.paper.util.AdventureAPI.showTitle(sender, Messages.TITLE_AFTER_REGISTER.asTitle().title, Messages.TITLE_AFTER_REGISTER.asTitle().subtitle, Messages.TITLE_AFTER_REGISTER.asTitle().start, Messages.TITLE_AFTER_REGISTER.asTitle().duration, Messages.TITLE_AFTER_REGISTER.asTitle().end);
            sender.sendMessage(Messages.SUCCESSFUL_REGISTER.asString());
            sender.getScheduler().run(plugin, task -> {
                sender.setWalkSpeed(0.2F);
                sender.setFlySpeed(0.1F);
                com.pumpkiiings.pklogin.paper.manager.LimboManager.removeLimboState(plugin, sender);
                com.pumpkiiings.pklogin.paper.manager.LimboManager.restoreLastLocation(sender);
            }, null);

            new AsyncAuthenticateEvent(sender).callEvt();
        }
    }

    private static void performConsole(CommandSender sender, PkLoginPaper plugin, String playerName, String password) {
        if (!sender.hasPermission("pklogin.admin")) {
            sender.sendMessage(Messages.INSUFFICIENT_PERMISSIONS.asString());
            return;
        }

        int passwordLength = password.length();

        if (passwordLength <= Settings.PASSWORD_SMALL.asInt()) {
            sender.sendMessage(Messages.PASSWORD_TOO_SMALL.asString());
            return;
        }

        if (passwordLength >= Settings.PASSWORD_LARGE.asInt()) {
            sender.sendMessage(Messages.PASSWORD_TOO_LARGE.asString());
            return;
        }

        Player playerIfOnline = plugin.getServer().getPlayerExact(playerName);
        if (playerIfOnline != null) {
            playerName = playerIfOnline.getName();
        }

        AccountManagement accountManagement = plugin.getAccountManagement();
        boolean exists = accountManagement.retrieveOrLoad(playerName).isPresent();
        if (exists) {
            sender.sendMessage(Messages.ALREADY_REGISTERED.asString());
            return;
        }

        String hashedPassword = HashStrategyFactory.fromSettings().hash(password);
        String address = playerIfOnline != null ?
                Objects.requireNonNull(playerIfOnline.getAddress()).getAddress().getHostAddress() : null;
        if (!accountManagement.update(playerName, hashedPassword, address, false)) {
            sender.sendMessage(Messages.DATABASE_ERROR.asString());
            return;
        }

        sender.sendMessage(Messages.SUCCESSFUL_REGISTER.asString());

        if (playerIfOnline != null) {
            AsyncRegisterEvent registerEvent = new AsyncRegisterEvent(playerIfOnline);
            if (registerEvent.callEvt()) {
                plugin.getLoginManagement().setAuthenticated(playerName);

                com.pumpkiiings.pklogin.paper.util.AdventureAPI.showTitle(playerIfOnline, Messages.TITLE_AFTER_REGISTER.asTitle().title, Messages.TITLE_AFTER_REGISTER.asTitle().subtitle, Messages.TITLE_AFTER_REGISTER.asTitle().start, Messages.TITLE_AFTER_REGISTER.asTitle().duration, Messages.TITLE_AFTER_REGISTER.asTitle().end);
                playerIfOnline.sendMessage(Messages.SUCCESSFUL_REGISTER.asString());

                playerIfOnline.getScheduler().run(plugin, task -> {
                    playerIfOnline.setWalkSpeed(0.2F);
                    playerIfOnline.setFlySpeed(0.1F);
                    com.pumpkiiings.pklogin.paper.manager.LimboManager.removeLimboState(plugin, playerIfOnline);
                    com.pumpkiiings.pklogin.paper.manager.LimboManager.restoreLastLocation(playerIfOnline);
                }, null);

                new AsyncAuthenticateEvent(playerIfOnline).callEvt();
            }
        }
    }
}
