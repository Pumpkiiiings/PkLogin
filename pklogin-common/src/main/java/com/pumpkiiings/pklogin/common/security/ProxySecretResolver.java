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

import com.pumpkiiings.pklogin.common.settings.Settings;

/**
 * Works out which key signs the plugin messages exchanged with the proxy.
 *
 * <p>Velocity's modern forwarding already establishes a secret that the proxy and
 * every backend know and the game client does not — exactly the property these
 * signatures need. Where that is in use, PkLogin reuses it and the server owner
 * configures nothing. {@code proxy-secret} in {@code config.yml} stays available
 * for setups that have no such secret, notably BungeeCord's legacy forwarding.</p>
 *
 * <p>The forwarding secret is never used directly. A separate key is derived from
 * it, so PkLogin's signatures and Velocity's forwarding never share key material:
 * whatever an attacker might learn from one cannot be turned against the other.</p>
 */
public final class ProxySecretResolver {

    /**
     * Domain separator for the derived key. Changing it invalidates every
     * signature, so proxy and backends must always agree on it — treat it as part
     * of the wire format and only bump it alongside a protocol change.
     */
    private static final String DERIVATION_LABEL = "PkLogin-ProxyAuth-v1";

    private ProxySecretResolver() {}

    /** Where the signing key came from, for logging and diagnostics. */
    public static final class Resolution {

        private final String key;
        private final String source;

        private Resolution(String key, String source) {
            this.key = key;
            this.source = source;
        }

        /** True when a usable signing key was found. */
        public boolean isPresent() {
            return key != null;
        }

        /** The signing key, or null when none could be resolved. */
        public String getKey() {
            return key;
        }

        /** Human-readable origin, or null when nothing was found. */
        public String getSource() {
            return source;
        }
    }

    /**
     * Resolves the signing key.
     *
     * @param forwardingSecret the proxy's modern-forwarding secret as read from
     *                         the platform, or null/blank if unavailable
     * @return the resolution; check {@link Resolution#isPresent()} before use
     */
    public static Resolution resolve(String forwardingSecret) {
        String configured = Settings.PROXY_SECRET.asString("");
        if (configured != null && !configured.trim().isEmpty()) {
            return new Resolution(configured.trim(), "'proxy-secret' in config.yml");
        }

        if (forwardingSecret != null && !forwardingSecret.trim().isEmpty()) {
            return new Resolution(derive(forwardingSecret.trim()),
                    "the Velocity modern forwarding secret");
        }

        return new Resolution(null, null);
    }

    /**
     * Derives PkLogin's signing key from the forwarding secret.
     *
     * <p>HMAC with the forwarding secret as the key and a fixed label as the
     * message: both sides compute the same value from the same input, and the
     * forwarding secret itself is never transmitted or exposed.</p>
     */
    public static String derive(String forwardingSecret) {
        return ProxyMessageSecurity.sign(forwardingSecret, DERIVATION_LABEL);
    }
}
