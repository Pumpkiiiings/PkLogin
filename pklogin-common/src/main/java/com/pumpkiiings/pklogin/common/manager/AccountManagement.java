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

package com.pumpkiiings.pklogin.common.manager;

import com.pumpkiiings.pklogin.common.database.Database;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.security.hashing.HashStrategy;
import com.pumpkiiings.pklogin.common.security.hashing.HashStrategyFactory;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class AccountManagement {

    private final Map<String, Account> accountCache = new HashMap<>();

    private final Database database;

    /**
     * Checks if the password provided is valid.
     *
     * @param password the password to compare
     * @return true if the passwords match
     */
    public boolean comparePassword(@NonNull Account account, @NonNull String password) {
        String storedHash = account.getHashedPassword();
        if (storedHash == null || storedHash.isEmpty()) return false;
        HashStrategy strategy = HashStrategyFactory.detectFor(storedHash);
        return strategy.verify(password, storedHash);
    }

    /**
     * Retrieve or load an account.
     *
     * @param name the name of the player
     * @return the player's {@link Account}. Failing, will return empty Optional.
     */
    public Optional<Account> retrieveOrLoad(@NonNull String name) {
        synchronized (accountCache) {
            Account account = accountCache.get(name.toLowerCase());
            if (account == null) {
                Optional<Account> accountOpt = search(name);
                if (accountOpt.isPresent()) {
                    account = accountOpt.get();
                    accountCache.put(name.toLowerCase(), account);
                }
            }
            return Optional.ofNullable(account);
        }
    }

    /**
     * Add an account to cache.
     *
     * @param account the account to add
     */
    public void addToCache(@NonNull Account account) {
        synchronized (accountCache) {
            accountCache.put(account.getRealName().toLowerCase(), account);
        }
    }

    /**
     * Invalidate an account from cache.
     *
     * @param key the key to invalidate
     */
    public void invalidateCache(@NonNull String key) {
        synchronized (accountCache) {
            accountCache.remove(key.toLowerCase());
        }
    }

    /**
     * Searches for saved accounts.
     *
     * @param name the name of the player
     * @return optional of {@link Account}
     */
    public Optional<Account> search(@NonNull String name) {
        try (Database.Query query = database.query("SELECT * FROM `pklogin` WHERE `name` = ?", name.toLowerCase())) {
            ResultSet resultSet = query.resultSet;
            if (resultSet.next()) {
                String realName = resultSet.getString("realname");
                String hashedPassword = resultSet.getString("password");
                String address = resultSet.getString("address");
                long lastLogin = resultSet.getLong("lastlogin");
                long regdate = resultSet.getLong("regdate");
                String totpSecret = null;
                try {
                    totpSecret = resultSet.getString("totp_secret");
                } catch (SQLException ignored) {
                    // Column might not exist in older setups
                }
                String uuidType = "REAL";
                try {
                    uuidType = resultSet.getString("uuid_type");
                    if (uuidType == null) uuidType = "REAL";
                } catch (SQLException ignored) { }
                String randomUuid = null;
                try {
                    randomUuid = resultSet.getString("random_uuid");
                } catch (SQLException ignored) { }
                String discordId = null;
                try {
                    discordId = resultSet.getString("discord_id");
                } catch (SQLException ignored) { }
                String emailAddress = null;
                try {
                    emailAddress = resultSet.getString("email_address");
                } catch (SQLException ignored) { }
                
                return Optional.of(new Account(realName, hashedPassword, address, totpSecret, uuidType, randomUuid, discordId, emailAddress, lastLogin, regdate));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Update the player's database column.
     *
     * @param name           the name of the player (realname)
     * @param hashedPassword the hashed password
     * @param address        the player address
     * @return true on success
     */
    public boolean update(@NonNull String name, @NonNull String hashedPassword, @Nullable String address) {
        return update(name, hashedPassword, address, true);
    }

    /**
     * Update the player's data.
     *
     * @param name           the name of the player (realname)
     * @param hashedPassword the hashed password
     * @param address        the player address
     * @param replace        forces update if player data exists
     * @return true on success
     */
    public boolean update(@NonNull String name, @NonNull String hashedPassword, @Nullable String address, boolean replace) {
        boolean exists = search(name).isPresent();
        if (exists) {
            if (!replace) {
                return false;
            }
        }

        if (hashedPassword.trim().isEmpty()) {
            return false;
        }

        long current = System.currentTimeMillis();

        try {
            if (exists) {
                database.update(
                        "UPDATE `pklogin` SET `password` = ?, `address` = ?, `lastlogin` = ? WHERE `name` = ?",
                        hashedPassword,
                        address == null ? "127.0.0.1" : address,
                        current,
                        name.toLowerCase()
                );
            } else {
                database.update(
                        "INSERT INTO `pklogin` (`name`, `realname`, `password`, `address`, `lastlogin`, `regdate`, `totp_secret`, `uuid_type`, `random_uuid`, `discord_id`, `email_address`) VALUES (?, ?, ?, ?, ?, ?, NULL, ?, ?, NULL, NULL)",
                        name.toLowerCase(),
                        name,
                        hashedPassword,
                        address == null ? "127.0.0.1" : address,
                        current,
                        current,
                        com.pumpkiiings.pklogin.common.settings.Settings.LEGACY_UNIQUE_ID_TYPE.asString(),
                        null
                );
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete all of the player's data.
     *
     * @param name the name of the player
     * @return true on success
     */
    public boolean delete(@NonNull String name) {
        boolean exists = search(name).isPresent();
        if (!exists) {
            return false;
        }

        try {
            database.update("DELETE FROM `pklogin` WHERE `name` = ?", name.toLowerCase());
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update the TOTP secret for the player.
     *
     * @param name       the name of the player
     * @param totpSecret the TOTP secret, or null to disable
     * @return true on success
     */
    public boolean updateTotpSecret(@NonNull String name, @Nullable String totpSecret) {
        try {
            database.update("UPDATE `pklogin` SET `totp_secret` = ? WHERE `name` = ?", totpSecret, name.toLowerCase());
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update the UUID Type for the player.
     *
     * @param name     the name of the player
     * @param uuidType the UUID type (REAL, RANDOM, OFFLINE)
     * @return true on success
     */
    public boolean updateUuidType(@NonNull String name, @NonNull String uuidType) {
        try {
            database.update("UPDATE `pklogin` SET `uuid_type` = ? WHERE `name` = ?", uuidType, name.toLowerCase());
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update the Random UUID for the player.
     *
     * @param name       the name of the player
     * @param randomUuid the random UUID string
     * @return true on success
     */
    public boolean updateRandomUuid(@NonNull String name, @NonNull String randomUuid) {
        try {
            database.update("UPDATE `pklogin` SET `random_uuid` = ? WHERE `name` = ?", randomUuid, name.toLowerCase());
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update the Discord ID for the player.
     */
    public boolean updateDiscordId(@NonNull String name, @Nullable String discordId) {
        try {
            database.update("UPDATE `pklogin` SET `discord_id` = ? WHERE `name` = ?", discordId, name.toLowerCase());
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update the Email Address for the player.
     */
    public boolean updateEmailAddress(@NonNull String name, @Nullable String emailAddress) {
        try {
            database.update("UPDATE `pklogin` SET `email_address` = ? WHERE `name` = ?", emailAddress, name.toLowerCase());
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Count the number of accounts associated with an IP address.
     *
     * @param ip the IP address
     * @return the number of accounts
     */
    public int countAccountsByIp(@NonNull String ip) {
        try (Database.Query query = database.query("SELECT COUNT(*) FROM `pklogin` WHERE `address` = ?", ip)) {
            ResultSet resultSet = query.resultSet;
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Get all accounts associated with an IP address.
     *
     * @param ip the IP address
     * @return a map of realname to lastlogin timestamp
     */
    public Map<String, Long> getAccountsByIp(@NonNull String ip) {
        Map<String, Long> accounts = new HashMap<>();
        try (Database.Query query = database.query("SELECT `realname`, `lastlogin` FROM `pklogin` WHERE `address` = ?", ip)) {
            ResultSet resultSet = query.resultSet;
            while (resultSet.next()) {
                accounts.put(resultSet.getString("realname"), resultSet.getLong("lastlogin"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return accounts;
    }
}

