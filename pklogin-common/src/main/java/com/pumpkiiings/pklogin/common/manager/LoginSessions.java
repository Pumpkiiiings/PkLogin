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

package com.pumpkiiings.pklogin.common.manager;

import com.pumpkiiings.pklogin.common.settings.Settings;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lets a player who reconnects shortly after leaving skip the password.
 *
 * <p>A session binds a name to the address it was authenticated from, and an
 * address is not an identity: everyone behind one router, public network or
 * mobile carrier NAT shares it. Where a name is all it takes to try, an open
 * session lets anyone on that address take the account over. Hence a window
 * measured in minutes, an address check that is not optional, and state that
 * dies with the process rather than surviving a restart.</p>
 */
public final class LoginSessions {

    private static final class Session {
        private final String address;
        private final long expiresAt;

        private Session(String address, long expiresAt) {
            this.address = address;
            this.expiresAt = expiresAt;
        }
    }

    /** Lower-cased player name to session. */
    private static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>();

    private LoginSessions() {}

    public static boolean isEnabled() {
        return Settings.SESSION_ENABLE.asBoolean();
    }

    /**
     * Opens a session for a player who is leaving. Only ever call this for one who
     * was authenticated — otherwise it hands out a login they never earned.
     */
    public static void remember(String name, String address) {
        if (!isEnabled() || name == null || address == null) return;

        long minutes = Math.max(0, Settings.SESSION_TIMEOUT.asInt());
        if (minutes == 0) return;

        SESSIONS.put(key(name), new Session(address,
                System.currentTimeMillis() + minutes * 60_000L));
    }

    /**
     * Spends a session, if there is a valid one for this name and address.
     *
     * @return true if the player may skip the password
     */
    public static boolean resume(String name, String address) {
        if (!isEnabled() || name == null || address == null) return false;

        // Removed even when it does not match: one disconnect buys one reconnect,
        // not a series of attempts.
        Session session = SESSIONS.remove(key(name));
        if (session == null) return false;
        if (System.currentTimeMillis() > session.expiresAt) return false;

        return session.address.equals(address);
    }

    /** Call when what a session vouches for stops holding: password changed, account gone. */
    public static void invalidate(String name) {
        if (name == null) return;
        SESSIONS.remove(key(name));
    }

    public static void clear() {
        SESSIONS.clear();
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
