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

import java.sql.SQLException;
import java.util.function.Consumer;

/**
 * Creates and upgrades the PkLogin tables.
 *
 * <p>Both the Paper plugin and the Velocity plugin talk to the same schema — and
 * frequently to the same file — so the statements live here rather than being
 * copied into each platform's startup code.</p>
 */
public final class DatabaseSchema {

    private DatabaseSchema() {}

    /**
     * Brings the database up to the current schema.
     *
     * @param database the open database
     * @param warn     receives non-fatal problems worth showing the administrator
     * @throws SQLException if the base tables cannot be created
     */
    public static void apply(Database database, Consumer<String> warn) throws SQLException {
        database.update("CREATE TABLE IF NOT EXISTS `pklogin` ("
                + "`name` VARCHAR(64), `realname` VARCHAR(64), `password` TEXT, `address` VARCHAR(64), "
                + "`lastlogin` BIGINT, `regdate` BIGINT, `totp_secret` TEXT, "
                + "`uuid_type` VARCHAR(16) DEFAULT 'REAL', `random_uuid` VARCHAR(36), "
                + "`discord_id` VARCHAR(32), `email_address` VARCHAR(255))");
        database.update("CREATE TABLE IF NOT EXISTS `settings` (`key` VARCHAR(64), `value` TEXT)");

        // Widen the timestamp columns on installs created when they were 32-bit.
        // MODIFY COLUMN is MySQL syntax, but it only ever mattered to installs
        // predating the fix — everywhere else CREATE TABLE already said BIGINT, so
        // nothing is lost when the statement fails.
        quietly(database, "ALTER TABLE `pklogin` MODIFY COLUMN `lastlogin` BIGINT");
        quietly(database, "ALTER TABLE `pklogin` MODIFY COLUMN `regdate` BIGINT");

        // Columns added after the first release. Each fails harmlessly if present.
        quietly(database, "ALTER TABLE `pklogin` ADD COLUMN `totp_secret` TEXT");
        quietly(database, "ALTER TABLE `pklogin` ADD COLUMN `uuid_type` VARCHAR(16) DEFAULT 'REAL'");
        quietly(database, "ALTER TABLE `pklogin` ADD COLUMN `random_uuid` VARCHAR(36)");
        quietly(database, "ALTER TABLE `pklogin` ADD COLUMN `discord_id` VARCHAR(32)");
        quietly(database, "ALTER TABLE `pklogin` ADD COLUMN `email_address` VARCHAR(255)");

        createIndexes(database, warn);
    }

    /**
     * Adds the lookup indexes.
     *
     * <p>Every account read filters on {@code name} and the IP-limit and dupeip
     * features filter on {@code address}; without indexes each one scans the whole
     * table, which is painful after an AuthMe import of tens of thousands of rows.
     * The unique index on {@code name} also stops a concurrent registration from
     * inserting the same account twice.</p>
     */
    private static void createIndexes(Database database, Consumer<String> warn) {
        String nameColumn = database.indexedColumn("name", 64);
        String addressColumn = database.indexedColumn("address", 64);

        boolean unique = tryCreateIndex(database,
                "CREATE UNIQUE INDEX " + ifNotExists(database) + "`idx_pklogin_name` ON `pklogin` (" + nameColumn + ")");

        if (!unique) {
            // Almost always means the table already holds duplicate names from a
            // version without the constraint. Fall back to a plain index so reads
            // are still fast, and tell the administrator so they can clean up.
            boolean plain = tryCreateIndex(database,
                    "CREATE INDEX " + ifNotExists(database) + "`idx_pklogin_name` ON `pklogin` (" + nameColumn + ")");
            if (plain) {
                warn.accept("Could not make the account name index unique — your 'pklogin' table probably contains "
                        + "duplicate names. Remove the duplicates and restart to enable the constraint.");
            }
        }

        tryCreateIndex(database,
                "CREATE INDEX " + ifNotExists(database) + "`idx_pklogin_address` ON `pklogin` (" + addressColumn + ")");
    }

    private static String ifNotExists(Database database) {
        return database.supportsIfNotExistsOnIndex() ? "IF NOT EXISTS " : "";
    }

    /** @return true if the index now exists (created here or already present) */
    private static boolean tryCreateIndex(Database database, String statement) {
        try {
            database.update(statement);
            return true;
        } catch (SQLException e) {
            // MySQL/MariaDB cannot say IF NOT EXISTS for indexes, so "already
            // exists" arrives as an error and is the expected path on restart.
            String message = e.getMessage();
            return message != null && message.toLowerCase().contains("exist");
        }
    }

    private static void quietly(Database database, String statement) {
        try {
            database.update(statement);
        } catch (Exception ignored) {
            // Column or modification already applied.
        }
    }
}
