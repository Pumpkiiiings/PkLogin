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

import com.pumpkiiings.pklogin.common.PluginConstants;
import com.pumpkiiings.pklogin.common.settings.Settings;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Asks Mojang whether a name belongs to a paid account.
 *
 * <p>Not proof of anything on its own — it says the name is owned, not that the
 * person connecting owns it. Proof is the encryption handshake that follows a
 * yes, and that is what callers must rely on.</p>
 *
 * <p>An unanswerable question is not a "no", which is why they are separate
 * results. Send {@link Answer#UNKNOWN} down the offline path: guessing online
 * locks out every player the guess was wrong about, while treating a premium
 * player as offline costs them one password they can undo with {@code /premium}.</p>
 */
public final class PremiumNameLookup {

    public enum Answer {
        PREMIUM,
        NOT_PREMIUM,
        /** Mojang could not be asked, or did not answer usefully. */
        UNKNOWN
    }

    private static final class Cached {
        private final Answer answer;
        private final long expiresAt;

        private Cached(Answer answer, long expiresAt) {
            this.answer = answer;
            this.expiresAt = expiresAt;
        }
    }

    private static final Map<String, Cached> CACHE = new ConcurrentHashMap<>();

    private PremiumNameLookup() {}

    public static boolean isEnabled() {
        return Settings.AUTOLOGIN_PREMIUM_ENABLE.asBoolean();
    }

    /**
     * Looks a name up, using the cache when it can. Blocks on the network — never
     * call it from a thread handling packets or ticking the server.
     */
    public static Answer lookup(String name) {
        if (name == null || name.isEmpty()) return Answer.UNKNOWN;

        String key = name.toLowerCase(Locale.ROOT);
        Cached cached = CACHE.get(key);
        if (cached != null && System.currentTimeMillis() < cached.expiresAt) {
            return cached.answer;
        }

        Answer answer = ask(name);

        // Only settled answers are worth keeping. Caching a failure would turn one
        // outage into an hour of players being sent down the wrong path.
        if (answer != Answer.UNKNOWN) {
            long minutes = Math.max(1, Settings.AUTOLOGIN_PREMIUM_CACHE_MINUTES.asInt());
            CACHE.put(key, new Cached(answer, System.currentTimeMillis() + minutes * 60_000L));
        }
        return answer;
    }

    public static void invalidate(String name) {
        if (name == null) return;
        CACHE.remove(name.toLowerCase(Locale.ROOT));
    }

    public static void clear() {
        CACHE.clear();
    }

    /** Any doubt at all comes back as {@link Answer#UNKNOWN}. */
    private static Answer ask(String name) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(PluginConstants.MOJANG_NAME_LOOKUP
                    + URLEncoder.encode(name, StandardCharsets.UTF_8.name()));

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent",
                    "PkLogin (+" + PluginConstants.GITHUB + ")");

            int timeout = Settings.AUTOLOGIN_PREMIUM_MOJANG_TIMEOUT.asInt();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);

            switch (connection.getResponseCode()) {
                case HttpURLConnection.HTTP_OK:
                    return Answer.PREMIUM;
                case HttpURLConnection.HTTP_NOT_FOUND:
                case HttpURLConnection.HTTP_NO_CONTENT:
                    // Both have meant "no such paid account" over the years.
                    return Answer.NOT_PREMIUM;
                default:
                    // Rate limiting and outages land here. Neither is an answer.
                    return Answer.UNKNOWN;
            }
        } catch (Exception unreachable) {
            return Answer.UNKNOWN;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
