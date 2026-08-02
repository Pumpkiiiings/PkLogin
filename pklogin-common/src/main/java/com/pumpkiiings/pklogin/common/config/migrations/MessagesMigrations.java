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
 * The upgrade chain for the message files, versioned separately from
 * {@code config.yml} so a translation change does not force a config bump.
 *
 * <p>Every language file shares this chain: they all carry the same key set, only
 * the text differs. A server that has customised its messages keeps every line it
 * edited; new releases only ever add the strings it does not have yet.</p>
 */
public final class MessagesMigrations {

    /**
     * Schema version of the bundled message files.
     *
     * <ul>
     *   <li>1 — original key set (unversioned, or {@code version: "1.0"} / {@code "1.1"})</li>
     *   <li>2 — Discord/e-mail sections, extra error strings, repaired Title block ({@code "1.2"})</li>
     * </ul>
     */
    public static final int CURRENT_VERSION = 2;

    private MessagesMigrations() {}

    /** Registers every message step and the legacy stamp reader. */
    public static ConfigurationVersionManager applyTo(ConfigurationVersionManager manager) {
        return manager
                .withLegacyVersion("version", MessagesMigrations::readLegacyVersion)
                .register(new OneToTwo());
    }

    static Integer readLegacyVersion(String raw) {
        if (raw == null) return null;
        switch (raw.trim()) {
            case "1":
            case "1.0":
            case "1.1":
                return 1;
            case "1.2":
                return 2;
            default:
                return null;
        }
    }

    /**
     * 1 &rarr; 2. Repairs the auto-login titles, which shipped at the wrong nesting
     * level and so were unreachable.
     */
    static final class OneToTwo implements ConfigurationMigration {

        private static final String TITLE = "Messages.Title.";

        @Override
        public int fromVersion() {
            return 1;
        }

        @Override
        public String description() {
            return "Discord/e-mail message sections, repaired auto-login titles";
        }

        @Override
        public void migrate(MigrationContext context) {
            // These two blocks were indented one level too deep, landing inside
            // after-register (or, in several translations, inside the obsolete
            // action-bar section). The plugin looked for them at Title level and
            // always came up empty, so the titles never showed. Anything a server
            // had customised in the wrong place is moved to where it is read from.
            for (String name : new String[]{"bedrock-auto-login", "premium-auto-login"}) {
                for (String part : new String[]{"title", "subtitle",
                        "delays.start", "delays.duration", "delays.end"}) {
                    context.rename(
                            TITLE + "after-register." + name + "." + part,
                            TITLE + name + "." + part);
                    context.rename(
                            "Messages.action-bar." + name + "." + part,
                            TITLE + name + "." + part);
                }
            }
        }
    }
}
