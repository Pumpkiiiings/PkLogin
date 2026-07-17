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
                        if (loginManagement.isAuthenticated(name)) {
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
                                player.sendMessage(Messages.INCORRECT_PASSWORD.asString() + " Attempts left: " + (maxTries - tries));
                            }
                            return;
                        }

                        loginManagement.resetFailedAttempts(name);

                        if (account.getDiscordId() != null) {
                            loginManagement.setAwaiting2FA(name);
                            String loginCode = com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorManager.getInstance().generateLoginCode(name);
                            com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorProvider discordProvider = com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorManager.getInstance().getDiscordProvider();
                            discordProvider.sendVerificationCode(account, loginCode);
                            player.sendMessage("§eTe hemos enviado un código a Discord. Escribe /2fa verify2fa <código> para entrar.");
                        } else {
                            AsyncLoginEvent loginEvent = new AsyncLoginEvent(player);
                            if (loginEvent.callEvt()) {
                                if (com.pumpkiiings.pklogin.common.settings.Settings.SECURE_PASSWORDS_ENFORCE.asBoolean() && !password.matches(com.pumpkiiings.pklogin.common.settings.Settings.SECURE_PASSWORDS_REGEX.asString())) {
                                    loginManagement.setMustChangePassword(name);
                                    player.sendMessage(Messages.CHANGE_PASSWORD_ENFORCED.asString());
                                } else {
                                    plugin.getLoginManagement().setAuthenticated(name);
                                    player.sendMessage(Messages.SUCCESSFUL_LOGIN.asString());
                                }
                                com.pumpkiiings.pklogin.paper.util.AdventureAPI.showTitle(player, Messages.TITLE_AFTER_LOGIN.asTitle().title, Messages.TITLE_AFTER_LOGIN.asTitle().subtitle, Messages.TITLE_AFTER_LOGIN.asTitle().start, Messages.TITLE_AFTER_LOGIN.asTitle().duration, Messages.TITLE_AFTER_LOGIN.asTitle().end);

                                player.getScheduler().run(plugin, task -> {
                                    player.setWalkSpeed(0.2F);
                                    player.setFlySpeed(0.1F);
                                    com.pumpkiiings.pklogin.paper.manager.LimboManager.removeLimboState(plugin, player);
                                    com.pumpkiiings.pklogin.paper.manager.LimboManager.restoreLastLocation(player);
                                }, null);

                                new AsyncAuthenticateEvent(player).callEvt();
                            }
                        }
                    });
                    return 1;
                })
            ).build();
    }
}
