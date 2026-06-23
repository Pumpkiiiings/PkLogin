/*
 * The MIT License (MIT)
 *
 * Copyright Â© 2020 - 2026 - PkLogin Contributors
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
import com.pumpkiiings.pklogin.bukkit.api.events.AsyncAuthenticateEvent;
import com.pumpkiiings.pklogin.bukkit.util.TextComponentMessage;
import com.pumpkiiings.pklogin.common.util.ClassUtils;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@AllArgsConstructor
public class PlayerAuthenticateListener implements Listener {

    private final PkLoginBukkit plugin;

    @EventHandler
    public void onAsyncAuthenticate(AsyncAuthenticateEvent e) {
        Player player = e.getPlayer();
        
        java.util.Optional<com.pumpkiiings.pklogin.common.model.Account> accountOpt = plugin.getAccountManagement().retrieveOrLoad(player.getName());
        if (accountOpt.isPresent() && accountOpt.get().getUuidType() != null && accountOpt.get().getUuidType().equals("REAL")) {
            com.pumpkiiings.pklogin.common.skin.SkinFetcher.SkinData data = com.pumpkiiings.pklogin.common.skin.SkinFetcher.fetchSkin(player.getUniqueId());
            if (data != null) {
                com.pumpkiiings.pklogin.bukkit.skin.BukkitSkinApplier.applySkin(player, data, plugin);
            }
        }

        // Send PluginMessage to Bungee/Velocity to notify auth success
        try {
            java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream out = new java.io.DataOutputStream(b);
            out.writeUTF("Authenticated");
            player.sendPluginMessage(plugin, "pklogin:main", b.toByteArray());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (player.hasPermission("pklogin.admin")) {
            if (plugin.isUpdateAvailable()) {
                player.sendMessage("");
                player.sendMessage(" §7A new version of §aPkLogin §7is available §a(v" + plugin.getDescription().getVersion() + " -> " + plugin.getLatestVersion() + ")§7.");
                player.sendMessage(" §7Use the command §f'/pklogin update' §7to download new version.");
                player.sendMessage("");
            }
        }
    }
}

