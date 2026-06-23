package com.pumpkiiings.pklogin.forge.command.executors;

import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.manager.LoginManagement;
import com.pumpkiiings.pklogin.common.model.Account;
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
            player.sendSystemMessage(ChatComponentSerializer.fromText("§eUsage: /2fa <setup|verify|disable>"));
            return;
        }

        String subCommand = args[0].toLowerCase();
        Optional<Account> accountOpt = accountManagement.retrieveOrLoad(name);
        
        if (!accountOpt.isPresent()) {
            player.sendSystemMessage(ChatComponentSerializer.fromText("§cYou must be registered to use 2FA."));
            return;
        }
        
        Account account = accountOpt.get();

        if (subCommand.equals("setup")) {
            if (!loginManagement.isAuthenticated(name)) {
                player.sendSystemMessage(ChatComponentSerializer.fromText("§cYou must be logged in to set up 2FA."));
                return;
            }
            if (account.getTotpSecret() != null) {
                player.sendSystemMessage(ChatComponentSerializer.fromText("§c2FA is already set up on your account."));
                return;
            }
            
            String secret = pendingSecrets.computeIfAbsent(name.toLowerCase(), k -> secretGenerator.generate());
            player.sendSystemMessage(ChatComponentSerializer.fromText("§e--- 2FA Setup ---"));
            player.sendSystemMessage(ChatComponentSerializer.fromText("§eSecret Key: §a" + secret));
            player.sendSystemMessage(ChatComponentSerializer.fromText("§ePlease enter this secret into your authenticator app."));
            player.sendSystemMessage(ChatComponentSerializer.fromText("§eThen, verify it using: §a/2fa verify <code>"));
            
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
                    player.sendSystemMessage(ChatComponentSerializer.fromText("§cInvalid 2FA code."));
                }
            } else if (pendingSecrets.containsKey(name.toLowerCase())) {
                // Verify to complete setup
                String secret = pendingSecrets.get(name.toLowerCase());
                if (verifier.isValidCode(secret, code)) {
                    accountManagement.updateTotpSecret(name, secret);
                    accountManagement.invalidateCache(name);
                    pendingSecrets.remove(name.toLowerCase());
                    player.sendSystemMessage(ChatComponentSerializer.fromText("§a2FA has been successfully set up!"));
                } else {
                    player.sendSystemMessage(ChatComponentSerializer.fromText("§cInvalid 2FA code. Please try again."));
                }
            } else {
                player.sendSystemMessage(ChatComponentSerializer.fromText("§cYou are not awaiting 2FA verification."));
            }
            
        } else if (subCommand.equals("disable")) {
            if (!loginManagement.isAuthenticated(name)) {
                player.sendSystemMessage(ChatComponentSerializer.fromText("§cYou must be logged in to disable 2FA."));
                return;
            }
            if (account.getTotpSecret() == null) {
                player.sendSystemMessage(ChatComponentSerializer.fromText("§cYou do not have 2FA set up."));
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
                player.sendSystemMessage(ChatComponentSerializer.fromText("§a2FA has been successfully disabled!"));
            } else {
                player.sendSystemMessage(ChatComponentSerializer.fromText("§cInvalid 2FA code."));
            }
            
        } else if (subCommand.equals("discord")) {
            if (!loginManagement.isAuthenticated(name)) {
                player.sendSystemMessage(ChatComponentSerializer.fromText("§cDebes estar logeado para vincular Discord."));
                return;
            }
            if (account.getDiscordId() != null) {
                player.sendSystemMessage(ChatComponentSerializer.fromText("§cYa tienes una cuenta de Discord vinculada."));
                return;
            }
            
            String code = com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorManager.getInstance().generateLinkCode(name, "DISCORD");
            player.sendSystemMessage(ChatComponentSerializer.fromText("§e--- Vinculación de Discord ---"));
            player.sendSystemMessage(ChatComponentSerializer.fromText("§eCódigo: §a" + code));
            player.sendSystemMessage(ChatComponentSerializer.fromText("§ePor favor, envía un Mensaje Privado (DM) a nuestro Bot de Discord con este código."));
            player.sendSystemMessage(ChatComponentSerializer.fromText("§eEl código expirará en 10 minutos."));
            
        } else if (subCommand.equals("verify2fa")) {
            if (args.length < 2) {
                player.sendSystemMessage(ChatComponentSerializer.fromText("§eUsage: /2fa verify2fa <code>"));
                return;
            }
            String code = args[1];
            if (com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorManager.getInstance().verifyLoginCode(name, code)) {
                loginManagement.removeAwaiting2FA(name);
                loginManagement.setAuthenticated(name);
                ForgeAuthenticateListener.onAuthenticate(player, false);
                player.sendSystemMessage(ChatComponentSerializer.fromText("§aHas iniciado sesión correctamente."));
            } else {
                player.sendSystemMessage(ChatComponentSerializer.fromText("§cCódigo 2FA incorrecto."));
            }
        } else {
            player.sendSystemMessage(ChatComponentSerializer.fromText("§eUsage: /2fa <setup|verify|disable|discord|verify2fa>"));
        }
    }
}
