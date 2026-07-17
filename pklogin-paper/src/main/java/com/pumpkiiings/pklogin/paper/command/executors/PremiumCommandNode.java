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

public class PremiumCommandNode {

    public static LiteralCommandNode<CommandSourceStack> build(PkLoginPaper plugin) {
        return Commands.literal("premium")
            .requires(source -> source.getSender() instanceof Player)
            .executes(context -> {
                CommandSender sender = context.getSource().getSender();
                plugin.runAsync(() -> perform(plugin, (Player) sender, false));
                return 1;
            })
            .then(Commands.literal("confirm")
                .executes(context -> {
                    CommandSender sender = context.getSource().getSender();
                    plugin.runAsync(() -> perform(plugin, (Player) sender, true));
                    return 1;
                })
            ).build();
    }

    private static void perform(PkLoginPaper plugin, Player player, boolean confirm) {
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

        if (confirm) {
            if (currentType.equals("REAL")) {
                player.sendMessage(Messages.PREMIUM_ALREADY.asString());
                return;
            }
            accountManagement.updateUuidType(name, "REAL");
            accountManagement.invalidateCache(name);
            player.sendMessage(Messages.PREMIUM_SUCCESS.asString());
            plugin.getServer().getScheduler().runTask(plugin, () -> player.kick(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize("§aHas cambiado a modo Premium.\n§ePor favor vuelve a conectarte al servidor.")));
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
