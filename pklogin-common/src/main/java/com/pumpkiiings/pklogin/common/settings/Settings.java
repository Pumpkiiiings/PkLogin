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

package com.pumpkiiings.pklogin.common.settings;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;

@RequiredArgsConstructor
public enum Settings {

    LANGUAGE_FILE(
            "languageFile",
            "messages_en.yml"
    ),
    TIME_TO_LOGIN(
            "Security.time-to-login",
            45
    ),
    PASSWORD_SMALL(
            "Security.password.small",
            5
    ),
    PASSWORD_LARGE(
            "Security.password.large",
            15
    ),
    SESSION_ENABLE(
            "Security.session.enable",
            true
    ),
    SESSION_TIMEOUT(
            "Security.session.timeout",
            5
    ),
    SECURE_PASSWORDS_ENABLE(
            "Security.password.secure.enable",
            false
    ),
    SECURE_PASSWORDS_ENFORCE(
            "Security.password.secure.enforce",
            false
    ),
    SECURE_PASSWORDS_REGEX(
            "Security.password.secure.secure-regex",
            "(?=\\S*\\d)(?=\\S*[A-Z])(?=\\S*[a-z])(?=\\S*[!@#$%^&*?])\\S*$"
    ),
    DATABASE_TYPE(
            "Database.type",
            "sqlite"
    ),
    DATABASE_SQLITE_FILE_PATH(
            "Database.sqlite-file-path",
            ""
    ),
    DATABASE_HOST(
            "Database.host",
            "localhost"
    ),
    DATABASE_PORT(
            "Database.port",
            3306
    ),
    DATABASE_NAME(
            "Database.database",
            "pklogin"
    ),
    DATABASE_USERNAME(
            "Database.username",
            "root"
    ),
    DATABASE_PASSWORD(
            "Database.password",
            ""
    ),
    APPENDER_ENABLED(
            "username-appender.enabled",
            false
    ),
    APPENDER_PREMIUM_APPENDIX(
            "username-appender.premium.username-appendix",
            ""
    ),
    APPENDER_PREMIUM_POSITION(
            "username-appender.premium.position",
            "suffix"
    ),
    APPENDER_PREMIUM_DOMAINS(
            "username-appender.premium.domains",
            java.util.Collections.singletonList("premium.myserver.com")
    ),
    APPENDER_OFFLINE_APPENDIX(
            "username-appender.offline.username-appendix",
            "+"
    ),
    APPENDER_OFFLINE_POSITION(
            "username-appender.offline.position",
            "suffix"
    ),
    APPENDER_OFFLINE_DOMAINS(
            "username-appender.offline.domains",
            java.util.Collections.singletonList("myserver.com")
    ),
    LEGACY_UNIQUE_ID_TYPE(
            "legacy.unique-id-type",
            "OFFLINE"
    ),
    AUTOLOGIN_BEDROCK_ENABLE(
            "autologin.bedrock.enable",
            true
    ),
    AUTOLOGIN_BEDROCK_SKIP_REGISTER(
            "autologin.bedrock.skip-register",
            true
    ),
    UI_TITLE_BAR(
            "ui.use-title-bar",
            true
    ),
    UI_ACTION_BAR(
            "ui.use-action-bar",
            true
    ),
    TELEPORT_SAFE_LOCATION(
            "teleport.safe-location",
            true
    ),
    TELEPORT_LAST_LOCATION(
            "teleport.last-location",
            true
    ),
    LIMBO_BLINDNESS_EFFECT(
            "limbo.blindness-effect",
            false
    ),
    LIMBO_HIDE_PLAYERS(
            "limbo.hide-players-before-login",
            true
    ),
    LIMBO_HIDE_INVENTORY(
            "limbo.inventory.hide-inventory",
            true
    ),
    LIMBO_BLOCK_WALK(
            "limbo.block-player-walk",
            true
    ),
    BRUTEFORCE_MAX_LOGIN_TRIES(
            "passwords.bruteforce.max-login-tries",
            3
    ),
    SECURITY_CAPTCHA_ENABLE(
            "security.captcha.enable",
            false
    ),
    SECURITY_CAPTCHA_TYPE(
            "security.captcha.type",
            "INVENTORY"
    ),
    SECURITY_IP_LIMIT_ENABLE(
            "security.ip-limit.enable",
            true
    ),
    SECURITY_IP_LIMIT(
            "security.ip-limit.limit",
            3
    ),
    BYPASS_ONLINE_CHECK_WITH_SAME_ADDRESS(
            "security.bypass-online-check-with-same-address",
            true
    ),
    // Lives under the capitalised "Security" block in config.yml, next to the
    // other password settings — not the lowercase "security" block below it.
    HASH_ALGORITHM(
            "Security.hash-algorithm",
            "BCRYPT"
    ),

