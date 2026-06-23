package com.pumpkiiings.pklogin.bukkit.command.executors;

import com.pumpkiiings.pklogin.bukkit.PkLoginBukkit;
import com.pumpkiiings.pklogin.bukkit.command.BukkitAbstractCommand;
import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.manager.LoginManagement;
import com.pumpkiiings.pklogin.common.model.Account;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class TwoFactorCommand extends BukkitAbstractCommand {

    public TwoFactorCommand(PkLoginBukkit plugin) {
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
            player.sendMessage("§eUso: /2fa <discord|verify2fa>");
            return;
        }

        String subCommand = args[0].toLowerCase();
        Optional<Account> accountOpt = accountManagement.retrieveOrLoad(name);

        if (!accountOpt.isPresent()) {
            player.sendMessage("§cDebes estar registrado para usar 2FA.");
            return;
        }

        Account account = accountOpt.get();

        if (subCommand.equals("discord")) {
            if (!loginManagement.isAuthenticated(name)) {
                player.sendMessage("§cDebes estar logeado para vincular Discord.");
                return;
            }
            if (account.getDiscordId() != null) {
                player.sendMessage("§cYa tienes una cuenta de Discord vinculada.");
                return;
            }

            String code = com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorManager.getInstance().generateLinkCode(name, "DISCORD");
            player.sendMessage("§e--- Vinculación de Discord ---");
            player.sendMessage("§eCódigo: §a" + code);
            player.sendMessage("§ePor favor, envía un Mensaje Privado (DM) a nuestro Bot de Discord con este código.");
            player.sendMessage("§eEl código expirará en 10 minutos.");

        } else if (subCommand.equals("verify2fa")) {
            if (args.length < 2) {
                player.sendMessage("§eUso: /2fa verify2fa <código>");
                return;
            }
            if (!loginManagement.isAwaiting2FA(name)) {
                player.sendMessage("§cNo estás esperando un código de 2FA.");
                return;
            }
            String code = args[1];
            if (com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorManager.getInstance().verifyLoginCode(name, code)) {
                loginManagement.removeAwaiting2FA(name);
                loginManagement.setAuthenticated(name);
                player.sendMessage("§aHas iniciado sesión correctamente con 2FA.");
                plugin.getFoliaLib().runAtEntity(player, task -> {
                    player.setWalkSpeed(0.2F);
                    player.setFlySpeed(0.1F);
                    com.pumpkiiings.pklogin.bukkit.manager.BukkitLimboManager.removeLimboState(player);
                    com.pumpkiiings.pklogin.bukkit.manager.BukkitLimboManager.restoreLastLocation(player);
                });
                new com.pumpkiiings.pklogin.bukkit.api.events.AsyncAuthenticateEvent(player).callEvt();
            } else {
                player.sendMessage("§cCódigo 2FA incorrecto. Inténtalo de nuevo.");
            }

        } else {
            player.sendMessage("§eUso: /2fa <discord|verify2fa>");
        }
    }
}
