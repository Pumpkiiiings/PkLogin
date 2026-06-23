package com.pumpkiiings.pklogin.forge.command.executors;

import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.security.hashing.HashStrategyFactory;
import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.forge.PkLoginForge;
import com.pumpkiiings.pklogin.forge.command.ForgeAbstractCommand;
import com.pumpkiiings.pklogin.forge.serializer.chat.ChatComponentSerializer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class ChangePasswordCommand extends ForgeAbstractCommand {

    public ChangePasswordCommand() {
        super("changepassword", true, 0); // Requires auth
    }

    @Override
    protected void perform(ServerPlayer player, String label, String[] args) {
        if (args.length != 2) {
            player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.MESSAGE_CHANGEPASSWORD.asString("§eUsage: /changepassword <old> <new>")));
            return;
        }

        String name = player.getGameProfile().getName();
        String oldPassword = args[0];
        String newPassword = args[1];

        AccountManagement accountManagement = PkLoginForge.getInstance().getAccountManagement();
        Optional<Account> accountOpt = accountManagement.retrieveOrLoad(name);
        
        if (accountOpt.isPresent() && accountManagement.comparePassword(accountOpt.get(), oldPassword)) {
            String hash = HashStrategyFactory.fromSettings().hash(newPassword);
            accountManagement.update(name, hash, accountOpt.get().getAddress());
            player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.PASSWORD_CHANGED.asString("§aPassword changed successfully!")));
        } else {
            player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.INCORRECT_PASSWORD.asString("§cIncorrect old password.")));
        }
    }
}
