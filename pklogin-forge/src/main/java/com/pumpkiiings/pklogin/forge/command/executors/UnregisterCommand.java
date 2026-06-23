package com.pumpkiiings.pklogin.forge.command.executors;

import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.forge.PkLoginForge;
import com.pumpkiiings.pklogin.forge.command.ForgeAbstractCommand;
import com.pumpkiiings.pklogin.forge.serializer.chat.ChatComponentSerializer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class UnregisterCommand extends ForgeAbstractCommand {

    public UnregisterCommand() {
        super("unregister", true, 0); // Requires auth
    }

    @Override
    protected void perform(ServerPlayer player, String label, String[] args) {
        if (args.length != 1) {
            player.sendSystemMessage(ChatComponentSerializer.fromText("§eUsage: /unregister <password>"));
            return;
        }

        String name = player.getGameProfile().getName();
        String password = args[0];

        AccountManagement accountManagement = PkLoginForge.getInstance().getAccountManagement();
        Optional<Account> accountOpt = accountManagement.retrieveOrLoad(name);
        
        if (accountOpt.isPresent() && accountManagement.comparePassword(accountOpt.get(), password)) {
            accountManagement.delete(name);
            PkLoginForge.getInstance().getLoginManagement().cleanup(name);
            player.connection.disconnect(ChatComponentSerializer.fromText(Messages.UNREGISTER_KICK.asString("§aAccount successfully unregistered.")));
        } else {
            player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.INCORRECT_PASSWORD.asString("§cIncorrect password.")));
        }
    }
}
