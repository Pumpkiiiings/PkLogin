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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Compatibility strategy for AuthMe's SHA256 format: $SHA$&lt;salt&gt;$&lt;hash&gt;
 * Used ONLY for verifying legacy AuthMe hashes during migration.
 * New passwords are never stored in this format (needsRehash always returns true).
 */
public class AuthMeSHA256Strategy implements HashStrategy {

    @Override
    public String hash(String plainPassword) {
        // Should never be called for new hashes — always upgrade
        throw new UnsupportedOperationException("AuthMeSHA256Strategy is read-only (migration use only).");
    }

    @Override
    public boolean verify(String plainPassword, String storedHash) {
        if (!storedHash.startsWith("$SHA$")) return false;
        String[] parts = storedHash.split("\\$", 4);
        // format: $SHA$<salt>$<hash>  =>  parts = ["", "SHA", salt, hash]
        if (parts.length < 4) return false;
        String salt = parts[2];
        String expected = parts[3];
        // AuthMe: sha256(sha256(plain) + salt)
        String inner = sha256(plainPassword);
        String outer = sha256(inner + salt);
        return expected.equalsIgnoreCase(outer);
    }

    @Override
    public boolean needsRehash(String storedHash) {
        // Always re-hash to upgrade away from AuthMe's format
        return true;
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return SHA256Strategy.bytesToHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

}
