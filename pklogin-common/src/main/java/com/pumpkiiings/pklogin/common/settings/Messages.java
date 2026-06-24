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

import com.pumpkiiings.pklogin.common.model.Title;
import com.pumpkiiings.pklogin.common.util.ChatColor;
import lombok.Getter;
import lombok.NonNull;

import java.util.List;

public enum Messages {

    // title messages
    TITLE_BEFORE_LOGIN("Title.before-login"),
    TITLE_BEFORE_REGISTER("Title.before-register"),
    TITLE_AFTER_LOGIN("Title.after-login"),
    TITLE_AFTER_REGISTER("Title.after-register"),
    TITLE_PREMIUM_AUTO_LOGIN("Title.premium-auto-login"),
    TITLE_BEDROCK_AUTO_LOGIN("Title.bedrock-auto-login"),

    // delay kick
    DELAY_KICK_LOGIN("delay-kick.login-kick"),
    DELAY_KICK_REGISTER("delay-kick.register-kick"),

    // successful operations
    PASSWORD_CHANGED("successful-operations.password-changed"),
    SUCCESSFUL_LOGIN("successful-operations.successful-login"),
    SUCCESSFUL_REGISTER("successful-operations.successful-register"),
    UNREGISTER_KICK("successful-operations.unregister-kick"),

    // kick messages
    NICK_ALREADY_REGISTERED("kick-messages.nick-already-registered"),
    FAILED_MANY_TIMES("kick-messages.failed-many-times"),
    INCORRECT_PASSWORD("kick-messages.incorrect-password"),
    INVALID_NICKNAME("kick-messages.invalid-nickname"),

    // error messages
    ALREADY_LOGIN("error-messages.already-login"),
    ALREADY_REGISTERED("error-messages.already-registered"),
    NOT_REGISTERED("error-messages.not-registered"),
    PASSWORDS_DONT_MATCH("error-messages.passwords-dont-match"),
    PASSWORD_SAME_AS_OLD("error-messages.password-same-as-old"),
    PASSWORD_TOO_LARGE("error-messages.password-too-large"),
    PASSWORD_TOO_SMALL("error-messages.password-too-small"),
    INSECURE_PASSWORD("error-messages.insecure-password"),
    CHANGE_PASSWORD_ENFORCED("error-messages.change-password-enforced"),
    INSUFFICIENT_PERMISSIONS("error-messages.insufficient-permissions"),
    ALREADY_ONLINE("error-messages.already-online"),
    PLAYER_COMMAND_USAGE("error-messages.player-command-usage"),
    PLUGIN_RELOAD_MESSAGE("error-messages.plugin-reload-message"),
    DATABASE_ERROR("error-messages.database-error"),

    // other messages
    MESSAGE_LOGIN("other-messages.message-login"),
    MESSAGE_REGISTER("other-messages.message-register"),
    MESSAGE_CHANGEPASSWORD("other-messages.message-changepassword"),
    MESSAGE_UNREGISTER("other-messages.message-unregister"),

    // admin commands
    ADMIN_FORCELOGIN_SUCCESS("admin.forcelogin-success"),
    ADMIN_UNREGISTER_SUCCESS("admin.unregister-success"),
    ADMIN_CHANGEPASS_SUCCESS("admin.changepass-success"),
    ADMIN_CHANGEPASS_KICK("admin.changepass-kick"),
    ADMIN_DUPEIP_HEADER("admin.dupeip-header"),
    ADMIN_DUPEIP_FORMAT("admin.dupeip-format"),
    ADMIN_DUPEIP_NONE("admin.dupeip-none"),
    ADMIN_AUTHME_IMPORT_START("admin.authme-import-start"),
    ADMIN_AUTHME_IMPORT_NOT_FOUND("admin.authme-import-not-found"),
    ADMIN_AUTHME_IMPORT_FOUND("admin.authme-import-found"),
    ADMIN_AUTHME_IMPORT_PROGRESS("admin.authme-import-progress"),
    ADMIN_AUTHME_IMPORT_DONE("admin.authme-import-done"),
    ADMIN_AUTHME_IMPORT_FAIL("admin.authme-import-fail"),
    ADMIN_DELETE_SUCCESS("admin.delete-success"),
    ADMIN_SETSPAWN_SUCCESS("admin.setspawn-success"),
    ADMIN_VERIFY_FORMAT("admin.verify-format"),
    ADMIN_ACCOUNT_NOT_FOUND("admin.account-not-found"),

