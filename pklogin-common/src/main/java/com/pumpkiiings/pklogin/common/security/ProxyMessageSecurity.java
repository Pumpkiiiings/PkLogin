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

package com.pumpkiiings.pklogin.common.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authenticates the {@code pklogin:main} plugin messages exchanged between the
 * proxy and a backend server.
 *
 * <p>Bukkit delivers messages on a registered incoming channel to the plugin
 * regardless of whether the proxy or the game client sent them — the two are
 * indistinguishable at the API level. Without a signature any client could send
 * a {@code PremiumAutoLogin} payload naming itself and be logged in. Every
 * privileged message therefore carries an HMAC-SHA256 over its fields, keyed by
 * a secret that only the proxy and the backends know.</p>
 *
 * <p>A timestamp window plus a single-use nonce stop a captured payload from
 * being replayed by whoever observed it.</p>
 */
public final class ProxyMessageSecurity {

    /** How far apart proxy and backend clocks may drift before a message is rejected. */
    public static final long MAX_CLOCK_SKEW_MILLIS = 30_000L;

    private static final String ALGORITHM = "HmacSHA256";
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Nonce -> expiry timestamp. Bounded by the freshness window. */
    private static final Map<String, Long> SEEN_NONCES = new ConcurrentHashMap<>();

    private ProxyMessageSecurity() {}

    /** Generates a fresh, unpredictable nonce for an outgoing message. */
    public static String newNonce() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Computes the signature for a message.
     *
     * @param secret the shared secret
     * @param parts  the message fields, in the exact order both sides agree on
     * @return the Base64 signature
     */
    public static String sign(String secret, String... parts) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            // Length-prefix each field so that ("ab","c") cannot collide with ("a","bc").
            for (String part : parts) {
                byte[] raw = (part == null ? "" : part).getBytes(StandardCharsets.UTF_8);
                mac.update(Integer.toString(raw.length).getBytes(StandardCharsets.UTF_8));
                mac.update((byte) ':');
                mac.update(raw);
            }
            return Base64.getEncoder().encodeToString(mac.doFinal());
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }

    /**
     * Validates a received message: signature, clock freshness and replay.
     *
     * @param secret    the shared secret; blank means "not configured"
     * @param signature the signature that came with the message
     * @param timestamp the timestamp that came with the message
     * @param nonce     the nonce that came with the message
     * @param parts     the signed fields, in the same order used by {@link #sign}
     * @return true only if the message is authentic, fresh and not a replay
     */
    public static boolean verify(String secret, String signature, long timestamp, String nonce, String... parts) {
        if (secret == null || secret.trim().isEmpty()) return false;
        if (signature == null || nonce == null) return false;

        long age = Math.abs(System.currentTimeMillis() - timestamp);
        if (age > MAX_CLOCK_SKEW_MILLIS) return false;

        if (!constantTimeEquals(sign(secret, parts), signature)) return false;

        // Only burn the nonce once the signature has proven the message genuine,
        // so a forged payload cannot invalidate a legitimate one.
        return consumeNonce(nonce);
    }

    /** @return true if this nonce had not been used yet within the freshness window */
    private static boolean consumeNonce(String nonce) {
        purgeExpiredNonces();
        Long previous = SEEN_NONCES.putIfAbsent(nonce, System.currentTimeMillis() + MAX_CLOCK_SKEW_MILLIS * 2);
        return previous == null;
    }

    private static void purgeExpiredNonces() {
        long now = System.currentTimeMillis();
        SEEN_NONCES.entrySet().removeIf(entry -> entry.getValue() < now);
    }

    /** Compares two strings without leaking their common prefix length through timing. */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
