package com.pumpkiiings.pklogin.common.security.twofactor.impl;

import com.pumpkiiings.pklogin.common.PkLogin;
import com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorManager;
import com.pumpkiiings.pklogin.common.settings.Messages;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.channel.ChannelType;

public class DiscordListener extends ListenerAdapter {

    /** Codes a single Discord user may try per window before being ignored. */
    private static int maxAttemptsPerWindow() {
        return Math.max(1, com.pumpkiiings.pklogin.common.settings.Settings
                .TWO_FACTOR_DISCORD_MAX_LINK_ATTEMPTS.asInt());
    }

    private static long attemptWindowMillis() {
        return com.pumpkiiings.pklogin.common.settings.Settings
                .TWO_FACTOR_DISCORD_LINK_WINDOW.asInt() * 1000L;
    }

    private final java.util.Map<String, Attempts> attemptsByUser = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromType(ChannelType.PRIVATE)) return;

        String message = event.getMessage().getContentRaw().trim();
        int codeLength = TwoFactorManager.getCodeLength();

        if (message.length() != codeLength || !message.matches("\\d+")) {
            event.getChannel().sendMessage(Messages.DISCORD_BOT_HELP
                    .asString("Hi! To link your account, send me the {0}-digit code the Minecraft server gave you with /2fa discord.")
                    .replace("{0}", String.valueOf(codeLength))).queue();
            return;
        }

        // Anyone can DM the bot, so without a cap a script could walk the whole
        // code space and hijack whichever link happens to be pending.
        if (!allowAttempt(event.getAuthor().getId())) {
            return;
        }

        TwoFactorManager.PendingLink link = TwoFactorManager.getInstance().getPendingLink(message);
        if (link == null || !link.providerId.equals("DISCORD")) {
            event.getChannel().sendMessage(Messages.DISCORD_BOT_INVALID_CODE
                    .asString("That code is not valid or has expired.")).queue();
            return;
        }

        String discordId = event.getAuthor().getId();
        if (PkLogin.getAccountManagement().updateDiscordId(link.username, discordId)) {
            TwoFactorManager.getInstance().removeLinkCode(message);
            PkLogin.getAccountManagement().invalidateCache(link.username);
            event.getChannel().sendMessage(Messages.DISCORD_BOT_LINK_SUCCESS
                    .asString("Your Minecraft account **{0}** has been linked to Discord successfully!")
                    .replace("{0}", link.username)).queue();
        } else {
            event.getChannel().sendMessage(Messages.DISCORD_BOT_LINK_ERROR
                    .asString("Something went wrong while linking your account in the database.")).queue();
        }
    }

    private boolean allowAttempt(String discordUserId) {
        long now = System.currentTimeMillis();
        long window = attemptWindowMillis();
        attemptsByUser.entrySet().removeIf(e -> now - e.getValue().windowStart > window);

        Attempts attempts = attemptsByUser.computeIfAbsent(discordUserId, id -> new Attempts(now));
        synchronized (attempts) {
            if (now - attempts.windowStart > window) {
                attempts.windowStart = now;
                attempts.count = 0;
            }
            return ++attempts.count <= maxAttemptsPerWindow();
        }
    }

    private static final class Attempts {
        private long windowStart;
        private int count;

        private Attempts(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
