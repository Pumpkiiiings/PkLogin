package com.pumpkiiings.pklogin.common.security.twofactor;

import com.pumpkiiings.pklogin.common.settings.Settings;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TwoFactorManager {

    private static final TwoFactorManager INSTANCE = new TwoFactorManager();

    /** Below this, brute force is trivial even when attempts are capped. */
    private static final int MIN_CODE_DIGITS = 4;
    /** Above this, 10^digits no longer fits in an int. */
    private static final int MAX_CODE_DIGITS = 9;

    /** Digits per code. Six keeps brute force impractical once attempts are capped. */
    private static int codeDigits() {
        int configured = Settings.TWO_FACTOR_CODE_LENGTH.asInt();
        return Math.min(MAX_CODE_DIGITS, Math.max(MIN_CODE_DIGITS, configured));
    }

    /** How long a login code stays usable. */
    private static long loginCodeTtlMillis() {
        return Settings.TWO_FACTOR_LOGIN_CODE_EXPIRATION.asInt() * 1000L;
    }

    /** How long a Discord link code stays usable. */
    private static long linkCodeTtlMillis() {
        return Settings.TWO_FACTOR_LINK_CODE_EXPIRATION.asInt() * 1000L;
    }

    /** Wrong guesses allowed before the code is burned and the player must log in again. */
    private static int maxVerifyAttempts() {
        return Math.max(1, Settings.TWO_FACTOR_MAX_VERIFY_ATTEMPTS.asInt());
    }

    /**
     * Codes must be unguessable, so they come from a CSPRNG rather than
     * {@code ThreadLocalRandom} — the latter's output is predictable from a few
     * observed values, which would let anyone who links an account derive the
     * codes issued to others.
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    // Map<Code, PendingLink>
    private final Map<String, PendingLink> pendingLinks = new ConcurrentHashMap<>();

    // Map<Username, PendingLogin>
    private final Map<String, PendingLogin> pendingLogins = new ConcurrentHashMap<>();

    private TwoFactorProvider discordProvider;
    private TwoFactorProvider emailProvider;

    private TwoFactorManager() {}

    /** Outcome of checking a login code. */
    public enum VerificationResult {
        /** The code matched; the caller may authenticate the player. */
        SUCCESS,
        /** Wrong code, but the player may try again. */
        INVALID_CODE,
        /** No code is pending, or the one issued has expired. */
        NO_PENDING_CODE,
        /** Too many wrong guesses; the code was discarded. */
        TOO_MANY_ATTEMPTS
    }

    /**
     * Brings up whichever 2FA providers have their optional libraries present.
     *
     * @param dataFolder the plugin data folder holding the {@code 2fa} configs
     */
    public void init(java.io.File dataFolder) {
        try {
            Class.forName("net.dv8tion.jda.api.JDA");
            if (discordProvider == null) {
                discordProvider = new com.pumpkiiings.pklogin.common.security.twofactor.impl.Discord2FA();
            }
            discordProvider.init(dataFolder);
        } catch (Throwable t) {
            System.err.println("[PkLogin] JDA library not found. Discord 2FA will be disabled.");
        }

        try {
            Class.forName("javax.mail.Authenticator");
            if (emailProvider == null) {
                emailProvider = new com.pumpkiiings.pklogin.common.security.twofactor.impl.Email2FA();
            }
            emailProvider.init(dataFolder);
        } catch (Throwable t) {
            System.err.println("[PkLogin] JavaMail library not found. Email 2FA will be disabled.");
        }
    }

    public void shutdown() {
        if (discordProvider != null) discordProvider.shutdown();
        if (emailProvider != null) emailProvider.shutdown();
        pendingLinks.clear();
        pendingLogins.clear();
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

    /** Number of digits in the codes this manager issues. */
    public static int getCodeLength() {
        return codeDigits();
    }

    private static String newCode() {
        int digits = codeDigits();
        int bound = (int) Math.pow(10, digits);
        return String.format("%0" + digits + "d", RANDOM.nextInt(bound));
    }

    /**
     * Issues a code the player must send to the provider's bot to link an account.
     *
     * <p>Link codes are keyed by the code itself, so a collision would hand one
     * player's pending link to another; the loop below keeps drawing until the
     * code is free.</p>
     */
    public String generateLinkCode(String username, String providerId) {
        purgeExpired();
        String code;
        PendingLink link = new PendingLink(username.toLowerCase(), providerId, System.currentTimeMillis());
        do {
            code = newCode();
        } while (pendingLinks.putIfAbsent(code, link) != null);
        return code;
    }

    public PendingLink getPendingLink(String code) {
        PendingLink link = pendingLinks.get(code);
        if (link == null) return null;
        if (System.currentTimeMillis() - link.timestamp > linkCodeTtlMillis()) {
            pendingLinks.remove(code);
            return null;
        }
        return link;
    }

    public void removeLinkCode(String code) {
        pendingLinks.remove(code);
    }

    /** Issues a fresh login code, replacing any previous one for that player. */
    public String generateLoginCode(String username) {
        purgeExpired();
        String code = newCode();
        pendingLogins.put(username.toLowerCase(),
                new PendingLogin(code, System.currentTimeMillis() + loginCodeTtlMillis()));
        return code;
    }

    /**
     * Checks a login code, counting the attempt.
     *
     * <p>A code survives only a handful of wrong guesses: without that cap the
     * six-digit space could be walked through in seconds by a scripted client.</p>
     */
    public VerificationResult verifyLoginCode(String username, String code) {
        String key = username.toLowerCase();
        PendingLogin pending = pendingLogins.get(key);

        if (pending == null || pending.isExpired()) {
            pendingLogins.remove(key);
            return VerificationResult.NO_PENDING_CODE;
        }

        synchronized (pending) {
            if (constantTimeEquals(pending.code, code)) {
                pendingLogins.remove(key);
                return VerificationResult.SUCCESS;
            }

            pending.attempts++;
            if (pending.attempts >= maxVerifyAttempts()) {
                pendingLogins.remove(key);
                return VerificationResult.TOO_MANY_ATTEMPTS;
            }
        }
        return VerificationResult.INVALID_CODE;
    }

    public void removeLoginCode(String username) {
        pendingLogins.remove(username.toLowerCase());
    }

    public boolean hasPendingLoginCode(String username) {
        PendingLogin pending = pendingLogins.get(username.toLowerCase());
        return pending != null && !pending.isExpired();
    }

    /**
     * Drops codes that are past their lifetime.
     *
     * <p>Entries used to sit in the maps until the process restarted, which both
     * leaked memory on a busy server and left every issued code valid forever.</p>
     */
    private void purgeExpired() {
        long now = System.currentTimeMillis();
        pendingLinks.entrySet().removeIf(e -> now - e.getValue().timestamp > linkCodeTtlMillis());
        pendingLogins.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
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

    private static final class PendingLogin {
        private final String code;
        private final long expiresAt;
        private int attempts;

        private PendingLogin(String code, long expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }

        private boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
