package com.pumpkiiings.pklogin.forge.command.executors;

import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.security.hashing.HashStrategyFactory;
import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.forge.PkLoginForge;
import com.pumpkiiings.pklogin.forge.command.ForgeAbstractCommand;
import com.pumpkiiings.pklogin.forge.listener.ForgeAuthenticateListener;
import com.pumpkiiings.pklogin.forge.serializer.chat.ChatComponentSerializer;
import net.minecraft.server.level.ServerPlayer;

public class RegisterCommand extends ForgeAbstractCommand {

    public RegisterCommand() {
        super("register");
    }

    @Override
    protected void perform(ServerPlayer player, String label, String[] args) {
        String name = player.getGameProfile().getName();
        AccountManagement accountManagement = PkLoginForge.getInstance().getAccountManagement();
        
        if (accountManagement.retrieveOrLoad(name).isPresent()) {
            player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.ALREADY_REGISTERED.asString("§cYou are already registered!")));
            return;
        }

        if (args.length != 2) {
            player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.MESSAGE_REGISTER.asString("§ePlease register using /register <password> <password>")));
            return;
        }

        if (com.pumpkiiings.pklogin.common.settings.Settings.SECURITY_IP_LIMIT_ENABLE.asBoolean()) {
            int ipLimit = com.pumpkiiings.pklogin.common.settings.Settings.SECURITY_IP_LIMIT.asInt();
            if (accountManagement.countAccountsByIp(player.getIpAddress()) >= ipLimit) {
                player.sendSystemMessage(ChatComponentSerializer.fromText("§cHas alcanzado el límite máximo de cuentas registradas en esta IP."));
                return;
            }
        }

        String password = args[0];
        String confirmPassword = args[1];

        if (!password.equals(confirmPassword)) {
            player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.PASSWORDS_DONT_MATCH.asString("§cPasswords do not match!")));
            return;
        }

        String hash = HashStrategyFactory.fromSettings().hash(password);
        accountManagement.update(name, hash, player.getIpAddress());
        PkLoginForge.getInstance().getLoginManagement().setAuthenticated(name);
        
        ForgeAuthenticateListener.onAuthenticate(player, true);
    }
}
