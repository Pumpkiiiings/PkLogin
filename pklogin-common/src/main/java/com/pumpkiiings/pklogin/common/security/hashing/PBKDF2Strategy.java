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

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public class PBKDF2Strategy implements HashStrategy {

    private static final int ITERATIONS = 600_000;
    private static final int KEY_LENGTH = 256;
    private static final SecureRandom RNG = new SecureRandom();

    @Override
    public String hash(String plainPassword) {
        byte[] salt = new byte[16];
        RNG.nextBytes(salt);
        byte[] hash = derive(plainPassword.toCharArray(), salt, ITERATIONS);
        return "$pbkdf2$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    @Override
    public boolean verify(String plainPassword, String storedHash) {
        if (!storedHash.startsWith("$pbkdf2$")) return false;
        String[] parts = storedHash.split("\\$", 5);
        if (parts.length < 5) return false;
        int iterations = Integer.parseInt(parts[2]);
        byte[] salt = Base64.getDecoder().decode(parts[3]);
        byte[] expected = Base64.getDecoder().decode(parts[4]);
        byte[] actual = derive(plainPassword.toCharArray(), salt, iterations);
        // constant-time comparison
        if (actual.length != expected.length) return false;
        int diff = 0;
        for (int i = 0; i < actual.length; i++) diff |= actual[i] ^ expected[i];
        return diff == 0;
    }

    @Override
    public boolean needsRehash(String storedHash) {
        if (!storedHash.startsWith("$pbkdf2$")) return true;
        try {
            String[] parts = storedHash.split("\\$", 5);
            return Integer.parseInt(parts[2]) < ITERATIONS;
        } catch (Exception e) {
            return true;
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            spec.clearPassword();
            return hash;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("PBKDF2 derivation failed", e);
        }
    }

}
