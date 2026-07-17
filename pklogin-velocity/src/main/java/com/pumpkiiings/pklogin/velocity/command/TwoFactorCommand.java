package com.pumpkiiings.pklogin.velocity.command;

import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorManager;
import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.velocity.PkLoginVelocity;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

import java.util.Optional;

public class TwoFactorCommand extends VelocityAbstractCommand {

    public TwoFactorCommand(PkLoginVelocity plugin) {
        super(plugin, true, null);
    }

    @Override
    public java.util.concurrent.CompletableFuture<java.util.List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            String prefix = args.length == 1 ? args[0].toLowerCase() : "";
            java.util.List<String> subcommands = java.util.Arrays.asList("discord");
            return java.util.concurrent.CompletableFuture.completedFuture(
                subcommands.stream().filter(cmd -> cmd.startsWith(prefix)).collect(java.util.stream.Collectors.toList())
            );
        }
        return java.util.concurrent.CompletableFuture.completedFuture(java.util.List.of());
    }

    @Override
    protected void performPlayer(Player player, String alias, String[] args) {
        if (args.length < 1) {
            sendMessage(player, Messages.TWO_FACTOR_USAGE.asString());
            return;
        }

        String subCommand = args[0].toLowerCase();
        String name = player.getUsername();
        AccountManagement accountManagement = plugin.getAccountManagement();
        Optional<Account> accountOpt = accountManagement.retrieveOrLoad(name);

        if (accountOpt.isEmpty()) {
            sendMessage(player, Messages.TWO_FACTOR_NOT_REGISTERED.asString());
            return;
        }

        Account account = accountOpt.get();

        if (subCommand.equals("discord")) {
            if (account.getDiscordId() != null) {
                sendMessage(player, Messages.TWO_FACTOR_DISCORD_ALREADY_LINKED.asString());
                return;
            }

            String code = TwoFactorManager.getInstance().generateLinkCode(name, "DISCORD");
            sendMessage(player, Messages.TWO_FACTOR_DISCORD_LINK_HEADER.asString());
            sendMessage(player, Messages.TWO_FACTOR_DISCORD_LINK_CODE.asString().replace("{0}", code));
            sendMessage(player, Messages.TWO_FACTOR_DISCORD_LINK_INSTRUCTION1.asString());
            sendMessage(player, Messages.TWO_FACTOR_DISCORD_LINK_INSTRUCTION2.asString());

        } else if (subCommand.equals("verify2fa")) {
            // Note: If they get here, they are already authenticated.
            sendMessage(player, "§cYou are already authenticated!");
        } else {
            sendMessage(player, Messages.TWO_FACTOR_USAGE.asString());
        }
    }

    @Override
    protected void performConsole(CommandSource console, String alias, String[] args) {
        sendMessage(console, "§cThis command is only for players.");
    }
}
