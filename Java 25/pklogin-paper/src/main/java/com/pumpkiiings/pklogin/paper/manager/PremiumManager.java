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

package com.pumpkiiings.pklogin.paper.manager;

import com.pumpkiiings.pklogin.common.manager.PremiumNameLookup;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.common.settings.Settings;
import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import com.pumpkiiings.pklogin.paper.task.LoginQueue;
import com.pumpkiiings.pklogin.paper.util.AdventureAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The decisions about premium accounts that are not protocol work: whether a
 * connection should be challenged, and the question a player is asked when they
 * arrive on a paid nickname the server has never seen.
 *
 * <p>Both come down to the same question asked at different moments — is this
 * name a paid account, and what should be done about it — so they answer it the
 * same way and share the rule that an unknown answer is never treated as a yes.
 * The packet layer only asks; nothing here writes or reads a packet.</p>
 */
public class PremiumManager {

    /** Question stages, so a stray click cannot skip the confirmation. */
    private enum Stage {
        ASKED,
        CONFIRMING
    }

    private static final class Pending {
        private Stage stage = Stage.ASKED;

        /**
         * Bumped whenever a new timer is armed, so the one it replaced does no
         * harm when it fires. Cheaper and less error-prone than holding a task
         * reference: the scheduler hands back different types on Paper and Folia,
         * and a cancel that quietly failed would decline a player mid-answer.
         */
        private int generation;
    }

    /**
     * Questions waiting on an answer, by lowercased name.
     *
     * <p>Its other job is authorisation: {@code /premium} is otherwise refused
     * before login, and these players have not logged in yet.</p>
     */
    private static final Map<String, Pending> PENDING = new ConcurrentHashMap<>();

    private PremiumManager() {}

    /**
     * Whether a joining player should be asked to prove they own a premium
     * account.
     *
     * <p>Saying yes starts Mojang's encryption handshake, and a client that
     * cannot complete it does not fall back to a password prompt — it cannot join
     * at all. Hence yes only on a real reason to believe the account is
     * premium.</p>
     *
     * @return true to send the encryption request
     */
    public static boolean shouldRequestEncryption(PkLoginPaper plugin, String username) {
        Optional<Account> accountOpt = plugin.getAccountManagement().retrieveOrLoad(username);
        if (accountOpt.isPresent()) {
            // A known player keeps whatever mode their account already says. This
            // is what stops a server's existing offline players from being locked
            // out the day passwordless premium logins are switched on.
            String type = accountOpt.get().getUuidType();
            return "REAL".equals(type) || "PREMIUM".equals(type);
        }

        if (!PremiumNameLookup.isEnabled()) {
            return false;
        }

        // Only an outright yes. An unreachable Mojang means the player registers a
        // password today and can convert later with /premium; guessing yes would
        // leave them unable to connect.
        return PremiumNameLookup.lookup(username) == PremiumNameLookup.Answer.PREMIUM;
    }

    /** True while this player has a question on screen. */
    public static boolean isAwaitingAnswer(String name) {
        return PENDING.containsKey(key(name));
    }

    /**
     * Asks a joining player whether the paid nickname they arrived on is theirs,
     * and reports whether the normal registration prompt should be held back.
     *
     * <p>Asked before registering rather than after, so a real owner never
     * creates a password they are about to stop using, and never loses anything
     * by converting: at this point the account does not exist yet.</p>
     *
     * <p>The Mojang lookup blocks, so this runs the check asynchronously and
     * calls {@code onDeclined} on the player's own thread when there is nothing
     * to ask or the player says no. The login queue is deliberately not started
     * until then — the seconds spent reading the question would otherwise come
     * out of the time to register.</p>
     */
    public static void askOnJoin(PkLoginPaper plugin, Player player, String ip, Runnable onDeclined) {
        if (!Settings.AUTOLOGIN_PREMIUM_QUESTION.asBoolean()) {
            onDeclined.run();
            return;
        }

        String name = player.getName();
        plugin.runAsync(() -> {
            PremiumNameLookup.Answer answer;
            try {
                answer = PremiumNameLookup.lookup(name);
            } catch (Throwable ex) {
                plugin.getLogger().warning("Premium question: lookup failed for " + name
                        + " (" + ex + "). Asking for a password instead.");
                answer = PremiumNameLookup.Answer.UNKNOWN;
            }

            // Only an outright yes. Telling someone their nickname is paid when
            // nobody checked would push them into claiming an account they cannot
            // then log into.
            if (answer != PremiumNameLookup.Answer.PREMIUM) {
                onPlayerThread(plugin, player, onDeclined);
                return;
            }

            onPlayerThread(plugin, player, () -> {
                if (!player.isOnline()) return;
                startQuestion(plugin, player, onDeclined);
            });
        });
    }

