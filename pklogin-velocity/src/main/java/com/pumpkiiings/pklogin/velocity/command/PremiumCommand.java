package com.pumpkiiings.pklogin.velocity.command;

import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.velocity.PkLoginVelocity;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

import java.util.Optional;

public class PremiumCommand extends VelocityAbstractCommand {

    public PremiumCommand(PkLoginVelocity plugin) {
        super(plugin, true, null);
    }

    @Override
    public java.util.concurrent.CompletableFuture<java.util.List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            String prefix = args.length == 1 ? args[0].toLowerCase() : "";
            java.util.List<String> subcommands = java.util.Arrays.asList("confirm");
            return java.util.concurrent.CompletableFuture.completedFuture(
                subcommands.stream().filter(cmd -> cmd.startsWith(prefix)).collect(java.util.stream.Collectors.toList())
            );
        }
        return java.util.concurrent.CompletableFuture.completedFuture(java.util.List.of());
    }

    @Override
    protected void performPlayer(Player player, String alias, String[] args) {
        String name = player.getUsername();
        AccountManagement accountManagement = plugin.getAccountManagement();

        Optional<Account> accountOpt = accountManagement.retrieveOrLoad(name);
        if (accountOpt.isEmpty()) {
            sendMessage(player, Messages.NOT_REGISTERED.asString());
            return;
        }

        Account account = accountOpt.get();
        String currentType = account.getUuidType() != null ? account.getUuidType() : "REAL";

        if (args.length == 1 && args[0].equalsIgnoreCase("confirm")) {
            if (currentType.equals("REAL")) {
                sendMessage(player, Messages.PREMIUM_ALREADY.asString());
                return;
            }
            accountManagement.updateUuidType(name, "REAL");
            accountManagement.invalidateCache(name);
            sendMessage(player, Messages.PREMIUM_SUCCESS.asString());
            player.disconnect(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize("§aHas cambiado a modo Premium.\n§ePor favor vuelve a conectarte al servidor."));
        } else {
            if (currentType.equals("REAL")) {
                sendMessage(player, Messages.PREMIUM_ALREADY.asString());
            } else {
                for (String msg : Messages.PREMIUM_WARNING.asList()) {
                    sendMessage(player, msg);
                }
            }
        }
    }

    @Override
    protected void performConsole(CommandSource console, String alias, String[] args) {
        sendMessage(console, "§cThis command is only for players.");
    }
}
