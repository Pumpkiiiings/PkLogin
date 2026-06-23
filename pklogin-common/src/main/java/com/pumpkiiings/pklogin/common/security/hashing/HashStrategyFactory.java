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

package com.pumpkiiings.pklogin.common.security.hashing;

import com.pumpkiiings.pklogin.common.settings.Settings;

/**
 * Resolves the correct {@link HashStrategy} based on plugin settings or
 * auto-detects it from an existing stored hash (for verification/migration).
 */
public final class HashStrategyFactory {

    private HashStrategyFactory() {}

    /**
     * Returns the strategy configured in {@code config.yml} (key {@code Security.hash-algorithm}).
     * Defaults to BCrypt if the setting is missing or unrecognised.
     */
    public static HashStrategy fromSettings() {
        String name = Settings.HASH_ALGORITHM.asString("BCRYPT").toUpperCase();
        switch (name) {
            case "SHA256":  return new SHA256Strategy();
            case "SHA512":  return new SHA512Strategy();
            case "PBKDF2":  return new PBKDF2Strategy();
            case "ARGON2":  return new Argon2Strategy();
            case "BCRYPT":  return new BCryptStrategy();
            default:
                System.err.println("[PkLogin] Unknown hash algorithm '" + name + "', falling back to BCRYPT.");
                return new BCryptStrategy();
        }
    }

    /**
     * Auto-detects the correct strategy from the hash prefix so that
     * passwords stored with an old algorithm can still be verified.
     * After a successful login, call {@link HashStrategy#needsRehash(String)}
     * and if true, re-hash and update the database.
     */
    public static HashStrategy detectFor(String storedHash) {
        if (storedHash == null)              return new BCryptStrategy();
        if (storedHash.startsWith("$2"))     return new BCryptStrategy();
        if (storedHash.startsWith("$sha256$")) return new SHA256Strategy();
        if (storedHash.startsWith("$sha512$")) return new SHA512Strategy();
        if (storedHash.startsWith("$pbkdf2$")) return new PBKDF2Strategy();
        if (storedHash.startsWith("$argon2"))  return new Argon2Strategy();
        // AuthMe legacy format
        if (storedHash.startsWith("$SHA$"))    return new AuthMeSHA256Strategy();
        // Unknown — fall back to BCrypt so it just fails verify rather than crashing
        return new BCryptStrategy();
    }

}
