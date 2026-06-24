/*
 * The MIT License (MIT)
 *
 * Copyright © 2020 - 2026 - PkLogin Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.pumpkiiings.pklogin.paper.command.executors;

import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import com.pumpkiiings.pklogin.api.event.bukkit.AsyncAuthenticateEvent;
import com.pumpkiiings.pklogin.api.event.bukkit.AsyncLoginEvent;
import com.pumpkiiings.pklogin.paper.command.BukkitAbstractCommand;

import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.manager.LoginManagement;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.settings.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class LoginCommand extends BukkitAbstractCommand {

    public LoginCommand(PkLoginPaper plugin) {
        super(plugin, "login");
    }

    protected void perform(CommandSender sender, String lb, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PLAYER_COMMAND_USAGE.asString());
            return;
        }

        String name = sender.getName();
        LoginManagement loginManagement = plugin.getLoginManagement();
        if (loginManagement.isAuthenticated(name)) {
            sender.sendMessage(Messages.ALREADY_LOGIN.asString());
            return;
        }

        if (loginManagement.mustChangePassword(name)) {
            sender.sendMessage(Messages.CHANGE_PASSWORD_ENFORCED.asString());
            return;
        }

        if (args.length != 1) {
            sender.sendMessage(Messages.MESSAGE_LOGIN.asString());
            return;
        }

        AccountManagement accountManagement = plugin.getAccountManagement();
        Optional<Account> accountOpt = accountManagement.retrieveOrLoad(name);
        if (!accountOpt.isPresent()) {
            sender.sendMessage(Messages.NOT_REGISTERED.asString());
            return;
        }

        Account account = accountOpt.get();
        String password = args[0];

        Player player = (Player) sender;
        if (!accountManagement.comparePassword(account, password)) {
            int tries = loginManagement.incrementFailedAttempts(name);
            int maxTries = com.pumpkiiings.pklogin.common.settings.Settings.BRUTEFORCE_MAX_LOGIN_TRIES.asInt();
            if (tries >= maxTries) {
                player.getScheduler().run(plugin, task -> player.kick(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(Messages.DELAY_KICK_LOGIN.asString("§cExceeded maximum login attempts."))), null);
            } else {
                sender.sendMessage(Messages.INCORRECT_PASSWORD.asString() + " Attempts left: " + (maxTries - tries));
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
                    // Do not call setAuthenticated yet
                } else {
                    plugin.getLoginManagement().setAuthenticated(name);
                    player.sendMessage(Messages.SUCCESSFUL_LOGIN.asString());
                }
                com.pumpkiiings.pklogin.paper.util.AdventureAPI.showTitle(player, Messages.TITLE_AFTER_LOGIN.asTitle().title, Messages.TITLE_AFTER_LOGIN.asTitle().subtitle, Messages.TITLE_AFTER_LOGIN.asTitle().start, Messages.TITLE_AFTER_LOGIN.asTitle().duration, Messages.TITLE_AFTER_LOGIN.asTitle().end);

                player.getScheduler().run(plugin, task -> {
                    player.setWalkSpeed(0.2F);
                    player.setFlySpeed(0.1F);
                    com.pumpkiiings.pklogin.paper.manager.BukkitLimboManager.removeLimboState(plugin, player);
                    com.pumpkiiings.pklogin.paper.manager.BukkitLimboManager.restoreLastLocation(player);
                }, null);

                new AsyncAuthenticateEvent(player).callEvt();
            }
        }
    }
}