    // Cost parameters for each algorithm. Raising them makes both hashing and
    // brute-forcing slower, so the right value depends on the server's CPU.
    HASH_BCRYPT_COST(
            "Security.hashing.bcrypt.cost",
            12
    ),
    HASH_PBKDF2_ITERATIONS(
            "Security.hashing.pbkdf2.iterations",
            600000
    ),
    HASH_ARGON2_ITERATIONS(
            "Security.hashing.argon2.iterations",
            2
    ),
    HASH_ARGON2_MEMORY_KB(
            "Security.hashing.argon2.memory-kb",
            65536
    ),
    HASH_ARGON2_PARALLELISM(
            "Security.hashing.argon2.parallelism",
            1
    ),
    COMMAND_COOLDOWN(
            "Security.command-cooldown",
            750
    ),
    VALID_NAME_REGEX(
            "Security.valid-name-regex",
            "([a-zA-Z0-9_]{3,16})|(\\*[a-zA-Z0-9_]{3,17})"
    ),

    TWO_FACTOR_CODE_LENGTH(
            "two-factor.code-length",
            6
    ),
    TWO_FACTOR_LOGIN_CODE_EXPIRATION(
            "two-factor.login-code-expiration",
            300
    ),
    TWO_FACTOR_LINK_CODE_EXPIRATION(
            "two-factor.link-code-expiration",
            600
    ),
    TWO_FACTOR_MAX_VERIFY_ATTEMPTS(
            "two-factor.max-verify-attempts",
            5
    ),
    TWO_FACTOR_DISCORD_MAX_LINK_ATTEMPTS(
            "two-factor.discord.max-link-attempts",
            10
    ),
    TWO_FACTOR_DISCORD_LINK_WINDOW(
            "two-factor.discord.link-attempt-window",
            600
    ),

    LIMBO_RESTORE_WALK_SPEED(
            "limbo.restore.walk-speed",
            0.2D
    ),
    LIMBO_RESTORE_FLY_SPEED(
            "limbo.restore.fly-speed",
            0.1D
    ),

    DATABASE_POOL_MAX_SIZE(
            "Database.pool.maximum-pool-size",
            10
    ),
    DATABASE_POOL_CONNECTION_TIMEOUT(
            "Database.pool.connection-timeout",
            10000
    ),
    DATABASE_POOL_MAX_LIFETIME(
            "Database.pool.max-lifetime",
            1800000
    ),

    SECURITY_CAPTCHA_CODE_LENGTH(
            "security.captcha.code-length",
            5
    ),
    SECURITY_CAPTCHA_BLOCK_COMMANDS(
            "security.captcha.blocked-commands",
            true
    ),

    UPDATES_CHECK(
            "updates.check",
            true
    ),
    UPDATES_NOTIFY_ADMINS(
            "updates.notify-admins",
            true
    ),

    AUTOLOGIN_PREMIUM_ENABLE(
            "autologin.premium.enable",
            true
    ),
    AUTOLOGIN_PREMIUM_CACHE_MINUTES(
            "autologin.premium.cache-minutes",
            60
    ),
    AUTOLOGIN_PREMIUM_SESSION_TIMEOUT(
            "autologin.premium.session-timeout",
            60
    ),
    AUTOLOGIN_PREMIUM_MOJANG_TIMEOUT(
            "autologin.premium.mojang-timeout",
            5000
    ),

    AUTHME_DATABASE_PATH(
            "authme-import.database-path",
            ""
    );

    static final HashMap<String, Object> SETTINGS = new HashMap<>();

    @Getter
    private final String key;
    @Getter
    private final Object def;

    /**
     * Add a setting to map.
     *
     * @param setting the setting to define
     * @param value   the setting value
     */
    public static void define(@NonNull Settings setting, Object value) {
        SETTINGS.put(setting.key, value);
    }

    /**
     * Clears the settings map.
     */
    public static void clear() {
        SETTINGS.clear();
    }

    public String asString() {
        return get(String.class);
    }

    public String asString(String fallback) {
        String val = get(String.class);
        return val == null ? fallback : val;
    }

    /**
     * Reads the value as an int.
     *
     * <p>Goes through {@link Number} rather than a strict {@code Integer} cast:
     * the YAML parser hands back a Long for large literals and a Double for
     * anything written with a decimal point, and neither should blow up here.</p>
     */
    public int asInt() {
        return (int) asNumber().longValue();
    }

    /** Reads the value as a double; see {@link #asInt()} for why this is lenient. */
    public double asDouble() {
        return asNumber().doubleValue();
    }

    /** Reads the value as a float, for the Bukkit speed setters. */
    public float asFloat() {
        return asNumber().floatValue();
    }

    private Number asNumber() {
        Object obj = SETTINGS.get(key);
        if (obj instanceof Number) {
            return (Number) obj;
        }
        if (def instanceof Number) {
            return (Number) def;
        }
        throw new ClassCastException("Setting " + key + " is not numeric!");
    }

    public boolean asBoolean() {
        return get(Boolean.class);
    }

    @SuppressWarnings("unchecked")
    public java.util.List<String> asList() {
        return get(java.util.List.class);
    }

    @SuppressWarnings("unchecked")
    private <T> T get(@NonNull Class<T> clasz) {
        if (def != null && !clasz.isAssignableFrom(def.getClass())) {
            throw new ClassCastException("Setting " + key + " is not assignable to " + clasz.getCanonicalName() + "!");
        }
        Object obj = SETTINGS.get(key);
        return (T) (obj == null || !clasz.isAssignableFrom(obj.getClass()) ? def : obj);
    }

}

