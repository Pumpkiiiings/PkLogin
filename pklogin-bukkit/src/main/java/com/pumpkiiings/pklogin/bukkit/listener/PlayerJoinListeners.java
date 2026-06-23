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

package com.pumpkiiings.pklogin.bukkit.listener;

import com.pumpkiiings.pklogin.bukkit.PkLoginBukkit;
import com.pumpkiiings.pklogin.bukkit.task.LoginQueue;
import com.pumpkiiings.pklogin.bukkit.ui.title.TitleAPI;
import com.pumpkiiings.pklogin.bukkit.util.TextComponentMessage;
import com.pumpkiiings.pklogin.common.model.Title;
import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.common.util.ClassUtils;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@AllArgsConstructor
public class PlayerJoinListeners implements Listener {

    private final PkLoginBukkit plugin;

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        String name = player.getName();



        boolean registered = plugin.getAccountManagement().retrieveOrLoad(name).isPresent();

        String ip = player.getAddress().getAddress().getHostAddress();
        com.pumpkiiings.pklogin.bukkit.protocollib.AutoLoginSession session = plugin.getVerifiedSessions().remove(ip);

        if (session != null && session.isVerified() && session.getUsername().equalsIgnoreCase(name)) {
            plugin.getLoginManagement().setAuthenticated(name);
            player.sendMessage(Messages.PREMIUM_AUTO_LOGIN.asString());
            if (com.pumpkiiings.pklogin.common.settings.Settings.UI_TITLE_BAR.asBoolean()) {
                TitleAPI.getApi().send(player, Messages.TITLE_PREMIUM_AUTO_LOGIN.asTitle());
            }
            plugin.getFoliaLib().runAsync(task -> new com.pumpkiiings.pklogin.bukkit.api.events.AsyncAuthenticateEvent(player).callEvt());
            return;
        }

        if (com.pumpkiiings.pklogin.common.hook.FloodgateHook.isBedrockPlayer(player.getUniqueId())) {
            plugin.getLoginManagement().setAuthenticated(name);
            player.sendMessage(Messages.PREMIUM_AUTO_LOGIN.asString().replace("Premium", "Bedrock"));
            if (com.pumpkiiings.pklogin.common.settings.Settings.UI_TITLE_BAR.asBoolean()) {
                TitleAPI.getApi().send(player, Messages.TITLE_BEDROCK_AUTO_LOGIN.asTitle());
            }
            plugin.getFoliaLib().runAsync(task -> new com.pumpkiiings.pklogin.bukkit.api.events.AsyncAuthenticateEvent(player).callEvt());
            return;
        }

        LoginQueue.addToQueue(name, registered);

        player.setWalkSpeed(0F);
        player.setFlySpeed(0F);

        if (com.pumpkiiings.pklogin.common.settings.Settings.TELEPORT_SAFE_LOCATION.asBoolean()) {
            com.pumpkiiings.pklogin.bukkit.manager.BukkitLimboManager.teleportToSpawn(player);
        }
        com.pumpkiiings.pklogin.bukkit.manager.BukkitLimboManager.applyLimboState(player);

        if (registered) {
            player.sendMessage(Messages.MESSAGE_LOGIN.asString());
            if (com.pumpkiiings.pklogin.common.settings.Settings.UI_TITLE_BAR.asBoolean()) {
                TitleAPI.getApi().send(player, Messages.TITLE_BEFORE_LOGIN.asTitle());
            }
        } else {
            player.sendMessage(Messages.MESSAGE_REGISTER.asString());
            if (com.pumpkiiings.pklogin.common.settings.Settings.UI_TITLE_BAR.asBoolean()) {
                TitleAPI.getApi().send(player, Messages.TITLE_BEFORE_REGISTER.asTitle());
            }
        }
    }
}

