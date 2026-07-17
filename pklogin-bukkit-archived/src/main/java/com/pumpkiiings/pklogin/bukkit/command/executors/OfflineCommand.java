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

public class OfflineCommand extends BukkitAbstractCommand {

    private final PkLoginBukkit plugin;

    public OfflineCommand(PkLoginBukkit plugin) {
        super(plugin, "offline");
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
            player.sendMessage(Messages.TWO_FACTOR_NOT_LOGGED_IN_SETUP.asString());
            return;
        }

        Optional<Account> accountOpt = accountManagement.retrieveOrLoad(name);
        if (!accountOpt.isPresent()) {
            player.sendMessage(Messages.TWO_FACTOR_NOT_REGISTERED.asString());
            return;
        }

        Account account = accountOpt.get();
        String currentType = account.getUuidType() != null ? account.getUuidType() : "REAL";

        if (currentType.equals("OFFLINE")) {
            player.sendMessage(Messages.OFFLINE_ALREADY.asString());
        } else {
            accountManagement.updateUuidType(name, "OFFLINE");
            accountManagement.invalidateCache(name);
            player.sendMessage(Messages.OFFLINE_SUCCESS.asString());
            plugin.getFoliaLib().runAtEntity(player, task -> player.kickPlayer("§aHas cambiado a modo Offline.\n§ePor favor vuelve a conectarte al servidor."));
        }
    }
}
