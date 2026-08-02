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

package com.pumpkiiings.pklogin.common.config;

/**
 * Where the migration system reports what it is doing.
 *
 * <p>Paper hands out a {@code java.util.logging.Logger} and Velocity an SLF4J
 * one; rather than pull either logging framework into the shared module, each
 * platform adapts its own.</p>
 */
public interface MigrationLogger {

    void info(String message);

    void warn(String message);

    void error(String message, Throwable cause);

    /** Adapter for the JUL logger Bukkit plugins are given. */
    static MigrationLogger of(java.util.logging.Logger logger) {
        return new MigrationLogger() {
            @Override
            public void info(String message) {
                logger.info(message);
            }

            @Override
            public void warn(String message) {
                logger.warning(message);
            }

            @Override
            public void error(String message, Throwable cause) {
                logger.log(java.util.logging.Level.SEVERE, message, cause);
            }
        };
    }

    /** Falls back to standard output; used when no platform logger is available. */
    static MigrationLogger console() {
        return new MigrationLogger() {
            @Override
            public void info(String message) {
                System.out.println("[PkLogin] " + message);
            }

            @Override
            public void warn(String message) {
                System.out.println("[PkLogin] WARN: " + message);
            }

            @Override
            public void error(String message, Throwable cause) {
                System.err.println("[PkLogin] ERROR: " + message);
                if (cause != null) cause.printStackTrace();
            }
        };
    }
}
