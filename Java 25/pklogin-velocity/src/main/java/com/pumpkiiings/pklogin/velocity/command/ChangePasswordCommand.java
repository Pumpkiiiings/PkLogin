package com.pumpkiiings.pklogin.velocity.command;

import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.security.hashing.HashStrategyFactory;
import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.common.settings.Settings;
import com.pumpkiiings.pklogin.velocity.PkLoginVelocity;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

import java.util.Optional;

public class ChangePasswordCommand extends VelocityAbstractCommand {

    public ChangePasswordCommand(PkLoginVelocity plugin) {
        super(plugin, true, null); // true means requiresAuth, null means no permission required
    }

    @Override
    protected void performPlayer(Player player, String alias, String[] args) {
        if (args.length != 2) {
            sendMessage(player, Messages.MESSAGE_CHANGEPASSWORD.asString());
            return;
        }

        String currentPassword = args[0];
        String newPassword = args[1];
        int passwordLength = newPassword.length();

        if (passwordLength <= Settings.PASSWORD_SMALL.asInt()) {
            sendMessage(player, Messages.PASSWORD_TOO_SMALL.asString());
            return;
        }

        if (passwordLength >= Settings.PASSWORD_LARGE.asInt()) {
            sendMessage(player, Messages.PASSWORD_TOO_LARGE.asString());
            return;
        }

        if (currentPassword.equals(newPassword)) {
            sendMessage(player, Messages.PASSWORD_SAME_AS_OLD.asString());
            return;
        }

        if (Settings.SECURE_PASSWORDS_ENABLE.asBoolean()) {
            if (!newPassword.matches(Settings.SECURE_PASSWORDS_REGEX.asString())) {
                sendMessage(player, Messages.INSECURE_PASSWORD.asString());
                return;
            }
        }

        AccountManagement accountManagement = plugin.getAccountManagement();
        String name = player.getUsername();
        Optional<Account> accountOpt = accountManagement.retrieveOrLoad(name);
        
        if (accountOpt.isEmpty()) {
            sendMessage(player, Messages.NOT_REGISTERED.asString());
            return;
        }

        Account account = accountOpt.get();
        if (!accountManagement.comparePassword(account, currentPassword)) {
            sendMessage(player, Messages.PASSWORDS_DONT_MATCH.asString());
            return;
        }

        String hashedPassword = HashStrategyFactory.fromSettings().hash(newPassword);
        String address = player.getRemoteAddress().getAddress().getHostAddress();
        
        if (!accountManagement.update(name, hashedPassword, address)) {
            sendMessage(player, Messages.DATABASE_ERROR.asString());
            return;
        }

        sendMessage(player, Messages.PASSWORD_CHANGED.asString());
    }

    @Override
    protected void performConsole(CommandSource console, String alias, String[] args) {
        sendMessage(console, "§cThis command is only for players. Use /pklogin changepassword <player> <password> instead.");
    }
}
