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

package com.pumpkiiings.pklogin.bukkit.converter;

import com.pumpkiiings.pklogin.bukkit.PkLoginBukkit;
import com.pumpkiiings.pklogin.common.database.Database;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Imports accounts from an AuthMe SQLite database into PkLogin.
 *
 * Passwords are stored as-is (AuthMe's $SHA$salt$hash format) and will be
 * automatically re-hashed to the configured algorithm on the player's first login,
 * because {@code AuthMeSHA256Strategy#needsRehash()} always returns {@code true}.
 *
 * Usage: {@code /pklogin authme-import}  (requires pklogin.admin permission)
 */
public class AuthMeConverter {

    private static final String FALLBACK_IP = "0.0.0.0";

    private final PkLoginBukkit plugin;

    public AuthMeConverter(PkLoginBukkit plugin) {
        this.plugin = plugin;
    }

    /** Runs the import asynchronously so it does not block the main thread. */
    public void run(CommandSender sender) {
        plugin.getFoliaLib().runAsync(task -> {
            try {
                doImport(sender);
            } catch (Exception e) {
                sender.sendMessage("§cConverter failed: " + e.getMessage());
                plugin.getLogger().severe("[AuthMeConverter] Import crashed: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // -------------------------------------------------------------------------

    private void doImport(CommandSender sender) throws Exception {
        File authMeDb = locateAuthMeDb();
        if (authMeDb == null) {
            sender.sendMessage("§cAuthMe SQLite database not found.");
            sender.sendMessage("§7Expected location: §fplugins/AuthMe/authme.db");
            sender.sendMessage("§7For MySQL AuthMe databases, export them to SQLite first.");
            return;
        }

        sender.sendMessage("§7Found AuthMe database at §f" + authMeDb.getPath());

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            sender.sendMessage("§cSQLite JDBC driver not found — cannot read the AuthMe database.");
            return;
        }

        List<AuthMeRow> rows;
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + authMeDb.getAbsolutePath())) {
            rows = readAuthMeRows(conn, sender);
        }

        if (rows == null) return; // error already reported to sender

        sender.sendMessage("§7Found §f" + rows.size() + " §7accounts. Starting import...");

        int imported = 0, skipped = 0, failed = 0;
        Database db = plugin.getDatabase();

        for (AuthMeRow row : rows) {
            if (alreadyExists(db, row.name)) {
                skipped++;
            } else if (insertRow(db, row)) {
                imported++;
            } else {
                failed++;
            }

            int processed = imported + skipped + failed;
            if (processed % 100 == 0) {
                sender.sendMessage("§7Progress: §f" + processed + " §7/ §f" + rows.size());
            }
        }

        sender.sendMessage("§aImport complete!");
        sender.sendMessage("  §fImported: §a" + imported
                + "  §fSkipped (already existed): §e" + skipped
                + "  §fFailed: §c" + failed);
        if (imported > 0) {
            sender.sendMessage("§7Players will be re-hashed to §f"
                    + com.pumpkiiings.pklogin.common.settings.Settings.HASH_ALGORITHM.asString("BCRYPT")
                    + " §7on their first successful login.");
        }
    }

    private List<AuthMeRow> readAuthMeRows(Connection conn, CommandSender sender) {
        List<AuthMeRow> rows = new ArrayList<>();
        String sql = "SELECT realname, password, ip, lastlogin FROM authme";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String realName = rs.getString("realname");
                String password = rs.getString("password");
                if (realName == null || realName.isEmpty() || password == null || password.isEmpty()) continue;
                String ip       = rs.getString("ip");
                long lastLogin  = rs.getLong("lastlogin");
                rows.add(new AuthMeRow(realName, password, ip, lastLogin));
            }
            return rows;
        } catch (SQLException e) {
            sender.sendMessage("§cFailed to read AuthMe database: " + e.getMessage());
            return null;
        }
    }

    private boolean alreadyExists(Database db, String name) {
        try (Database.Query q = db.query(
                "SELECT COUNT(*) FROM `pklogin` WHERE `name` = ?", name.toLowerCase())) {
            return q.resultSet.next() && q.resultSet.getInt(1) > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean insertRow(Database db, AuthMeRow row) {
        String ip = (row.ip != null && !row.ip.isEmpty()) ? row.ip : FALLBACK_IP;
        long ts   = row.lastLogin > 0 ? row.lastLogin : System.currentTimeMillis();
        try {
            db.update(
                "INSERT INTO `pklogin` " +
                "(`name`, `realname`, `password`, `address`, `lastlogin`, `regdate`) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
                row.name.toLowerCase(), row.name, row.password, ip, ts, ts
            );
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("[AuthMeConverter] Failed to import '" + row.name + "': " + e.getMessage());
            return false;
        }
    }

    private File locateAuthMeDb() {
        File pluginsDir = plugin.getDataFolder().getParentFile();
        File candidate  = new File(pluginsDir, "AuthMe/authme.db");
        return candidate.exists() ? candidate : null;
    }

    // -------------------------------------------------------------------------

    private static class AuthMeRow {
        final String name;
        final String password;
        final String ip;
        final long   lastLogin;

        AuthMeRow(String name, String password, String ip, long lastLogin) {
            this.name      = name;
            this.password  = password;
            this.ip        = ip;
            this.lastLogin = lastLogin;
        }
    }
}
