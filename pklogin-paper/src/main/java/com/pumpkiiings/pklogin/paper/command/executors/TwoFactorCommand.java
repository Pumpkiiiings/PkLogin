package com.pumpkiiings.pklogin.paper.command.executors;

import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import com.pumpkiiings.pklogin.paper.command.BukkitAbstractCommand;
import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.manager.LoginManagement;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.settings.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class TwoFactorCommand extends BukkitAbstractCommand {

    public TwoFactorCommand(PkLoginPaper plugin) {
        super(plugin, "2fa");
    }

    @Override
    protected void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return;
        }

        Player player = (Player) sender;
        String name = player.getName();
        LoginManagement loginManagement = plugin.getLoginManagement();
        AccountManagement accountManagement = plugin.getAccountManagement();

        if (args.length < 1) {
            player.sendMessage(Messages.TWO_FACTOR_USAGE.asString());
            return;
        }

        String subCommand = args[0].toLowerCase();
        Optional<Account> accountOpt = accountManagement.retrieveOrLoad(name);

        if (!accountOpt.isPresent()) {
            player.sendMessage(Messages.TWO_FACTOR_NOT_REGISTERED.asString());
            return;
        }

        Account account = accountOpt.get();

        if (subCommand.equals("discord")) {
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

        } else if (subCommand.equals("verify2fa")) {
            if (args.length < 2) {
                player.sendMessage("§eUso: /2fa verify2fa <código>");
                return;
            }
            if (!loginManagement.isAwaiting2FA(name)) {
                player.sendMessage(Messages.TWO_FACTOR_NOT_AWAITING.asString());
                return;
            }
            String code = args[1];
            if (com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorManager.getInstance().verifyLoginCode(name, code)) {
                loginManagement.removeAwaiting2FA(name);
                loginManagement.setAuthenticated(name);
                player.sendMessage(Messages.TWO_FACTOR_LOGIN_SUCCESS.asString());
                player.getScheduler().run(plugin, task -> {
                    player.setWalkSpeed(0.2F);
                    player.setFlySpeed(0.1F);
                    com.pumpkiiings.pklogin.paper.manager.BukkitLimboManager.removeLimboState(plugin, player);
                    com.pumpkiiings.pklogin.paper.manager.BukkitLimboManager.restoreLastLocation(player);
                }, null);
                new com.pumpkiiings.pklogin.api.event.bukkit.AsyncAuthenticateEvent(player).callEvt();
            } else {
                player.sendMessage(Messages.TWO_FACTOR_INVALID_CODE.asString());
            }

        } else {
            player.sendMessage(Messages.TWO_FACTOR_USAGE.asString());
        }
    }
}
