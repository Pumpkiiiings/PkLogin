package com.pumpkiiings.pklogin.bukkit.command.executors;

import com.pumpkiiings.pklogin.bukkit.PkLoginBukkit;
import com.pumpkiiings.pklogin.bukkit.command.BukkitAbstractCommand;
import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.manager.LoginManagement;
import com.pumpkiiings.pklogin.common.model.Account;
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
            sender.sendMessage("§cOnly players can use this command.");
            return;
        }

        Player player = (Player) sender;
        String name = player.getName();
        LoginManagement loginManagement = plugin.getLoginManagement();
        AccountManagement accountManagement = plugin.getAccountManagement();

        if (!loginManagement.isAuthenticated(name)) {
            player.sendMessage("§cYou must be logged in to use this command.");
            return;
        }

        Optional<Account> accountOpt = accountManagement.retrieveOrLoad(name);
        if (!accountOpt.isPresent()) {
            player.sendMessage("§cYou must be registered to use this command.");
            return;
        }

        Account account = accountOpt.get();
        String currentType = account.getUuidType() != null ? account.getUuidType() : "REAL";

        if (args.length == 1) {
            String newType = args[0].toUpperCase();
            if (newType.equals("REAL") || newType.equals("OFFLINE") || newType.equals("RANDOM")) {
                if (currentType.equals(newType)) {
                    player.sendMessage("§cYour account is already set to " + newType + " mode.");
                    return;
                }

                accountManagement.updateUuidType(name, newType);
                accountManagement.invalidateCache(name);

                player.sendMessage("§aYour account mode has been changed to " + newType + ".");
                player.sendMessage("§eChanges will apply on your next login.");
            } else {
                player.sendMessage("§cInvalid mode! Use: /premium <REAL|OFFLINE|RANDOM>");
            }
        } else {
            if (currentType.equals("REAL")) {
                player.sendMessage("§eYour account is currently in REAL mode.");
                player.sendMessage("§eUse §a/premium OFFLINE §eto switch to cracked mode.");
            } else {
                player.sendMessage("§eYour account is currently in " + currentType + " mode.");
                player.sendMessage("§eUse §a/premium REAL §eto secure your account with Mojang auth.");
            }
        }
    }
}
