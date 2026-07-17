package com.pumpkiiings.pklogin.paper.command.executors;

import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.settings.Messages;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class UnregisterCommandNode {

    public static LiteralCommandNode<CommandSourceStack> build(PkLoginPaper plugin) {
        return Commands.literal("unregister")
            .executes(context -> {
                context.getSource().getSender().sendMessage(Messages.MESSAGE_UNREGISTER.asString());
                return 1;
            })
            .then(Commands.argument("arg1", StringArgumentType.string())
                .executes(context -> {
                    CommandSender sender = context.getSource().getSender();
                    String arg1 = context.getArgument("arg1", String.class);
                    plugin.runAsync(() -> {
                        if (sender instanceof Player) {
                            performPlayer((Player) sender, plugin, arg1);
                        } else {
                            performConsole(sender, plugin, arg1);
                        }
                    });
                    return 1;
                })
            ).build();
    }

    private static void performPlayer(Player sender, PkLoginPaper plugin, String currentPassword) {
        AccountManagement accountManagement = plugin.getAccountManagement();
        String name = sender.getName();
        Optional<Account> accountOpt = accountManagement.retrieveOrLoad(name);
        if (!accountOpt.isPresent()) {
            sender.sendMessage(Messages.NOT_REGISTERED.asString());
            return;
        }

        Account account = accountOpt.get();
        if (!accountManagement.comparePassword(account, currentPassword)) {
            sender.sendMessage(Messages.INCORRECT_PASSWORD.asString());
            return;
        }

        if (!accountManagement.delete(name)) {
            sender.sendMessage(Messages.DATABASE_ERROR.asString());
            return;
        }

        sender.getScheduler().run(plugin, task -> sender.kick(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(Messages.UNREGISTER_KICK.asString())), null);
    }

    private static void performConsole(CommandSender sender, PkLoginPaper plugin, String playerName) {
        if (!sender.hasPermission("pklogin.admin")) {
            sender.sendMessage(Messages.INSUFFICIENT_PERMISSIONS.asString());
            return;
        }
        
        AccountManagement accountManagement = plugin.getAccountManagement();

        Optional<Account> accountOpt = accountManagement.retrieveOrLoad(playerName);
        if (!accountOpt.isPresent()) {
            sender.sendMessage(Messages.NOT_REGISTERED.asString());
            return;
        }

        if (!accountManagement.delete(playerName)) {
            sender.sendMessage(Messages.DATABASE_ERROR.asString());
            return;
        }

        Player playerIfOnline = plugin.getServer().getPlayer(playerName);
        if (playerIfOnline != null) {
            playerIfOnline.getScheduler().run(plugin, task -> playerIfOnline.kick(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(Messages.UNREGISTER_KICK.asString())), null);
        }

        sender.sendMessage(Messages.ADMIN_UNREGISTER_SUCCESS.asString());
    }
}
