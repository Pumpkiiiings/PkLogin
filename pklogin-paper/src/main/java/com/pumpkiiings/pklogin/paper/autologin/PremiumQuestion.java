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

package com.pumpkiiings.pklogin.paper.autologin;

import com.pumpkiiings.pklogin.common.manager.PremiumNameLookup;
import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.common.settings.Settings;
import com.pumpkiiings.pklogin.paper.util.AdventureAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.entity.Player;

/**
 * Offers a player who just registered a paid nickname the chance to claim it.
 *
 * <p>This is the counterpart to {@code autologin.premium.enable}. Reserving a
 * paid nickname up front costs an offline player the ability to join at all, and
 * with a message from their own launcher that says nothing about this server —
 * on a network where offline players routinely pick paid nicknames, that reads
 * as an outage. Asking after the fact reaches the same destination for a real
 * owner, a couple of clicks later, and costs a stranger nothing.</p>
 *
 * <p>Asked once, on the first registration of a name. A player who says no is
 * never asked again because there is no second first registration; nothing has
 * to be stored to remember the answer.</p>
 */
public final class PremiumQuestion {

    private PremiumQuestion() {}

    /**
     * Sends the question if this name is worth asking about.
     *
     * <p>Safe to call from an async task, and expects to be — it may perform the
     * Mojang lookup on the calling thread.</p>
     *
     * @param player the player who has just registered
     */
    public static void offerTo(Player player) {
        if (!Settings.AUTOLOGIN_PREMIUM_QUESTION.asBoolean()) {
            return;
        }

        // Only an outright yes. UNKNOWN means Mojang could not be reached, and
        // telling someone their nickname is paid when nobody checked would push
        // them into converting an account they cannot then log into.
        if (PremiumNameLookup.lookup(player.getName()) != PremiumNameLookup.Answer.PREMIUM) {
            return;
        }

        for (String line : Messages.PREMIUM_QUESTION.asList()) {
            player.sendMessage(AdventureAPI.parse(line));
        }

        // Defaults use the section sign directly: they never pass through
        // Messages.define, which is what turns '&' into '§' for file values.
        //
        // Yes goes to a second prompt rather than straight to /premium confirm. A
        // player who does not own the nickname and clicks through converts their
        // seconds-old account to REAL, which leaves it with no password and no way
        // to pass the Mojang handshake — unreachable until an admin deletes it. One
        // stray click should not be able to reach that state.
        Component yes = AdventureAPI.parse(Messages.PREMIUM_QUESTION_YES.asString("§a§l[ YES, IT IS MINE ]"))
                .clickEvent(ClickEvent.runCommand("/premium question-confirm"))
                .hoverEvent(HoverEvent.showText(AdventureAPI.parse(
                        Messages.PREMIUM_QUESTION_HOVER_YES.asString("§7Switch to premium and reconnect"))));

        Component no = AdventureAPI.parse(Messages.PREMIUM_QUESTION_NO.asString("§c§l[ NO ]"))
                .clickEvent(ClickEvent.runCommand("/premium no"))
                .hoverEvent(HoverEvent.showText(AdventureAPI.parse(
                        Messages.PREMIUM_QUESTION_HOVER_NO.asString("§7Keep using a password"))));

        player.sendMessage(Component.empty().append(yes).append(Component.text("  ")).append(no));
    }
}
