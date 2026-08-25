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

package com.pumpkiiings.pklogin.common.config.migrations;

import com.pumpkiiings.pklogin.common.config.ConfigurationMigration;
import com.pumpkiiings.pklogin.common.config.ConfigurationVersionManager;
import com.pumpkiiings.pklogin.common.config.MigrationContext;

/**
 * The upgrade chain for {@code config.yml}, and the one place its current version
 * is declared.
 *
 * <p>To ship a schema change: edit the bundled {@code config.yml}, add a step
 * below for {@code CURRENT_VERSION -> CURRENT_VERSION + 1}, register it in
 * {@link #applyTo}, then raise {@link #CURRENT_VERSION}. Purely additive changes
 * still need a step so the chain stays unbroken, but its body can be empty — the
 * merger picks new keys up from the shipped file.</p>
 */
public final class ConfigMigrations {

    /**
     * Schema version of the bundled {@code config.yml}.
     *
     * <p>History, with the version strings the old scheme used:</p>
     * <ul>
     *   <li>1 — original layout ({@code version: "1"} / {@code "1.0"} / {@code "1.1"})</li>
     *   <li>2 — proxy-secret and Bedrock auto-login ({@code "1.2"})</li>
     *   <li>3 — tunables moved out of the code ({@code "1.3"})</li>
     *   <li>4 — proxy setup stopped being configuration</li>
     *   <li>5 — login sessions</li>
     *   <li>6 — passwordless premium logins</li>
     *   <li>7 — the premium question, and premium auto-login off by default</li>
     * </ul>
     */
    public static final int CURRENT_VERSION = 8;

    private ConfigMigrations() {}

    /** Registers every config step and the legacy stamp reader. */
    public static ConfigurationVersionManager applyTo(ConfigurationVersionManager manager) {
        return manager
                .withLegacyVersion("version", ConfigMigrations::readLegacyVersion)
                .withLegacyVersion("file-version", ConfigMigrations::readLegacyVersion)
                .register(new OneToTwo())
                .register(new TwoToThree())
                .register(new ThreeToFour())
                .register(new FourToFive())
                .register(new FiveToSix())
                .register(new SixToSeven())
                .register(new SevenToEight());
    }

    /**
     * Translates the old dotted version strings into the new counter.
     *
     * @return the matching version, or null if the value is unrecognised
     */
    static Integer readLegacyVersion(String raw) {
        if (raw == null) return null;
        switch (raw.trim()) {
            case "1":
            case "1.0":
            case "1.1":
                return 1;
            case "1.2":
                return 2;
            case "1.3":
                return 3;
            default:
                return null;
        }
    }

    /**
     * 1 &rarr; 2. Adds {@code proxy-secret} and the Bedrock auto-login block, both
     * handled by the merger, and relocates the hashing algorithm setting.
     */
    static final class OneToTwo implements ConfigurationMigration {

        @Override
        public int fromVersion() {
            return 1;
        }

        @Override
        public String description() {
            return "proxy-secret, Bedrock auto-login, hash-algorithm relocation";
        }

        @Override
        public void migrate(MigrationContext context) {
            // The setting was documented under the capitalised "Security" block but
            // read from the lowercase one, so it never applied. Anyone who had
            // worked that out and set the lowercase key keeps their choice.
            context.rename("security.hash-algorithm", "Security.hash-algorithm");

            // Superseded by the version counter the manager now maintains.
            context.remove("file-version");
        }
    }

    /**
     * 2 &rarr; 3. Values that used to be compiled in became settings; every one of
     * them is a new key, so the merger adds them and there is nothing to move.
     */
    static final class TwoToThree implements ConfigurationMigration {

        @Override
        public int fromVersion() {
            return 2;
        }

        @Override
        public String description() {
            return "hashing costs, timeouts and limits became configurable";
        }

        @Override
        public void migrate(MigrationContext context) {
            // Intentionally empty. `allow-advertising` stopped being read in this
            // version but is left in place: an unused key is harmless, and deleting
            // settings out from under the server owner is not this system's job.
        }
    }

    /**
     * 3 &rarr; 4. Removes the two proxy options, which PkLogin now works out on
     * its own.
     *
     * <p>Deleted rather than left behind, against the usual rule, because an
     * unread key that looks like it decides where premium logins are handled — or
     * like it keeps them safe — has owners reasoning about their network's
     * security from a value nothing reads.</p>
     */
    static final class ThreeToFour implements ConfigurationMigration {

        @Override
        public int fromVersion() {
            return 3;
        }

        @Override
        public String description() {
            return "proxy-mode and proxy-secret are now detected, not configured";
        }

        @Override
        public void migrate(MigrationContext context) {
            context.remove("proxy-mode");
            context.remove("proxy-secret");
        }
    }

    /** 4 &rarr; 5. Adds the login session block; the merger writes it in. */
    static final class FourToFive implements ConfigurationMigration {

        @Override
        public int fromVersion() {
            return 4;
        }

        @Override
        public String description() {
            return "login sessions";
        }

        @Override
        public void migrate(MigrationContext context) {
            // Intentionally empty; see the class javadoc on additive steps.
        }
    }

    /**
     * 5 &rarr; 6. Adds the passwordless premium login options.
     *
     * <p>{@code autologin.premium.enable} arrives switched on, which is safe
     * because it only applies to names the server has never seen — no existing
     * account changes behaviour.</p>
     */
    static final class FiveToSix implements ConfigurationMigration {

        @Override
        public int fromVersion() {
            return 5;
        }

        @Override
        public String description() {
            return "premium players can log in without a password";
        }

        @Override
        public void migrate(MigrationContext context) {
            // Intentionally empty; see the class javadoc on additive steps.
        }
    }

    /**
     * Adds {@code autologin.premium.question}.
     *
     * <p>The bundled default for {@code autologin.premium.enable} flipped to
     * {@code false} in this version, but this step deliberately does not write
     * that: a server already running with it on has offline players who have
     * lived with the trade-off, and turning it off under them would silently
     * open every paid nickname they were relying on being reserved. New installs
     * get the new default from the bundled file; existing ones keep their
     * answer.</p>
     */
    static final class SixToSeven implements ConfigurationMigration {

        @Override
        public int fromVersion() {
            return 6;
        }

        @Override
        public String description() {
            return "ask a registering player whether a paid nickname is theirs";
        }

        @Override
        public void migrate(MigrationContext context) {
            // Intentionally empty; see the class javadoc on additive steps.
        }
    }

    static final class SevenToEight implements ConfigurationMigration {

        @Override
        public int fromVersion() {
            return 7;
        }

        @Override
        public String description() {
            return "forwarding-secret-path for custom pterodactyl panels etc.";
        }

        @Override
        public void migrate(MigrationContext context) {
            // Intentionally empty; see the class javadoc on additive steps.
        }
    }
}
