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
import com.pumpkiiings.pklogin.paper.task.LoginQueue;


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

    private final PkLoginPaper plugin;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        String name = player.getName();
        // Revoke state belonging to an older connection before this player can
        // emit any gameplay event. The Player object is the connection identity.
        plugin.beginConnection(player);
        boolean registered = plugin.getAccountManagement().retrieveOrLoad(name).isPresent();

        String ip = player.getAddress() == null || player.getAddress().getAddress() == null
                ? null
                : player.getAddress().getAddress().getHostAddress();
        // Verified sessions are keyed by address, so without one there is nothing
        // to look up — and the map would reject the null key outright.
        com.pumpkiiings.pklogin.paper.packet.AutoLoginSession session =
                ip == null ? null : plugin.consumeVerifiedSession(ip, name);

        if (session != null && session.isVerified()) {
            plugin.authenticate(player);

            // First time in: Mojang's handshake just proved this connection owns
            // the account, so there is something to save and no password to ask
            // for. Later joins find the account and skip this.
            if (!registered) {
                final String address = ip;
                plugin.runAsync(() -> plugin.getAccountManagement().createPremium(name, address));
            }

            player.sendMessage(Messages.PREMIUM_AUTO_LOGIN.asString());
            if (com.pumpkiiings.pklogin.common.settings.Settings.UI_TITLE_BAR.asBoolean()) {
                com.pumpkiiings.pklogin.paper.util.AdventureAPI.showTitle(player, Messages.TITLE_PREMIUM_AUTO_LOGIN.asTitle().title, Messages.TITLE_PREMIUM_AUTO_LOGIN.asTitle().subtitle, Messages.TITLE_PREMIUM_AUTO_LOGIN.asTitle().start, Messages.TITLE_PREMIUM_AUTO_LOGIN.asTitle().duration, Messages.TITLE_PREMIUM_AUTO_LOGIN.asTitle().end);
            }

            // The skin is already on: PremiumProfileListener put Mojang's signed
            // textures on the profile at pre-login, before the player existed.
            plugin.runAsync(() -> new com.pumpkiiings.pklogin.api.event.bukkit.AsyncAuthenticateEvent(player).callEvt());
            return;
        }

        boolean proxyAuthoritative = com.pumpkiiings.pklogin.paper.PaperProxyConfig.isVelocityForwardingEnabled()
                && plugin.getProxySecret() != null && !plugin.getProxySecret().isEmpty();

        if (!proxyAuthoritative
                && com.pumpkiiings.pklogin.common.settings.Settings.AUTOLOGIN_BEDROCK_ENABLE.asBoolean()
                && com.pumpkiiings.pklogin.common.hook.FloodgateHook.isBedrockPlayer(player.getUniqueId())) {
            plugin.authenticate(player);
            player.sendMessage(Messages.PREMIUM_AUTO_LOGIN.asString().replace("Premium", "Bedrock"));
            if (com.pumpkiiings.pklogin.common.settings.Settings.UI_TITLE_BAR.asBoolean()) {
                com.pumpkiiings.pklogin.paper.util.AdventureAPI.showTitle(player, Messages.TITLE_BEDROCK_AUTO_LOGIN.asTitle().title, Messages.TITLE_BEDROCK_AUTO_LOGIN.asTitle().subtitle, Messages.TITLE_BEDROCK_AUTO_LOGIN.asTitle().start, Messages.TITLE_BEDROCK_AUTO_LOGIN.asTitle().duration, Messages.TITLE_BEDROCK_AUTO_LOGIN.asTitle().end);
            }
            plugin.runAsync(() -> new com.pumpkiiings.pklogin.api.event.bukkit.AsyncAuthenticateEvent(player).callEvt());
            return;
        }

        // Checked before the queue and the captcha: a player with an open session
        // is already past the point either of those exists to guard.
        if (registered && com.pumpkiiings.pklogin.common.manager.LoginSessions.resume(name, ip)) {
            plugin.authenticate(player);
            player.sendMessage(Messages.SESSION_RESUMED.asString());
            plugin.runAsync(() -> new com.pumpkiiings.pklogin.api.event.bukkit.AsyncAuthenticateEvent(player).callEvt());
            return;
        }

        prepareLimbo(plugin, player);

        if (proxyAuthoritative) {
            // Do not guess from the database or display auth UI. Velocity owns
            // the handshake and answers once this backend says it is ready.
            plugin.getProxyAuthCoordinator().request(player);
            return;
        }

        beginPasswordAuthentication(plugin, player, true);
    }

    private static void prepareLimbo(PkLoginPaper plugin, Player player) {
        if (com.pumpkiiings.pklogin.common.settings.Settings.TELEPORT_SAFE_LOCATION.asBoolean()) {
            com.pumpkiiings.pklogin.paper.manager.LimboManager.teleportToSpawn(plugin, player);
        }
        com.pumpkiiings.pklogin.paper.manager.LimboManager.applyLimboState(plugin, player);
    }

    /** Starts backend password auth only after the proxy requests it or times out. */
    public static void beginPasswordAuthentication(PkLoginPaper plugin, Player player,
                                                   boolean limboAlreadyApplied) {
        if (!player.isOnline() || plugin.isAuthenticated(player)) return;

        String name = player.getName();
        boolean registered = plugin.getAccountManagement().retrieveOrLoad(name).isPresent();
        String ip = player.getAddress() == null || player.getAddress().getAddress() == null
                ? null
                : player.getAddress().getAddress().getHostAddress();

        if (!limboAlreadyApplied) prepareLimbo(plugin, player);

        if (com.pumpkiiings.pklogin.common.settings.Settings.SECURITY_CAPTCHA_ENABLE.asBoolean()) {
            LoginQueue.addToQueue(name, registered);
            com.pumpkiiings.pklogin.common.manager.CaptchaManager.getInstance().addPending(name, "");
            player.sendMessage(Messages.CAPTCHA_REQUIRED.asString());

            com.pumpkiiings.pklogin.paper.captcha.PaperCaptchaHandler.sendCaptcha(plugin, player);
            return;
        }

        if (registered) {
            LoginQueue.addToQueue(name, true);
            player.sendMessage(Messages.MESSAGE_LOGIN.asString());
            if (com.pumpkiiings.pklogin.common.settings.Settings.UI_TITLE_BAR.asBoolean()) {
                com.pumpkiiings.pklogin.paper.util.AdventureAPI.showTitle(player, Messages.TITLE_BEFORE_LOGIN.asTitle().title, Messages.TITLE_BEFORE_LOGIN.asTitle().subtitle, Messages.TITLE_BEFORE_LOGIN.asTitle().start, Messages.TITLE_BEFORE_LOGIN.asTitle().duration, Messages.TITLE_BEFORE_LOGIN.asTitle().end);
            }
            return;
        }

        // A name with no account here may still belong to someone: ask before
        // sending them off to invent a password they would immediately stop
        // using. The queue starts only once that is settled, so the seconds spent
        // reading the question do not come out of the time to register.
        com.pumpkiiings.pklogin.paper.manager.PremiumManager.askOnJoin(plugin, player, ip,
                () -> promptToRegister(plugin, player));
    }

    private static void promptToRegister(PkLoginPaper plugin, Player player) {
        // The proxy confirmation can arrive while the premium-name lookup is
        // still running. Its callback must not resurrect the registration UI.
        if (!player.isOnline() || plugin.isAuthenticated(player)) return;

        com.pumpkiiings.pklogin.paper.manager.PremiumManager.startLoginQueue(player, false);

        player.sendMessage(Messages.MESSAGE_REGISTER.asString());
        if (com.pumpkiiings.pklogin.common.settings.Settings.UI_TITLE_BAR.asBoolean()) {
            com.pumpkiiings.pklogin.paper.util.AdventureAPI.showTitle(player, Messages.TITLE_BEFORE_REGISTER.asTitle().title, Messages.TITLE_BEFORE_REGISTER.asTitle().subtitle, Messages.TITLE_BEFORE_REGISTER.asTitle().start, Messages.TITLE_BEFORE_REGISTER.asTitle().duration, Messages.TITLE_BEFORE_REGISTER.asTitle().end);
        }
    }
}
