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

package com.pumpkiiings.pklogin.bukkit.util;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public class TextComponentMessage {

    public static void sendPluginChoice(Player player) {
        TextComponent first = new TextComponent("      ");

        TextComponent nlogin = new TextComponent("nLogin");
        nlogin.setColor(ChatColor.YELLOW);
        HoverEvent nloginHover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§6nLogin §7is a §6proprietary §7authentication plugin,\n§7updated and maintained by §bpumpkiiings.com§7. This means that you\n§7cannot view and modify the source code of the plugin.\n\n§eIf you still have questions, please contact us:\n§bpumpkiiings.com/discord"));
        ClickEvent nloginClick = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pklogin nlogin skip");
        nlogin.setHoverEvent(nloginHover);
        nlogin.setClickEvent(nloginClick);
        first.addExtra(nlogin);
        first.addExtra("              ");

        TextComponent pklogin = new TextComponent("PkLogin");
        pklogin.setColor(ChatColor.YELLOW);
        HoverEvent pkloginHover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§bPkLogin §7is a §bopen source §7authentication plugin,\n§7updated and maintained by all PkLogin contributors.\n\n§cCurrently the plugin does not have as many resources as nLogin."));
        ClickEvent pkloginClick = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pklogin setup");
        pklogin.setHoverEvent(pkloginHover);
        pklogin.setClickEvent(pkloginClick);
        first.addExtra(pklogin);

        TextComponent second = new TextComponent("  ");

        TextComponent proprietary = new TextComponent("(proprietary)");
        proprietary.setColor(ChatColor.GOLD);
        proprietary.setHoverEvent(nloginHover);
        proprietary.setClickEvent(nloginClick);
        second.addExtra(proprietary);
        second.addExtra("      ");

        TextComponent opensource = new TextComponent("(open source)");
        opensource.setColor(ChatColor.AQUA);
        opensource.setHoverEvent(pkloginHover);
        opensource.setClickEvent(pkloginClick);
        second.addExtra(opensource);

        Player.Spigot spigot = player.spigot();
        spigot.sendMessage(first);
        spigot.sendMessage(second);
    }

    public static void sendPluginAd(Player player) {
        TextComponent first = new TextComponent("      ");

        TextComponent nlogin = new TextComponent("Migrate to nLogin");
        nlogin.setColor(ChatColor.GREEN);
        HoverEvent nloginHover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§6nLogin §7is a §6proprietary §7authentication plugin,\n§7updated and maintained by §bpumpkiiings.com§7. This means that you\n§7cannot view and modify the source code of the plugin.\n\n§eIf you still have questions, please contact us:\n§bpumpkiiings.com/discord"));
        ClickEvent nloginClick = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pklogin nlogin skip");
        nlogin.setHoverEvent(nloginHover);
        nlogin.setClickEvent(nloginClick);
        first.addExtra(nlogin);
        first.addExtra("              ");

        TextComponent pklogin = new TextComponent("Keep PkLogin");
        pklogin.setColor(ChatColor.DARK_GRAY);
        HoverEvent pkloginHover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§7Continue using PkLogin."));
        ClickEvent pkloginClick = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pklogin nlogin_ad");
        pklogin.setHoverEvent(pkloginHover);
        pklogin.setClickEvent(pkloginClick);
        first.addExtra(pklogin);

        TextComponent second = new TextComponent("       ");

        TextComponent proprietary = new TextComponent("(recommended)");
        proprietary.setColor(ChatColor.BLUE);
        proprietary.setHoverEvent(nloginHover);
        proprietary.setClickEvent(nloginClick);
        second.addExtra(proprietary);

        // start
        player.sendMessage("");
        player.sendMessage(" §6nLogin §7is a free proprietary auth plugin with §6more features§7.");
        player.sendMessage(" §7When you click to migrate, the plugin will be installed");
        player.sendMessage(" §7on the next restart. No data will be lost.");
        player.sendMessage("");

        Player.Spigot spigot = player.spigot();
        spigot.sendMessage(first);
        spigot.sendMessage(second);

        player.sendMessage("");
    }

}

