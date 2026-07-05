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
    ALLOW_ADVERTISING(
            "allow-advertising",
            true
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
    PREMIUM_PROXY_MODE(
            "premium.proxy-mode",
            false
    ),
    APPENDER_ENABLED(
            "premium.username-appender.enabled",
            false
    ),
    APPENDER_PREMIUM_APPENDIX(
            "premium.username-appender.premium.username-appendix",
            ""
    ),
    APPENDER_PREMIUM_POSITION(
            "premium.username-appender.premium.position",
            "suffix"
    ),
    APPENDER_PREMIUM_DOMAINS(
            "premium.username-appender.premium.domains",
            java.util.Collections.singletonList("premium.myserver.com")
    ),
    APPENDER_OFFLINE_APPENDIX(
            "premium.username-appender.offline.username-appendix",
            "+"
    ),
    APPENDER_OFFLINE_POSITION(
            "premium.username-appender.offline.position",
            "suffix"
    ),
    APPENDER_OFFLINE_DOMAINS(
            "premium.username-appender.offline.domains",
            java.util.Collections.singletonList("myserver.com")
    ),
    LEGACY_UNIQUE_ID_TYPE(
            "premium.legacy.unique-id-type",
            "OFFLINE"
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
    HASH_ALGORITHM(
            "security.hash-algorithm",
            "BCRYPT"
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

    public int asInt() {
        return get(Integer.class);
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

