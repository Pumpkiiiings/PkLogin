package com.pumpkiiings.pklogin.common.security.twofactor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class TwoFactorManager {

    private static final TwoFactorManager INSTANCE = new TwoFactorManager();

    // Map<Code, PendingLink>
    private final Map<String, PendingLink> pendingLinks = new ConcurrentHashMap<>();

    // Map<Username, Code>
    private final Map<String, String> pendingLogins = new ConcurrentHashMap<>();

    private TwoFactorProvider discordProvider;
    private TwoFactorProvider emailProvider;

    private TwoFactorManager() {}

    public void init() {
        try {
            Class.forName("net.dv8tion.jda.api.JDA");
            if (discordProvider == null) {
                discordProvider = new com.pumpkiiings.pklogin.common.security.twofactor.impl.Discord2FA();
            }
            discordProvider.init();
        } catch (Throwable t) {
            System.err.println("[PkLogin] JDA library not found. Discord 2FA will be disabled.");
        }

        try {
            Class.forName("javax.mail.Authenticator");
            if (emailProvider == null) {
                emailProvider = new com.pumpkiiings.pklogin.common.security.twofactor.impl.Email2FA();
            }
            emailProvider.init();
        } catch (Throwable t) {
            System.err.println("[PkLogin] JavaMail library not found. Email 2FA will be disabled.");
        }
    }
    
    public void shutdown() {
        if (discordProvider != null) discordProvider.shutdown();
        if (emailProvider != null) emailProvider.shutdown();
    }
    
    public TwoFactorProvider getDiscordProvider() {
        return discordProvider;
    }
    
    public TwoFactorProvider getEmailProvider() {
        return emailProvider;
    }

    public static TwoFactorManager getInstance() {
        return INSTANCE;
    }

    public String generateLinkCode(String username, String providerId) {
        String code = String.format("%05d", ThreadLocalRandom.current().nextInt(100000));
        pendingLinks.put(code, new PendingLink(username.toLowerCase(), providerId, System.currentTimeMillis()));
        return code;
    }

    public PendingLink getPendingLink(String code) {
        PendingLink link = pendingLinks.get(code);
        if (link != null) {
            // Check expiration (10 minutes)
            if (System.currentTimeMillis() - link.timestamp > 10 * 60 * 1000) {
                pendingLinks.remove(code);
                return null;
            }
        }
        return link;
    }

    public void removeLinkCode(String code) {
        pendingLinks.remove(code);
    }

    public String generateLoginCode(String username) {
        String code = String.format("%05d", ThreadLocalRandom.current().nextInt(100000));
        pendingLogins.put(username.toLowerCase(), code);
        return code;
    }

    public boolean verifyLoginCode(String username, String code) {
        String expected = pendingLogins.get(username.toLowerCase());
        if (expected != null && expected.equals(code)) {
            pendingLogins.remove(username.toLowerCase());
            return true;
        }
        return false;
    }

    public void removeLoginCode(String username) {
        pendingLogins.remove(username.toLowerCase());
    }

    public boolean hasPendingLoginCode(String username) {
        return pendingLogins.containsKey(username.toLowerCase());
    }

    public static class PendingLink {
        public final String username;
        public final String providerId;
        public final long timestamp;

        public PendingLink(String username, String providerId, long timestamp) {
            this.username = username;
            this.providerId = providerId;
            this.timestamp = timestamp;
        }
    }
}
