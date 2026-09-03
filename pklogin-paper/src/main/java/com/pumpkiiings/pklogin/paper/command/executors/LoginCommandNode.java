package com.pumpkiiings.pklogin.paper.command.executors;

import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import com.pumpkiiings.pklogin.api.event.bukkit.AsyncAuthenticateEvent;
import com.pumpkiiings.pklogin.api.event.bukkit.AsyncLoginEvent;
import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.manager.LoginManagement;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.settings.Messages;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class LoginCommandNode {

    public static LiteralCommandNode<CommandSourceStack> build(PkLoginPaper plugin) {
        return Commands.literal("login")
            .requires(source -> source.getSender() instanceof Player)
            .executes(context -> {
                context.getSource().getSender().sendMessage(Messages.MESSAGE_LOGIN.asString());
                return 1;
            })
            .then(Commands.argument("password", StringArgumentType.string())
                .executes(context -> {
                    CommandSender sender = context.getSource().getSender();
                    if (!(sender instanceof Player)) return 1;

                    Player player = (Player) sender;
                    String name = player.getName();
                    String password = context.getArgument("password", String.class);
                    LoginManagement loginManagement = plugin.getLoginManagement();

                    if (!loginManagement.isUnlocked(name)) return 1;

                    plugin.runAsync(() -> {
                        if (plugin.isAuthenticated(player)) {
                            player.sendMessage(Messages.ALREADY_LOGIN.asString());
                            return;
                        }

                        if (loginManagement.mustChangePassword(name)) {
                            player.sendMessage(Messages.CHANGE_PASSWORD_ENFORCED.asString());
                            return;
                        }

                        AccountManagement accountManagement = plugin.getAccountManagement();
                        Optional<Account> accountOpt = accountManagement.retrieveOrLoad(name);
                        if (!accountOpt.isPresent()) {
                            player.sendMessage(Messages.NOT_REGISTERED.asString());
                            return;
                        }

                        Account account = accountOpt.get();

                        if (!accountManagement.comparePassword(account, password)) {
                            int tries = loginManagement.incrementFailedAttempts(name);
                            int maxTries = com.pumpkiiings.pklogin.common.settings.Settings.BRUTEFORCE_MAX_LOGIN_TRIES.asInt();
                            if (tries >= maxTries) {
                                player.getScheduler().run(plugin, task -> player.kick(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(Messages.DELAY_KICK_LOGIN.asString("§cExceeded maximum login attempts."))), null);
                            } else {
                                player.sendMessage(Messages.INCORRECT_PASSWORD.asString());
                                player.sendMessage(Messages.ATTEMPTS_LEFT
                                        .asString("§cAttempts left: §e{0}")
                                        .replace("{0}", String.valueOf(maxTries - tries)));
                            }
                            return;
                        }

                        loginManagement.resetFailedAttempts(name);

                        // The plain-text password is only available here, so this is the
                        // one place a legacy or weaker hash can be silently upgraded.
                        accountManagement.rehashIfNeeded(account, password);

                        com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorManager twoFactor =
                                com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorManager.getInstance();
                        com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorProvider discordProvider =
                                twoFactor.getDiscordProvider();

                        // A linked Discord account only gates the login while the provider is
                        // actually usable; if the admin turned it off or JDA is absent, the
                        // player must not be left stuck awaiting a code that never arrives.
                        boolean discordRequired = account.getDiscordId() != null
                                && discordProvider != null
                                && discordProvider.isEnabled();

                        if (discordRequired) {
                            String loginCode = twoFactor.generateLoginCode(name);
                            if (discordProvider.sendVerificationCode(account, loginCode)) {
                                loginManagement.setAwaiting2FA(name);
                                player.sendMessage(Messages.TWO_FACTOR_LOGIN_MESSAGE.asString());
                            } else {
                                twoFactor.removeLoginCode(name);
                                player.sendMessage(Messages.TWO_FACTOR_SEND_FAILED
                                        .asString("§cCould not deliver your 2FA code. Contact an administrator."));
                            }
                        } else {
                            AsyncLoginEvent loginEvent = new AsyncLoginEvent(player);
                            if (loginEvent.callEvt()) {
                                if (com.pumpkiiings.pklogin.common.settings.Settings.SECURE_PASSWORDS_ENFORCE.asBoolean() && !password.matches(com.pumpkiiings.pklogin.common.settings.Settings.SECURE_PASSWORDS_REGEX.asString())) {
                                    loginManagement.setMustChangePassword(name);
                                    player.sendMessage(Messages.CHANGE_PASSWORD_ENFORCED.asString());
                                } else {
                                    if (!plugin.authenticate(player)) return;
                                    player.sendMessage(Messages.SUCCESSFUL_LOGIN.asString());
                                }
                                com.pumpkiiings.pklogin.paper.util.AdventureAPI.showTitle(player, Messages.TITLE_AFTER_LOGIN.asTitle().title, Messages.TITLE_AFTER_LOGIN.asTitle().subtitle, Messages.TITLE_AFTER_LOGIN.asTitle().start, Messages.TITLE_AFTER_LOGIN.asTitle().duration, Messages.TITLE_AFTER_LOGIN.asTitle().end);

                                player.getScheduler().run(plugin, task ->
                                        com.pumpkiiings.pklogin.paper.manager.LimboManager.leaveLimbo(plugin, player), null);

                                new AsyncAuthenticateEvent(player).callEvt();
                            }
                        }
                    });
                    return 1;
                })
            ).build();
    }
}
