package com.pumpkiiings.pklogin.paper.command.executors;

import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.manager.LoginManagement;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.settings.Messages;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class OfflineCommandNode {

    public static LiteralCommandNode<CommandSourceStack> build(PkLoginPaper plugin) {
        return Commands.literal("offline")
            .requires(source -> source.getSender() instanceof Player)
            .executes(context -> {
                CommandSender sender = context.getSource().getSender();
                plugin.runAsync(() -> {
                    Player player = (Player) sender;
                    String name = player.getName();
                    LoginManagement loginManagement = plugin.getLoginManagement();
                    AccountManagement accountManagement = plugin.getAccountManagement();

                    if (!plugin.isAuthenticated(player)) {
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
                        player.getScheduler().run(plugin, task -> player.kick(
                                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                                        .deserialize(Messages.OFFLINE_SWITCH_KICK.asString(
                                                "§aYou have switched to Offline mode.\n§ePlease reconnect to the server."))), null);
                    }
                });
                return 1;
            }).build();
    }
}
