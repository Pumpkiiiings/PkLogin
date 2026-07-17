package com.pumpkiiings.pklogin.paper.command.executors;

import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.manager.LoginManagement;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.settings.Messages;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class TwoFactorCommandNode {

    public static LiteralCommandNode<CommandSourceStack> build(PkLoginPaper plugin) {
        return Commands.literal("2fa")
            .requires(source -> source.getSender() instanceof Player)
            .executes(context -> {
                context.getSource().getSender().sendMessage(Messages.TWO_FACTOR_USAGE.asString());
                return 1;
            })
            .then(Commands.literal("discord")
                .executes(context -> {
                    CommandSender sender = context.getSource().getSender();
                    plugin.runAsync(() -> performDiscord(plugin, (Player) sender));
                    return 1;
                })
            )
            .then(Commands.literal("verify2fa")
                .executes(context -> {
                    context.getSource().getSender().sendMessage("§eUso: /2fa verify2fa <código>");
                    return 1;
                })
                .then(Commands.argument("code", StringArgumentType.word())
                    .executes(context -> {
                        CommandSender sender = context.getSource().getSender();
                        String code = context.getArgument("code", String.class);
                        plugin.runAsync(() -> performVerify(plugin, (Player) sender, code));
                        return 1;
                    })
                )
            ).build();
    }

    private static void performDiscord(PkLoginPaper plugin, Player player) {
        String name = player.getName();
        LoginManagement loginManagement = plugin.getLoginManagement();
        AccountManagement accountManagement = plugin.getAccountManagement();

        Optional<Account> accountOpt = accountManagement.retrieveOrLoad(name);
        if (!accountOpt.isPresent()) {
            player.sendMessage(Messages.TWO_FACTOR_NOT_REGISTERED.asString());
            return;
        }
        Account account = accountOpt.get();

        if (!loginManagement.isAuthenticated(name)) {
            player.sendMessage(Messages.TWO_FACTOR_DISCORD_NOT_LOGGED_IN.asString());
            return;
        }
        if (account.getDiscordId() != null) {
            player.sendMessage(Messages.TWO_FACTOR_DISCORD_ALREADY_LINKED.asString());
            return;
        }

        String code = com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorManager.getInstance().generateLinkCode(name, "DISCORD");
        player.sendMessage(Messages.TWO_FACTOR_DISCORD_LINK_HEADER.asString());
        player.sendMessage(Messages.TWO_FACTOR_DISCORD_LINK_CODE.asString().replace("{0}", code));
        player.sendMessage(Messages.TWO_FACTOR_DISCORD_LINK_INSTRUCTION1.asString());
        player.sendMessage(Messages.TWO_FACTOR_DISCORD_LINK_INSTRUCTION2.asString());
    }

    private static void performVerify(PkLoginPaper plugin, Player player, String code) {
        String name = player.getName();
        LoginManagement loginManagement = plugin.getLoginManagement();
        AccountManagement accountManagement = plugin.getAccountManagement();

        Optional<Account> accountOpt = accountManagement.retrieveOrLoad(name);
        if (!accountOpt.isPresent()) {
            player.sendMessage(Messages.TWO_FACTOR_NOT_REGISTERED.asString());
            return;
        }

        if (!loginManagement.isAwaiting2FA(name)) {
            player.sendMessage(Messages.TWO_FACTOR_NOT_AWAITING.asString());
            return;
        }

        if (com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorManager.getInstance().verifyLoginCode(name, code)) {
            loginManagement.removeAwaiting2FA(name);
            loginManagement.setAuthenticated(name);
            player.sendMessage(Messages.TWO_FACTOR_LOGIN_SUCCESS.asString());
            player.getScheduler().run(plugin, task -> {
                player.setWalkSpeed(0.2F);
                player.setFlySpeed(0.1F);
                com.pumpkiiings.pklogin.paper.manager.LimboManager.removeLimboState(plugin, player);
                com.pumpkiiings.pklogin.paper.manager.LimboManager.restoreLastLocation(player);
            }, null);
            new com.pumpkiiings.pklogin.api.event.bukkit.AsyncAuthenticateEvent(player).callEvt();
        } else {
            player.sendMessage(Messages.TWO_FACTOR_INVALID_CODE.asString());
        }
    }
}
