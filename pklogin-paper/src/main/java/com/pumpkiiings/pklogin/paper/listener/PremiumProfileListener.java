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

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.pumpkiiings.pklogin.common.settings.Settings;
import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import com.pumpkiiings.pklogin.paper.packet.AutoLoginSession;
import lombok.AllArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

/**
 * Gives a player who just proved they own a premium account the identity that
 * goes with it.
 *
 * <p>An offline-mode server derives a player's UUID from their name, so a
 * premium player who logs in here is a different person as far as everything
 * else is concerned: Mojang serves no skin for that UUID, and client mods that
 * look players up by it — Essential, Lunar and the like — find nobody. Nothing
 * done later can repair that, because those lookups happen off this server.</p>
 *
 * <p>Pre-login is the one moment where it can be corrected. The handshake has
 * already finished, so the real profile is known, and the server has not yet
 * built the player from it.</p>
 *
 * <p>Both halves of the profile are handled separately on purpose. The skin is
 * cosmetic and always applied. The UUID is the player's identity — every other
 * plugin's data is keyed by it — so changing it is left to
 * {@code legacy.unique-id-type: REAL}, which is what that setting has always
 * claimed to do.</p>
 */
@AllArgsConstructor
public class PremiumProfileListener implements Listener {

    private static final String TEXTURES = "textures";

    private final PkLoginPaper plugin;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        String ip = event.getAddress() == null ? null : event.getAddress().getHostAddress();
        if (ip == null) {
            return;
        }

        // Left in place: the join listener is what spends it.
        AutoLoginSession session = plugin.peekVerifiedSession(ip, event.getName());
        if (session == null || !session.isVerified()) {
            return;
        }

        try {
            PlayerProfile profile = event.getPlayerProfile();

            if (session.getSkinValue() != null) {
                profile.removeProperties(profile.getProperties().stream()
                        .filter(property -> TEXTURES.equals(property.getName()))
                        .toList());
                profile.setProperty(new ProfileProperty(
                        TEXTURES, session.getSkinValue(), session.getSkinSignature()));
            }

            if (session.getPremiumUUID() != null && wantsRealUuid()) {
                profile.setId(session.getPremiumUUID());
            }

            event.setPlayerProfile(profile);
        } catch (Throwable ex) {
            // A player without their skin still gets to play.
            plugin.getLogger().warning("Could not apply the premium profile for "
                    + event.getName() + ": " + ex);
        }
    }

    private static boolean wantsRealUuid() {
        return "REAL".equalsIgnoreCase(Settings.LEGACY_UNIQUE_ID_TYPE.asString("OFFLINE"));
    }
}
