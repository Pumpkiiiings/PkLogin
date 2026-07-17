package com.pumpkiiings.pklogin.bukkit.command.executors;

import com.pumpkiiings.pklogin.bukkit.PkLoginBukkit;
import com.pumpkiiings.pklogin.bukkit.command.BukkitAbstractCommand;
import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.manager.LoginManagement;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.settings.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class PremiumCommand extends BukkitAbstractCommand {

    private final PkLoginBukkit plugin;

    public PremiumCommand(PkLoginBukkit plugin) {
        super(plugin, "premium");
        this.plugin = plugin;
    }

    @Override
    protected void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.INSUFFICIENT_PERMISSIONS.asString());
            return;
        }

        Player player = (Player) sender;
        String name = player.getName();
        LoginManagement loginManagement = plugin.getLoginManagement();
        AccountManagement accountManagement = plugin.getAccountManagement();

        if (!loginManagement.isAuthenticated(name)) {
            player.sendMessage(Messages.TWO_FACTOR_NOT_LOGGED_IN_SETUP.asString()); // Can reuse or better, but anyway
            return;
        }

        Optional<Account> accountOpt = accountManagement.retrieveOrLoad(name);
        if (!accountOpt.isPresent()) {
            player.sendMessage(Messages.TWO_FACTOR_NOT_REGISTERED.asString());
            return;
        }

        Account account = accountOpt.get();
        String currentType = account.getUuidType() != null ? account.getUuidType() : "REAL";

        if (args.length == 1 && args[0].equalsIgnoreCase("confirm")) {
            if (currentType.equals("REAL")) {
                player.sendMessage(Messages.PREMIUM_ALREADY.asString());
                return;
            }
            accountManagement.updateUuidType(name, "REAL");
            accountManagement.invalidateCache(name);
            player.sendMessage(Messages.PREMIUM_SUCCESS.asString());
            plugin.getFoliaLib().runAtEntity(player, task -> player.kickPlayer("§aHas cambiado a modo Premium.\n§ePor favor vuelve a conectarte al servidor."));
        } else {
            if (currentType.equals("REAL")) {
                player.sendMessage(Messages.PREMIUM_ALREADY.asString());
            } else {
                for (String msg : Messages.PREMIUM_WARNING.asList()) {
                    player.sendMessage(msg);
                }
            }
        }
    }
}