    private static void startQuestion(PkLoginPaper plugin, Player player, Runnable onDeclined) {
        Pending pending = new Pending();
        PENDING.put(key(player.getName()), pending);

        for (String line : Messages.PREMIUM_QUESTION.asList()) {
            player.sendMessage(AdventureAPI.parse(line));
        }

        // Defaults use the section sign directly: they never pass through
        // Messages.define, which is what turns '&' into '§' for file values.
        Component yes = AdventureAPI.parse(Messages.PREMIUM_QUESTION_YES.asString("§a§l[ YES, IT IS MINE ]"))
                .clickEvent(ClickEvent.runCommand("/premium question-confirm"))
                .hoverEvent(HoverEvent.showText(AdventureAPI.parse(
                        Messages.PREMIUM_QUESTION_HOVER_YES.asString("§7Switch to premium and reconnect"))));

        Component no = AdventureAPI.parse(Messages.PREMIUM_QUESTION_NO.asString("§c§l[ NO ]"))
                .clickEvent(ClickEvent.runCommand("/premium no"))
                .hoverEvent(HoverEvent.showText(AdventureAPI.parse(
                        Messages.PREMIUM_QUESTION_HOVER_NO.asString("§7Keep using a password"))));

        player.sendMessage(Component.empty().append(yes).append(Component.text("  ")).append(no));

        scheduleTimeout(plugin, player, pending, onDeclined);
    }

    /**
     * Second half of a yes.
     *
     * <p>A player who does not own the nickname and clicks straight through ends
     * up with an account that has no password and cannot pass the Mojang
     * handshake — unreachable until an admin deletes it. One stray click should
     * not be able to reach that state.</p>
     */
    public static void confirmQuestion(PkLoginPaper plugin, Player player, Runnable onDeclined) {
        Pending pending = PENDING.get(key(player.getName()));
        if (pending == null || pending.stage != Stage.ASKED) {
            return;
        }

        pending.stage = Stage.CONFIRMING;
        cancelTimeout(pending);

        for (String line : Messages.PREMIUM_QUESTION_CONFIRM.asList()) {
            player.sendMessage(AdventureAPI.parse(line));
        }

        Component yes = AdventureAPI.parse(
                        Messages.PREMIUM_QUESTION_CONFIRM_YES.asString("§4§l[ YES, I AM SURE ]"))
                .clickEvent(ClickEvent.runCommand("/premium confirm"))
                .hoverEvent(HoverEvent.showText(AdventureAPI.parse(
                        Messages.PREMIUM_QUESTION_CONFIRM_HOVER_YES.asString(
                                "§cOnly if this account is really yours"))));

        Component no = AdventureAPI.parse(Messages.PREMIUM_QUESTION_NO.asString("§c§l[ NO ]"))
                .clickEvent(ClickEvent.runCommand("/premium no"))
                .hoverEvent(HoverEvent.showText(AdventureAPI.parse(
                        Messages.PREMIUM_QUESTION_HOVER_NO.asString("§7Keep using a password"))));

        player.sendMessage(Component.empty().append(yes).append(Component.text("  ")).append(no));

        scheduleTimeout(plugin, player, pending, onDeclined);
    }

    /**
     * Claims the nickname: creates the account as premium and asks the player to
     * reconnect, at which point the handshake proves ownership and lets them in
     * without a password.
     */
    public static void acceptQuestion(PkLoginPaper plugin, Player player, String ip) {
        Pending pending = PENDING.get(key(player.getName()));
        if (pending == null || pending.stage != Stage.CONFIRMING) {
            return;
        }

        clear(player.getName());
        String name = player.getName();

        plugin.runAsync(() -> {
            plugin.getAccountManagement().createPremium(name, ip);
            onPlayerThread(plugin, player, () -> {
                if (!player.isOnline()) return;
                player.kick(LegacyComponentSerializer.legacySection().deserialize(
                        Messages.PREMIUM_SWITCH_KICK.asString(
                                "§aYou have switched to Premium mode.\n§ePlease reconnect to the server.")));
            });
        });
    }

    /** No, or ran out of time: carry on to the normal registration. */
    public static void declineQuestion(PkLoginPaper plugin, Player player, Runnable onDeclined) {
        if (clear(player.getName()) == null) {
            return;
        }

        player.sendMessage(Messages.PREMIUM_QUESTION_DECLINED.asString(
                "§7No problem. You will keep logging in with your password."));
        onDeclined.run();
    }

    /** Drops any pending question, for a player who left mid-answer. */
    public static void forget(String name) {
        clear(name);
    }

    private static void scheduleTimeout(PkLoginPaper plugin, Player player, Pending pending,
                                        Runnable onDeclined) {
        int seconds = Math.max(1, Settings.AUTOLOGIN_PREMIUM_QUESTION_TIMEOUT.asInt());
        final int armed = ++pending.generation;

        player.getScheduler().runDelayed(plugin, task -> {
            Pending current = PENDING.get(key(player.getName()));
            if (current != pending || current.generation != armed) {
                // Already answered, or a later step armed its own timer.
                return;
            }
            if (!player.isOnline()) {
                clear(player.getName());
                return;
            }
            declineQuestion(plugin, player, onDeclined);
        }, () -> clear(player.getName()), seconds * 20L);
    }

    /** Makes any armed timer a no-op without needing to cancel it. */
    private static void cancelTimeout(Pending pending) {
        pending.generation++;
    }

    private static Pending clear(String name) {
        Pending pending = PENDING.remove(key(name));
        if (pending != null) {
            cancelTimeout(pending);
        }
        return pending;
    }

    /**
     * Runs work on the thread that owns this player. Folia has no single main
     * thread, so anything touching a player goes through its own scheduler.
     */
    private static void onPlayerThread(PkLoginPaper plugin, Player player, Runnable work) {
        player.getScheduler().run(plugin, task -> work.run(), null);
    }

    /** Starts the login timer that the question was holding back. */
    public static void startLoginQueue(Player player, boolean registered) {
        LoginQueue.addToQueue(player.getName(), registered);
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
