package com.pumpkiiings.pklogin.paper.placeholder;

import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.manager.LoginManagement;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

/**
 * Internal PlaceholderAPI expansion for PkLogin.
 *
 * Register with {@code new PkLoginPlaceholderExpansion(plugin).register()}
 * after confirming PlaceholderAPI is enabled.
 *
 * Available placeholders (prefix: pklogin_):
 *   is_logged_in        - true/false
 *   is_registered       - true/false
 *   account_type        - PREMIUM / CRACKED / BEDROCK / UNKNOWN
 *   is_premium          - true/false
 *   is_cracked          - true/false
 *   is_bedrock          - true/false
 *   has_2fa             - true/false
 *   has_discord         - true/false (Discord 2FA linked)
 *   has_email           - true/false (email 2FA linked)
 *   last_login          - formatted date or N/A
 *   reg_date            - formatted date or N/A
 *   ip                  - last known IP or N/A
 *   total_registered    - total accounts in the database
 */
public class PkLoginPlaceholderExpansion extends PlaceholderExpansion {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm";

    private final PkLoginPaper plugin;

    public PkLoginPlaceholderExpansion(PkLoginPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "pklogin";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "Pumpkiiiings";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /** Must be true for internal expansions so PlaceholderAPI does not unload on reload. */
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        String name = player.getName();
        if (name == null) return "";

        LoginManagement loginManagement = plugin.getLoginManagement();
        AccountManagement accountManagement = plugin.getAccountManagement();

        switch (params.toLowerCase()) {

            // Auth state
            case "is_logged_in": {
                org.bukkit.entity.Player online = player.getPlayer();
                return bool(online != null && plugin.isAuthenticated(online));
            }

            case "is_registered": {
                Optional<Account> acc = accountManagement.retrieveOrLoad(name);
                return bool(acc.isPresent()
                        && acc.get().getHashedPassword() != null
                        && !acc.get().getHashedPassword().isEmpty());
            }

            // Account type
            case "account_type": {
                Optional<Account> acc = accountManagement.retrieveOrLoad(name);
                return acc.map(PkLoginPlaceholderExpansion::resolveAccountType).orElse("UNKNOWN");
            }

            case "is_premium": {
                Optional<Account> acc = accountManagement.retrieveOrLoad(name);
                return bool(acc.isPresent() && "REAL".equalsIgnoreCase(acc.get().getUuidType()));
            }

            case "is_cracked": {
                Optional<Account> acc = accountManagement.retrieveOrLoad(name);
                return bool(acc.isPresent() && "OFFLINE".equalsIgnoreCase(acc.get().getUuidType()));
            }

            case "is_bedrock": {
                Optional<Account> acc = accountManagement.retrieveOrLoad(name);
                return bool(acc.isPresent() && "BEDROCK".equalsIgnoreCase(acc.get().getUuidType()));
            }

            // Two-Factor / security
            case "has_2fa": {
                Optional<Account> acc = accountManagement.retrieveOrLoad(name);
                return bool(acc.isPresent()
                        && acc.get().getTotpSecret() != null
                        && !acc.get().getTotpSecret().isEmpty());
            }

            case "has_discord": {
                Optional<Account> acc = accountManagement.retrieveOrLoad(name);
                return bool(acc.isPresent()
                        && acc.get().getDiscordId() != null
                        && !acc.get().getDiscordId().isEmpty());
            }

            case "has_email": {
                Optional<Account> acc = accountManagement.retrieveOrLoad(name);
                return bool(acc.isPresent()
                        && acc.get().getEmailAddress() != null
                        && !acc.get().getEmailAddress().isEmpty());
            }

            // Dates
            case "last_login": {
                Optional<Account> acc = accountManagement.retrieveOrLoad(name);
                if (!acc.isPresent() || acc.get().getLastLogin() == 0) return "N/A";
                return formatDate(acc.get().getLastLogin());
            }

            case "reg_date": {
                Optional<Account> acc = accountManagement.retrieveOrLoad(name);
                if (!acc.isPresent() || acc.get().getRegDate() == 0) return "N/A";
                return formatDate(acc.get().getRegDate());
            }

            // Misc
            case "ip": {
                Optional<Account> acc = accountManagement.retrieveOrLoad(name);
                if (!acc.isPresent()) return "N/A";
                String ip = acc.get().getAddress();
                return (ip != null && !ip.isEmpty()) ? ip : "N/A";
            }

            case "total_registered":
                return String.valueOf(plugin.getRegisteredUsers());

            default:
                return null;
        }
    }

    private static String bool(boolean value) {
        return value ? "true" : "false";
    }

    private static String resolveAccountType(Account account) {
        String type = account.getUuidType();
        if (type == null) return "UNKNOWN";
        switch (type.toUpperCase()) {
            case "REAL":    return "PREMIUM";
            case "OFFLINE": return "CRACKED";
            case "BEDROCK": return "BEDROCK";
            default:        return type.toUpperCase();
        }
    }

    private static String formatDate(long epochMillis) {
        return new SimpleDateFormat(DATE_FORMAT).format(new Date(epochMillis));
    }
}
