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

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

/**
 * Argon2id strategy. Requires the argon2-jvm library.
 * If the library is not present at runtime this will fail with a NoClassDefFoundError.
 */
public class Argon2Strategy implements HashStrategy {

    private final Argon2 argon2 = Argon2Factory.createAdvanced(Argon2Factory.Argon2Types.ARGON2id);

    @Override
    public String hash(String plainPassword) {
        // Argon2 encodes its parameters into the hash, so raising these only
        // affects newly created passwords.
        return argon2.hash(
                com.pumpkiiings.pklogin.common.settings.Settings.HASH_ARGON2_ITERATIONS.asInt(),
                com.pumpkiiings.pklogin.common.settings.Settings.HASH_ARGON2_MEMORY_KB.asInt(),
                com.pumpkiiings.pklogin.common.settings.Settings.HASH_ARGON2_PARALLELISM.asInt(),
                plainPassword.toCharArray());
    }

    @Override
    public boolean verify(String plainPassword, String storedHash) {
        try {
            return argon2.verify(storedHash, plainPassword.toCharArray());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean needsRehash(String storedHash) {
        return !storedHash.startsWith("$argon2");
    }

}
