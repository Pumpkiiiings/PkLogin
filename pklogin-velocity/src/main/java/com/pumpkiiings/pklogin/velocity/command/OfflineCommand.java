package com.pumpkiiings.pklogin.velocity.command;

import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.velocity.PkLoginVelocity;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

import java.util.Optional;

public class OfflineCommand extends VelocityAbstractCommand {

    public OfflineCommand(PkLoginVelocity plugin) {
        super(plugin, true, null);
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

        if (currentType.equals("OFFLINE")) {
            sendMessage(player, Messages.OFFLINE_ALREADY.asString());
        } else {
            accountManagement.updateUuidType(name, "OFFLINE");
            accountManagement.invalidateCache(name);
            sendMessage(player, Messages.OFFLINE_SUCCESS.asString());
            player.disconnect(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize("§aHas cambiado a modo Offline.\n§ePor favor vuelve a conectarte al servidor."));
        }
    }

    @Override
    protected void performConsole(CommandSource console, String alias, String[] args) {
        sendMessage(console, "§cThis command is only for players.");
    }
}
