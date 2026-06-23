package com.pumpkiiings.pklogin.forge.command.executors;

import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.manager.LoginManagement;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.forge.PkLoginForge;
import com.pumpkiiings.pklogin.forge.command.ForgeAbstractCommand;
import com.pumpkiiings.pklogin.forge.listener.ForgeAuthenticateListener;
import com.pumpkiiings.pklogin.forge.serializer.chat.ChatComponentSerializer;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TwoFactorCommand extends ForgeAbstractCommand {

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final CodeVerifier verifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());
    
    // Store secrets temporarily while they are being set up
    private final Map<String, String> pendingSecrets = new HashMap<>();

    public TwoFactorCommand() {
        super("2fa");
    }

    @Override
    protected void perform(ServerPlayer player, String label, String[] args) {
        String name = player.getGameProfile().getName();
        LoginManagement loginManagement = PkLoginForge.getInstance().getLoginManagement();
        AccountManagement accountManagement = PkLoginForge.getInstance().getAccountManagement();

        if (args.length < 1) {
            player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_USAGE.asString()));
            return;
        }

        String subCommand = args[0].toLowerCase();
        Optional<Account> accountOpt = accountManagement.retrieveOrLoad(name);
        
        if (!accountOpt.isPresent()) {
            player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_NOT_REGISTERED.asString()));
            return;
        }
        
        Account account = accountOpt.get();

        if (subCommand.equals("setup")) {
            if (!loginManagement.isAuthenticated(name)) {
                player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_NOT_LOGGED_IN_SETUP.asString()));
                return;
            }
            if (account.getTotpSecret() != null) {
                player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_ALREADY_SETUP.asString()));
                return;
            }
            
            String secret = pendingSecrets.computeIfAbsent(name.toLowerCase(), k -> secretGenerator.generate());
            player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_SETUP_HEADER.asString()));
            player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_SETUP_SECRET.asString().replace("{0}", secret)));
            player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_SETUP_INSTRUCTION1.asString()));
            player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_SETUP_INSTRUCTION2.asString()));
            
        } else if (subCommand.equals("verify")) {
            if (args.length < 2) {
                player.sendSystemMessage(ChatComponentSerializer.fromText("§eUsage: /2fa verify <code>"));
                return;
            }
            String code = args[1];
            
            if (loginManagement.isAwaiting2FA(name)) {
                // Verify to complete login
                if (verifier.isValidCode(account.getTotpSecret(), code)) {
                    loginManagement.removeAwaiting2FA(name);
                    loginManagement.setAuthenticated(name);
                    ForgeAuthenticateListener.onAuthenticate(player, false);
                } else {
                    player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_INVALID_CODE.asString()));
                }
            } else if (pendingSecrets.containsKey(name.toLowerCase())) {
                // Verify to complete setup
                String secret = pendingSecrets.get(name.toLowerCase());
                if (verifier.isValidCode(secret, code)) {
                    accountManagement.updateTotpSecret(name, secret);
                    accountManagement.invalidateCache(name);
                    pendingSecrets.remove(name.toLowerCase());
                    player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_SETUP_SUCCESS.asString()));
                } else {
                    player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_INVALID_CODE.asString()));
                }
            } else {
                player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_NOT_AWAITING.asString()));
            }
            
        } else if (subCommand.equals("disable")) {
            if (!loginManagement.isAuthenticated(name)) {
                player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_NOT_LOGGED_IN_SETUP.asString()));
                return;
            }
            if (account.getTotpSecret() == null) {
                player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_NOT_SETUP.asString()));
                return;
            }
            
            if (args.length < 2) {
                player.sendSystemMessage(ChatComponentSerializer.fromText("§eUsage: /2fa disable <code>"));
                return;
            }
            String code = args[1];
            if (verifier.isValidCode(account.getTotpSecret(), code)) {
                accountManagement.updateTotpSecret(name, null);
                accountManagement.invalidateCache(name);
                player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_DISABLE_SUCCESS.asString()));
            } else {
                player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_INVALID_CODE.asString()));
            }
            
        } else if (subCommand.equals("discord")) {
            if (!loginManagement.isAuthenticated(name)) {
                player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_DISCORD_NOT_LOGGED_IN.asString()));
                return;
            }
            if (account.getDiscordId() != null) {
                player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_DISCORD_ALREADY_LINKED.asString()));
                return;
            }
            
            String code = com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorManager.getInstance().generateLinkCode(name, "DISCORD");
            player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_DISCORD_LINK_HEADER.asString()));
            player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_DISCORD_LINK_CODE.asString().replace("{0}", code)));
            player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_DISCORD_LINK_INSTRUCTION1.asString()));
            player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_DISCORD_LINK_INSTRUCTION2.asString()));
            
        } else if (subCommand.equals("verify2fa")) {
            if (args.length < 2) {
                player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_USAGE.asString()));
                return;
            }
            String code = args[1];
            if (com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorManager.getInstance().verifyLoginCode(name, code)) {
                loginManagement.removeAwaiting2FA(name);
                loginManagement.setAuthenticated(name);
                ForgeAuthenticateListener.onAuthenticate(player, false);
                player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_LOGIN_SUCCESS.asString()));
            } else {
                player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_INVALID_CODE.asString()));
            }
        } else {
            player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.TWO_FACTOR_USAGE.asString()));
        }
    }
}
