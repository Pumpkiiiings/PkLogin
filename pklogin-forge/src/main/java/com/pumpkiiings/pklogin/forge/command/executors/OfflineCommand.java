package com.pumpkiiings.pklogin.forge.command.executors;

import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.manager.LoginManagement;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.forge.PkLoginForge;
import com.pumpkiiings.pklogin.forge.command.ForgeAbstractCommand;
import com.pumpkiiings.pklogin.forge.serializer.chat.ChatComponentSerializer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class OfflineCommand extends ForgeAbstractCommand {

    public OfflineCommand() {
        super("offline");
    }

    @Override
    protected void perform(ServerPlayer player, String label, String[] args) {
        String name = player.getGameProfile().getName();
        LoginManagement loginManagement = PkLoginForge.getInstance().getLoginManagement();
        AccountManagement accountManagement = PkLoginForge.getInstance().getAccountManagement();

        if (!loginManagement.isAuthenticated(name)) {
            player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_NOT_LOGGED_IN_SETUP.asString()));
            return;
        }

        Optional<Account> accountOpt = accountManagement.retrieveOrLoad(name);
        if (!accountOpt.isPresent()) {
            player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_NOT_REGISTERED.asString()));
            return;
        }

        Account account = accountOpt.get();
        String currentType = account.getUuidType() != null ? account.getUuidType() : "REAL";

        if (currentType.equals("OFFLINE")) {
            player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.OFFLINE_ALREADY.asString()));
        } else {
            accountManagement.updateUuidType(name, "OFFLINE");
            accountManagement.invalidateCache(name);
            player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.OFFLINE_SUCCESS.asString()));
        }
    }
}
