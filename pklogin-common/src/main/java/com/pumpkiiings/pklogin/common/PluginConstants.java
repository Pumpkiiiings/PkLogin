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

package com.pumpkiiings.pklogin.common;

/**
 * Identifiers and endpoints that are fixed by a protocol or by the project
 * itself, and therefore must not become configuration.
 *
 * <p>Changing a plugin-message channel would stop the proxy and the backend from
 * understanding each other; changing a Mojang endpoint would break premium
 * verification; changing the update URL would point the updater at someone
 * else's builds. They are centralised only so each value appears once.</p>
 */
public final class PluginConstants {

    private PluginConstants() {}

    /** Plugin-message channel shared by the proxy and the backend servers. */
    public static final String CHANNEL_MAIN = "pklogin:main";

    /** Sub-channel the proxy uses to announce a verified premium player. */
    public static final String SUBCHANNEL_PREMIUM_AUTO_LOGIN = "PremiumAutoLogin";

    /** Sub-channel a backend uses to tell the proxy a player authenticated. */
    public static final String SUBCHANNEL_AUTHENTICATED = "Authenticated";

    /** Sub-channel the proxy uses to ask a backend to identify itself. */
    public static final String SUBCHANNEL_VERIFY = "PkLoginVerify";

    /** Sub-channel a backend answers {@link #SUBCHANNEL_VERIFY} on. */
    public static final String SUBCHANNEL_VERIFY_REPLY = "PkLoginVerifyReply";

    /** Address stored when a player's real IP is unavailable. */
    public static final String FALLBACK_ADDRESS = "127.0.0.1";

    // --- Mojang session API (fixed by the protocol) ---

    public static final String MOJANG_HAS_JOINED =
            "https://sessionserver.mojang.com/session/minecraft/hasJoined";

    public static final String MOJANG_PROFILE =
            "https://sessionserver.mojang.com/session/minecraft/profile/";

    /**
     * Answers whether a name belongs to a paid account; {@code 200} means it does,
     * {@code 404} that it does not. Asked before the login mode is decided, which
     * is the only moment the answer can still change anything.
     */
    public static final String MOJANG_NAME_LOOKUP =
            "https://api.mojang.com/users/profiles/minecraft/";

    // --- Project links ---

    public static final String GITHUB = "https://github.com/Pumpkiiiings/PkLogin";
    public static final String RELEASES = GITHUB + "/releases";
    public static final String LATEST_RELEASE_API =
            "https://api.github.com/repos/Pumpkiiiings/PkLogin/releases/latest";
    public static final String WEBSITE = "https://www.pumpkiiings.com";
    public static final String DISCORD_INVITE = "https://discord.gg/MVQ5r7X4Qd";

    /** Where {@code /pklogin update} pulls a new jar from; {0} is the tag. */
    public static String downloadUrl(String tag) {
        return RELEASES + "/download/" + tag + "/PkLogin.jar";
    }
}
