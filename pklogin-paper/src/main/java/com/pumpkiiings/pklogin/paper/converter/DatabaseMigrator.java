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

package com.pumpkiiings.pklogin.paper.converter;

import com.pumpkiiings.pklogin.common.database.Database;
import com.pumpkiiings.pklogin.common.database.DatabaseFactory;
import com.pumpkiiings.pklogin.common.database.DatabaseSchema;
import com.pumpkiiings.pklogin.common.settings.Settings;
import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import org.bukkit.command.CommandSender;

import java.sql.ResultSet;

/**
 * Copies every account from the database in use to a different engine, so that
 * switching {@code Database.type} is not a fresh start with every player
 * unregistered.
 *
 * <p>Reads and writes whole rows rather than going through {@code Account}, which
 * carries only the fields needed at runtime — a migration that quietly dropped a
 * column would be worse than one that refused to run.</p>
 *
 * <p>Deletes nothing and does not touch {@code config.yml}: the administrator
 * still has the original if the report does not convince them.</p>
 */
public class DatabaseMigrator {

    private static final int PROGRESS_REPORT_INTERVAL = 100;

    /** The account columns, in the order both statements below use them. */
    private static final String[] COLUMNS = {
            "name", "realname", "password", "address", "lastlogin", "regdate",
            "totp_secret", "uuid_type", "random_uuid", "discord_id", "email_address"
    };

    private final PkLoginPaper plugin;

    public DatabaseMigrator(PkLoginPaper plugin) {
        this.plugin = plugin;
    }

    /** Runs the copy asynchronously so it does not block the main thread. */
    public void run(CommandSender sender, String targetType) {
        plugin.runAsync(() -> {
            try {
                migrate(sender, targetType);
            } catch (Exception e) {
                sender.sendMessage("§cMigration failed: " + e.getMessage());
                plugin.getLogger().severe("[DatabaseMigrator] Migration crashed: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void migrate(CommandSender sender, String targetType) throws Exception {
        if (!DatabaseFactory.isKnownEngine(targetType)) {
            sender.sendMessage("§cUnknown engine '" + targetType + "'.");
            sender.sendMessage("§7Choose one of: sqlite, h2, mariadb, mysql, postgresql");
            return;
        }

        String currentType = Settings.DATABASE_TYPE.asString();
        if (targetType.equalsIgnoreCase(currentType)) {
            sender.sendMessage("§cThat is the engine already in use. Nothing to copy.");
            return;
        }

        sender.sendMessage("§eCopying accounts from §f" + currentType + " §eto §f" + targetType + "§e...");
        sender.sendMessage("§7Connection details for the target are read from the same Database section");
        sender.sendMessage("§7of config.yml, so host, port, database, username and password must already");
        sender.sendMessage("§7point at it.");

        Database target = DatabaseFactory.create(targetType, plugin.getDataFolder());
        try {
            target.openConnection();
            DatabaseSchema.apply(target, warning -> sender.sendMessage("§e" + warning));
        } catch (Exception e) {
            sender.sendMessage("§cCould not open the target database: " + e.getMessage());
            closeQuietly(target);
            return;
        }

        int copied = 0, skipped = 0, failed = 0;

        try (Database.Query query = plugin.getDatabase().query(
                "SELECT `name`, `realname`, `password`, `address`, `lastlogin`, `regdate`, "
                        + "`totp_secret`, `uuid_type`, `random_uuid`, `discord_id`, `email_address` "
                        + "FROM `pklogin`")) {

            ResultSet source = query.resultSet;
            while (source.next()) {
                Object[] row = new Object[COLUMNS.length];
                for (int i = 0; i < COLUMNS.length; i++) {
                    row[i] = source.getObject(COLUMNS[i]);
                }

                String name = row[0] == null ? null : String.valueOf(row[0]);
                if (name == null || name.isEmpty()) {
                    failed++;
                } else if (exists(target, name)) {
                    // Re-running after a partial migration is normal, so an account
                    // already there is expected rather than an error.
                    skipped++;
                } else if (insert(target, row)) {
                    copied++;
                } else {
                    failed++;
                }

                int processed = copied + skipped + failed;
                if (processed % PROGRESS_REPORT_INTERVAL == 0) {
                    sender.sendMessage("§7Copied " + processed + " accounts so far...");
                }
            }
        } catch (Exception e) {
            sender.sendMessage("§cCould not read the current database: " + e.getMessage());
            closeQuietly(target);
            return;
        }

        int settings = copySettings(target);
        closeQuietly(target);

        sender.sendMessage("§aMigration complete.");
        sender.sendMessage("  §fCopied: §a" + copied
                + "  §fSkipped (already there): §e" + skipped
                + "  §fFailed: §c" + failed);
        if (settings > 0) {
            sender.sendMessage("  §fPlugin settings rows copied: §a" + settings);
        }
        sender.sendMessage("§eNothing was removed from " + currentType + ". To start using "
                + targetType + ",");
        sender.sendMessage("§eset Database.type to '" + targetType + "' in config.yml and restart.");
    }

    private boolean exists(Database target, String name) {
        try (Database.Query query = target.query(
                "SELECT COUNT(*) FROM `pklogin` WHERE `name` = ?", name.toLowerCase())) {
            return query.resultSet.next() && query.resultSet.getInt(1) > 0;
        } catch (Exception e) {
            // A broken read skips the row rather than risking a duplicate.
            return true;
        }
    }

    private boolean insert(Database target, Object[] row) {
        try {
            target.update("INSERT INTO `pklogin` (`name`, `realname`, `password`, `address`, "
                    + "`lastlogin`, `regdate`, `totp_secret`, `uuid_type`, `random_uuid`, "
                    + "`discord_id`, `email_address`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", row);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("[DatabaseMigrator] Could not copy '" + row[0] + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Copies the plugin's key/value rows, which hold things like the spawn point.
     * Losing them would leave the new database subtly incomplete.
     */
    private int copySettings(Database target) {
        int copied = 0;
        try (Database.Query query = plugin.getDatabase().query(
                "SELECT `key`, `value` FROM `settings`")) {

            ResultSet source = query.resultSet;
            while (source.next()) {
                String key = source.getString("key");
                String value = source.getString("value");
                if (key == null) continue;

                try (Database.Query existing = target.query(
                        "SELECT COUNT(*) FROM `settings` WHERE `key` = ?", key)) {
                    if (existing.resultSet.next() && existing.resultSet.getInt(1) > 0) {
                        continue;
                    }
                }

                target.update("INSERT INTO `settings` (`key`, `value`) VALUES (?, ?)", key, value);
                copied++;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[DatabaseMigrator] Could not copy plugin settings: " + e.getMessage());
        }
        return copied;
    }

    private void closeQuietly(Database database) {
        try {
            database.closeConnection();
        } catch (Exception ignored) {
        }
    }
}
