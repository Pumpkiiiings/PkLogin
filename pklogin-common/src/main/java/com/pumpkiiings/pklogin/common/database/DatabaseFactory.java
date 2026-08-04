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

package com.pumpkiiings.pklogin.common.database;

import com.pumpkiiings.pklogin.common.settings.Settings;

import java.io.File;
import java.util.Locale;

/**
 * Builds the database for a given engine name.
 *
 * <p>Paper, Velocity and the migrator all turn {@code Database.type} into a
 * connection. They used to each keep their own copy of the same chain, which is
 * how one ends up supporting an engine the others do not.</p>
 */
public final class DatabaseFactory {

    private DatabaseFactory() {}

    /**
     * @param dataFolder where a file-backed engine keeps its database
     * @return an unopened database; the caller still has to connect it
     */
    public static Database create(String type, File dataFolder) {
        String engine = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);

        switch (engine) {
            case "mariadb":
            case "mysql":
                return new MariaDB(
                        Settings.DATABASE_HOST.asString(),
                        Settings.DATABASE_PORT.asInt(),
                        Settings.DATABASE_NAME.asString(),
                        Settings.DATABASE_USERNAME.asString(),
                        Settings.DATABASE_PASSWORD.asString());

            case "postgresql":
            case "postgres":
                return new PostgreSQL(
                        Settings.DATABASE_HOST.asString(),
                        Settings.DATABASE_PORT.asInt(),
                        Settings.DATABASE_NAME.asString(),
                        Settings.DATABASE_USERNAME.asString(),
                        Settings.DATABASE_PASSWORD.asString());

            case "h2":
                return new H2(new File(dataFolder, "accounts"));

            default:
                // SQLite also catches unrecognised names, so a typo degrades to a
                // working server rather than one that will not start.
                String customPath = Settings.DATABASE_SQLITE_FILE_PATH.asString("");
                return new SQLite(customPath.isEmpty()
                        ? new File(dataFolder, "accounts.db")
                        : new File(customPath));
        }
    }

    /** @return true if this is an engine name PkLogin knows */
    public static boolean isKnownEngine(String type) {
        if (type == null) return false;
        switch (type.trim().toLowerCase(Locale.ROOT)) {
            case "mariadb":
            case "mysql":
            case "postgresql":
            case "postgres":
            case "h2":
            case "sqlite":
                return true;
            default:
                return false;
        }
    }
}
