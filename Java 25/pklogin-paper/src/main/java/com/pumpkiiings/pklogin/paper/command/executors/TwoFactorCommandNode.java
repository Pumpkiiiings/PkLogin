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
                    context.getSource().getSender().sendMessage(
                            Messages.TWO_FACTOR_VERIFY_USAGE.asString("§eUsage: /2fa verify2fa <code>"));
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

        if (!plugin.isAuthenticated(player)) {
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

        com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorManager.VerificationResult result =
                com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorManager.getInstance()
                        .verifyLoginCode(name, code);

        switch (result) {
            case SUCCESS:
                loginManagement.removeAwaiting2FA(name);
                if (!plugin.authenticate(player)) return;
                player.sendMessage(Messages.TWO_FACTOR_LOGIN_SUCCESS.asString());
                player.getScheduler().run(plugin, task ->
                        com.pumpkiiings.pklogin.paper.manager.LimboManager.leaveLimbo(plugin, player), null);
                new com.pumpkiiings.pklogin.api.event.bukkit.AsyncAuthenticateEvent(player).callEvt();
                break;

            case TOO_MANY_ATTEMPTS:
                // The code is gone; drop them back to the login step rather than
                // leaving them stuck waiting for a code that no longer exists.
                loginManagement.removeAwaiting2FA(name);
                player.sendMessage(Messages.TWO_FACTOR_TOO_MANY_ATTEMPTS
                        .asString("§cToo many invalid 2FA codes. Please log in again."));
                break;

            case NO_PENDING_CODE:
                loginManagement.removeAwaiting2FA(name);
                player.sendMessage(Messages.TWO_FACTOR_NOT_AWAITING.asString());
                break;

            case INVALID_CODE:
            default:
                player.sendMessage(Messages.TWO_FACTOR_INVALID_CODE.asString());
                break;
        }
    }
}