    // two factor
    TWO_FACTOR_USAGE("two-factor.usage"),
    TWO_FACTOR_NOT_REGISTERED("two-factor.not-registered"),
    TWO_FACTOR_NOT_LOGGED_IN_SETUP("two-factor.not-logged-in-setup"),
    TWO_FACTOR_ALREADY_SETUP("two-factor.already-setup"),
    TWO_FACTOR_SETUP_HEADER("two-factor.setup-header"),
    TWO_FACTOR_SETUP_SECRET("two-factor.setup-secret"),
    TWO_FACTOR_SETUP_INSTRUCTION1("two-factor.setup-instruction1"),
    TWO_FACTOR_SETUP_INSTRUCTION2("two-factor.setup-instruction2"),
    TWO_FACTOR_SETUP_SUCCESS("two-factor.setup-success"),
    TWO_FACTOR_INVALID_CODE("two-factor.invalid-code"),
    TWO_FACTOR_NOT_AWAITING("two-factor.not-awaiting"),
    TWO_FACTOR_NOT_LOGGED_IN_DISABLE("two-factor.not-logged-in-disable"),
    TWO_FACTOR_NOT_SETUP("two-factor.not-setup"),
    TWO_FACTOR_DISABLE_USAGE("two-factor.disable-usage"),
    TWO_FACTOR_DISABLE_SUCCESS("two-factor.disable-success"),
    TWO_FACTOR_DISCORD_NOT_LOGGED_IN("two-factor.discord-not-logged-in"),
    TWO_FACTOR_DISCORD_ALREADY_LINKED("two-factor.discord-already-linked"),
    TWO_FACTOR_DISCORD_LINK_HEADER("two-factor.discord-link-header"),
    TWO_FACTOR_DISCORD_LINK_CODE("two-factor.discord-link-code"),
    TWO_FACTOR_DISCORD_LINK_INSTRUCTION1("two-factor.discord-link-instruction1"),
    TWO_FACTOR_DISCORD_LINK_INSTRUCTION2("two-factor.discord-link-instruction2"),
    TWO_FACTOR_LOGIN_SUCCESS("two-factor.login-success"),
    TWO_FACTOR_LOGIN_MESSAGE("two-factor.login-message"),

    // premium / offline
    OFFLINE_ALREADY("offline.already-offline"),
    OFFLINE_SUCCESS("offline.success"),
    PREMIUM_ALREADY("premium.already-premium"),
    PREMIUM_SUCCESS("premium.success"),
    PREMIUM_CURRENT_MODE("premium.current-mode"),
    PREMIUM_WARNING("premium.warning"),
    PREMIUM_AUTO_LOGIN("premium.auto-login"),

    // queues and limits
    QUEUE_MESSAGE("queue.message"),
    MAX_IPS_REACHED("error-messages.max-ips-reached"),
    ;

    @Getter
    private final String key;

    Messages(String key) {
        this.key = "Messages." + key;
    }

    /**
     * Add a message to settings map
     *
     * @param message the message to define
     * @param value   the message value
     */
    public static void define(@NonNull Messages message, Object value) {
        if (value instanceof String) {
            value = ChatColor.translateAlternateColorCodes('&', (String) value);
        } else if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            if (!list.isEmpty() && list.get(0) instanceof String) {
                list.replaceAll(a -> ChatColor.translateAlternateColorCodes('&', (String) a));
            }
        }
        Settings.SETTINGS.put(message.key, value);
    }

    public String asString() {
        return asString("§cMissing message: " + key);
    }

    public String asString(@NonNull String def) {
        Object obj = Settings.SETTINGS.get(key);
        return (String) (!(obj instanceof String) ? def : obj);
    }

    public List<String> asList() {
        Object obj = Settings.SETTINGS.get(key);
        if (obj instanceof List) {
            return (List<String>) obj;
        } else if (obj instanceof String) {
            return java.util.Collections.singletonList((String) obj);
        }
        return java.util.Collections.singletonList("§cMissing message: " + key);
    }

    public Title asTitle() {
        Object obj = Settings.SETTINGS.get(key);
        return (Title) (!(obj instanceof Title) ? Title.EMPTY : obj);
    }

}

