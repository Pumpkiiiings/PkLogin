package com.pumpkiiings.pklogin.paper.placeholder;

import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import io.github.miniplaceholders.api.Expansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

public final class PkLoginMiniExpansion {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm";

    private PkLoginMiniExpansion() {}

    public static void register(PkLoginPaper plugin) {
        AccountManagement accountMgr = plugin.getAccountManagement();

        Expansion.builder("pklogin")

            // Auth state
            .audiencePlaceholder(Player.class, "is_logged_in", (player, queue, ctx) ->
                Tag.preProcessParsed(bool(plugin.isAuthenticated(player))))

            .audiencePlaceholder(Player.class, "is_registered", (player, queue, ctx) -> {
                Optional<Account> acc = accountMgr.retrieveOrLoad(player.getName());
                return Tag.preProcessParsed(bool(acc.isPresent()
                        && acc.get().getHashedPassword() != null
                        && !acc.get().getHashedPassword().isEmpty()));
            })

            // Account type
            .audiencePlaceholder(Player.class, "account_type", (player, queue, ctx) -> {
                Optional<Account> acc = accountMgr.retrieveOrLoad(player.getName());
                return Tag.preProcessParsed(acc.map(PkLoginMiniExpansion::resolveType).orElse("UNKNOWN"));
            })

            .audiencePlaceholder(Player.class, "is_premium", (player, queue, ctx) -> {
                Optional<Account> acc = accountMgr.retrieveOrLoad(player.getName());
                return Tag.preProcessParsed(bool(acc.isPresent()
                        && "REAL".equalsIgnoreCase(acc.get().getUuidType())));
            })

            .audiencePlaceholder(Player.class, "is_cracked", (player, queue, ctx) -> {
                Optional<Account> acc = accountMgr.retrieveOrLoad(player.getName());
                return Tag.preProcessParsed(bool(acc.isPresent()
                        && "OFFLINE".equalsIgnoreCase(acc.get().getUuidType())));
            })

            .audiencePlaceholder(Player.class, "is_bedrock", (player, queue, ctx) -> {
                Optional<Account> acc = accountMgr.retrieveOrLoad(player.getName());
                return Tag.preProcessParsed(bool(acc.isPresent()
                        && "BEDROCK".equalsIgnoreCase(acc.get().getUuidType())));
            })

            // 2FA / security
            .audiencePlaceholder(Player.class, "has_2fa", (player, queue, ctx) -> {
                Optional<Account> acc = accountMgr.retrieveOrLoad(player.getName());
                return Tag.preProcessParsed(bool(acc.isPresent()
                        && acc.get().getTotpSecret() != null
                        && !acc.get().getTotpSecret().isEmpty()));
            })

            .audiencePlaceholder(Player.class, "has_discord", (player, queue, ctx) -> {
                Optional<Account> acc = accountMgr.retrieveOrLoad(player.getName());
                return Tag.preProcessParsed(bool(acc.isPresent()
                        && acc.get().getDiscordId() != null
                        && !acc.get().getDiscordId().isEmpty()));
            })

            .audiencePlaceholder(Player.class, "has_email", (player, queue, ctx) -> {
                Optional<Account> acc = accountMgr.retrieveOrLoad(player.getName());
                return Tag.preProcessParsed(bool(acc.isPresent()
                        && acc.get().getEmailAddress() != null
                        && !acc.get().getEmailAddress().isEmpty()));
            })

            // Dates
            .audiencePlaceholder(Player.class, "last_login", (player, queue, ctx) -> {
                Optional<Account> acc = accountMgr.retrieveOrLoad(player.getName());
                if (!acc.isPresent() || acc.get().getLastLogin() == 0) return Tag.preProcessParsed("N/A");
                return Tag.preProcessParsed(formatDate(acc.get().getLastLogin()));
            })

            .audiencePlaceholder(Player.class, "reg_date", (player, queue, ctx) -> {
                Optional<Account> acc = accountMgr.retrieveOrLoad(player.getName());
                if (!acc.isPresent() || acc.get().getRegDate() == 0) return Tag.preProcessParsed("N/A");
                return Tag.preProcessParsed(formatDate(acc.get().getRegDate()));
            })

            // Misc
            .audiencePlaceholder(Player.class, "ip", (player, queue, ctx) -> {
                Optional<Account> acc = accountMgr.retrieveOrLoad(player.getName());
                if (!acc.isPresent()) return Tag.preProcessParsed("N/A");
                String ip = acc.get().getAddress();
                return Tag.preProcessParsed(ip != null && !ip.isEmpty() ? ip : "N/A");
            })

            .globalPlaceholder("total_registered", (queue, ctx) ->
                Tag.selfClosingInserting(Component.text(plugin.getRegisteredUsers())))

            .build()
            .register();
    }

    private static String bool(boolean value) {
        return value ? "true" : "false";
    }

    private static String resolveType(Account account) {
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
