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

package com.pumpkiiings.pklogin.paper.listener;

import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.settings.Messages;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.Optional;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class PlayerKickListeners implements Listener {

    private final PkLoginPaper plugin;

    /** Compiled from config and cached, since the pattern only changes on reload. */
    private static volatile String cachedRegex;
    private static volatile Pattern cachedPattern;

    private static Pattern validNamePattern() {
        String regex = com.pumpkiiings.pklogin.common.settings.Settings.VALID_NAME_REGEX.asString();
        Pattern pattern = cachedPattern;
        if (pattern == null || !regex.equals(cachedRegex)) {
            try {
                pattern = Pattern.compile(regex);
            } catch (java.util.regex.PatternSyntaxException e) {
                // A broken pattern must not lock every player out of the server.
                pattern = Pattern.compile((String) com.pumpkiiings.pklogin.common.settings.Settings
                        .VALID_NAME_REGEX.getDef());
            }
            cachedRegex = regex;
            cachedPattern = pattern;
        }
        return pattern;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(AsyncPlayerPreLoginEvent e) {
        String name = e.getName();

        // Validate what the player actually typed. The appender may add characters
        // (the default offline suffix is "+") that the name pattern does not allow, which
        // would otherwise kick every offline player the moment it is enabled.
        if (!validNamePattern().matcher(name).matches()) {
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Messages.INVALID_NICKNAME.asString("§cSorry, but you are using an invalid nickname."));
            return;
        }

        if (com.pumpkiiings.pklogin.common.settings.Settings.APPENDER_ENABLED.asBoolean()) {
            Optional<Account> accountOpt = plugin.getAccountManagement().search(name);
            boolean isPremium = false;
            if (accountOpt.isPresent() && ("REAL".equalsIgnoreCase(accountOpt.get().getUuidType()) || "PREMIUM".equalsIgnoreCase(accountOpt.get().getUuidType()))) {
                isPremium = true;
            }

            String newName = name;
            if (isPremium) {
                String appendix = com.pumpkiiings.pklogin.common.settings.Settings.APPENDER_PREMIUM_APPENDIX.asString();
                String pos = com.pumpkiiings.pklogin.common.settings.Settings.APPENDER_PREMIUM_POSITION.asString();
                if ("PREFIX".equalsIgnoreCase(pos)) {
                    newName = appendix + newName;
                } else {
                    newName = newName + appendix;
                }
            } else {
                String appendix = com.pumpkiiings.pklogin.common.settings.Settings.APPENDER_OFFLINE_APPENDIX.asString();
                String pos = com.pumpkiiings.pklogin.common.settings.Settings.APPENDER_OFFLINE_POSITION.asString();
                if ("PREFIX".equalsIgnoreCase(pos)) {
                    newName = appendix + newName;
                } else {
                    newName = newName + appendix;
                }
            }

            if (!newName.equals(name)) {
                if (newName.length() > 16) {
                    newName = newName.substring(0, 16);
                }
                e.getPlayerProfile().setName(newName);
                name = newName;
            }
        }
        
        Player player = Bukkit.getPlayerExact(name);

        // prevent double online nickname
        if (player != null) {
            if (com.pumpkiiings.pklogin.common.settings.Settings.BYPASS_ONLINE_CHECK_WITH_SAME_ADDRESS.asBoolean()) {
                String existingIp = player.getAddress().getAddress().getHostAddress();
                String newIp = e.getAddress().getHostAddress();
                if (existingIp.equals(newIp)) {
                    player.getScheduler().run(plugin, task -> player.kick(
                            net.kyori.adventure.text.Component.text("Reconnecting...")), null);
                    return;
                }
            }
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Messages.ALREADY_ONLINE.asString());
            return;
        }

        Optional<Account> accountOpt = plugin.getAccountManagement().retrieveOrLoad(name);
        if (accountOpt.isPresent()) {
            Account account = accountOpt.get();
            String realname = account.getRealName();
            if (!name.equals(realname)) {
                String kickMessage = Messages.NICK_ALREADY_REGISTERED.asString()
                        .replace("{0}", name)
                        .replace("{1}", realname);
                e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, kickMessage);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent e) {
        Player player = Bukkit.getPlayer(e.getPlayer().getName());

        // prevent double online nickname
        if (player != null) {
            if (com.pumpkiiings.pklogin.common.settings.Settings.BYPASS_ONLINE_CHECK_WITH_SAME_ADDRESS.asBoolean()) {
                String existingIp = player.getAddress().getAddress().getHostAddress();
                String newIp = e.getAddress().getHostAddress();
                if (existingIp.equals(newIp)) {
                    // Already kicked in AsyncPreLogin, just return
                    return;
                }
            }
            e.disallow(PlayerLoginEvent.Result.KICK_OTHER, Messages.ALREADY_ONLINE.asString());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerKick(PlayerKickEvent e) {
        // Matching the kick text only worked on English servers, and broke as soon
        // as the message was translated or reworded. The cause is locale-independent.
        if (e.getCause() == PlayerKickEvent.Cause.DUPLICATE_LOGIN) {
            e.setCancelled(true);
        }
    }

}

