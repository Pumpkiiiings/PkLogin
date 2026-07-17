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
import com.pumpkiiings.pklogin.api.event.bukkit.AsyncRegisterEvent;
import com.pumpkiiings.pklogin.paper.command.BukkitAbstractCommand;

import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.manager.LoginManagement;
import com.pumpkiiings.pklogin.common.security.hashing.HashStrategyFactory;
import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.common.settings.Settings;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

public class RegisterCommand extends BukkitAbstractCommand {

    public RegisterCommand(PkLoginPaper plugin) {
        super(plugin, "register");
    }

    protected void perform(CommandSender sender, String lb, String[] args) {
        if (sender instanceof Player) {
            performPlayer((Player) sender, lb, args);
        } else {
            performConsole(sender, lb, args);
        }
    }

    private void performPlayer(Player sender, String lb, String[] args) {
        String name = sender.getName();
        LoginManagement loginManagement = plugin.getLoginManagement();
        if (loginManagement.isAuthenticated(name)) {
            sender.sendMessage(Messages.ALREADY_LOGIN.asString());
            return;
        }

        if (args.length != 2) {
            sender.sendMessage(Messages.MESSAGE_REGISTER.asString());
            return;
        }

        if (com.pumpkiiings.pklogin.common.settings.Settings.SECURITY_IP_LIMIT_ENABLE.asBoolean()) {
            int ipLimit = com.pumpkiiings.pklogin.common.settings.Settings.SECURITY_IP_LIMIT.asInt();
            String addressStr = sender.getAddress().getAddress().getHostAddress();
            if (plugin.getAccountManagement().countAccountsByIp(addressStr) >= ipLimit) {
                sender.sendMessage(Messages.MAX_IPS_REACHED.asString());
                return;
            }
        }

        String password = args[0];
        int passwordLength = password.length();

        if (passwordLength <= Settings.PASSWORD_SMALL.asInt()) {
            sender.sendMessage(Messages.PASSWORD_TOO_SMALL.asString());
            return;
        }

        if (passwordLength >= Settings.PASSWORD_LARGE.asInt()) {
            sender.sendMessage(Messages.PASSWORD_TOO_LARGE.asString());
            return;
        }

        if (!password.equals(args[1])) {
            sender.sendMessage(Messages.PASSWORDS_DONT_MATCH.asString());
            return;
        }

        if (com.pumpkiiings.pklogin.common.settings.Settings.SECURE_PASSWORDS_ENABLE.asBoolean()) {
            if (!password.matches(com.pumpkiiings.pklogin.common.settings.Settings.SECURE_PASSWORDS_REGEX.asString())) {
                sender.sendMessage(Messages.INSECURE_PASSWORD.asString());
                return;
            }
        }

        AccountManagement accountManagement = plugin.getAccountManagement();
        boolean exists = accountManagement.retrieveOrLoad(name).isPresent();
        if (exists) {
            sender.sendMessage(Messages.ALREADY_REGISTERED.asString());
            return;
        }

        String hashedPassword = HashStrategyFactory.fromSettings().hash(password);
        String address = sender.getAddress().getAddress().getHostAddress();
        if (!accountManagement.update(name, hashedPassword, address, false)) {
            sender.sendMessage(Messages.DATABASE_ERROR.asString());
            return;
        }

        AsyncRegisterEvent registerEvent = new AsyncRegisterEvent(sender);
        if (registerEvent.callEvt()) {
            plugin.getLoginManagement().setAuthenticated(name);

            com.pumpkiiings.pklogin.paper.util.AdventureAPI.showTitle(sender, Messages.TITLE_AFTER_REGISTER.asTitle().title, Messages.TITLE_AFTER_REGISTER.asTitle().subtitle, Messages.TITLE_AFTER_REGISTER.asTitle().start, Messages.TITLE_AFTER_REGISTER.asTitle().duration, Messages.TITLE_AFTER_REGISTER.asTitle().end);
            sender.sendMessage(Messages.SUCCESSFUL_REGISTER.asString());
            Player player = (Player) sender;
            player.getScheduler().run(plugin, task -> {
                player.setWalkSpeed(0.2F);
                player.setFlySpeed(0.1F);
                com.pumpkiiings.pklogin.paper.manager.LimboManager.removeLimboState(plugin, player);
                com.pumpkiiings.pklogin.paper.manager.LimboManager.restoreLastLocation(player);
            }, null);

            new AsyncAuthenticateEvent(sender).callEvt();
        }
    }

    private void performConsole(CommandSender sender, String lb, String[] args) {
        if (!sender.hasPermission("pklogin.admin")) {
            sender.sendMessage(Messages.INSUFFICIENT_PERMISSIONS.asString());
            return;
        }

        if (args.length != 2) {
            sender.sendMessage("§cUsage: /" + lb + " <player> <password>");
            return;
        }

        String playerName = args[0];
        String password = args[1];
        int passwordLength = password.length();

        if (passwordLength <= Settings.PASSWORD_SMALL.asInt()) {
            sender.sendMessage(Messages.PASSWORD_TOO_SMALL.asString());
            return;
        }

        if (passwordLength >= Settings.PASSWORD_LARGE.asInt()) {
            sender.sendMessage(Messages.PASSWORD_TOO_LARGE.asString());
            return;
        }

        Player playerIfOnline = plugin.getServer().getPlayerExact(playerName);
        if (playerIfOnline != null) {
            playerName = playerIfOnline.getName();
        }

        AccountManagement accountManagement = plugin.getAccountManagement();
        boolean exists = accountManagement.retrieveOrLoad(playerName).isPresent();
        if (exists) {
            sender.sendMessage(Messages.ALREADY_REGISTERED.asString());
            return;
        }

        String hashedPassword = HashStrategyFactory.fromSettings().hash(password);
        String address = playerIfOnline != null ?
                Objects.requireNonNull(playerIfOnline.getAddress()).getAddress().getHostAddress() : null;
        if (!accountManagement.update(playerName, hashedPassword, address, false)) {
            sender.sendMessage(Messages.DATABASE_ERROR.asString());
            return;
        }

        sender.sendMessage(Messages.SUCCESSFUL_REGISTER.asString());

        if (playerIfOnline != null) {
            AsyncRegisterEvent registerEvent = new AsyncRegisterEvent(playerIfOnline);
            if (registerEvent.callEvt()) {
                plugin.getLoginManagement().setAuthenticated(playerName);

                com.pumpkiiings.pklogin.paper.util.AdventureAPI.showTitle(playerIfOnline, Messages.TITLE_AFTER_REGISTER.asTitle().title, Messages.TITLE_AFTER_REGISTER.asTitle().subtitle, Messages.TITLE_AFTER_REGISTER.asTitle().start, Messages.TITLE_AFTER_REGISTER.asTitle().duration, Messages.TITLE_AFTER_REGISTER.asTitle().end);
                playerIfOnline.sendMessage(Messages.SUCCESSFUL_REGISTER.asString());

                playerIfOnline.getScheduler().run(plugin, task -> {
                    playerIfOnline.setWalkSpeed(0.2F);
                    playerIfOnline.setFlySpeed(0.1F);
                    com.pumpkiiings.pklogin.paper.manager.LimboManager.removeLimboState(plugin, playerIfOnline);
                    com.pumpkiiings.pklogin.paper.manager.LimboManager.restoreLastLocation(playerIfOnline);
                }, null);

                new AsyncAuthenticateEvent(playerIfOnline).callEvt();
            }
        }
    }
}

