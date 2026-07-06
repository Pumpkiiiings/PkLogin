package com.pumpkiiings.pklogin.api;

import java.util.Map;
import java.util.Optional;

@Deprecated
public interface PkLoginAPI {

    /**
     * Checks if the player is currently authenticated (logged in).
     *
     * @param player the name of the player
     * @return true if authenticated
     */
    boolean isAuthenticated(String player);

    /**
     * Forces a player's login without requiring a password.
     *
     * @param player the name of the player
     * @return true if successfully forced login, false if player is not online
     */
    boolean forceLogin(String player);

    /**
     * Changes a player's password.
     *
     * @param player      the name of the player
     * @param newPassword the new password
     * @return true on success
     */
    boolean changePassword(String player, String newPassword);

    /**
     * Register a player.
     *
     * @param player   the name of the player
     * @param password the password to use
     * @return true on success
     */
    boolean performRegister(String player, String password);

    /**
     * Unregister a player.
     *
     * @param player the name of the player
     * @return true on success
     */
    boolean performUnregister(String player);

    /**
     * Returns all accounts associated with a specific IP.
     *
     * @param ip the IP address
     * @return a map of player names and their last login times
     */
    Map<String, Long> getAccountsByIp(String ip);

    /**
     * Returns the count of all accounts stored in the database.
     *
     * @return the total count of accounts
     */
    long getAccountCount();

    /**
     * Get the player account data.
     *
     * @param player the name of the player
     * @return Optional of {@link AccountData}
     */
    Optional<AccountData> getAccount(String player);

    /**
     * Checks if the password provided is valid for a given player.
     *
     * @param player   the name of the player
     * @param password the password to compare
     * @return true if the passwords match
     */
    boolean comparePassword(String player, String password);

    /**
     * Checks if the player is registered in the database.
     *
     * @param player the name of the player
     * @return true if registered
     */
    boolean isRegistered(String player);

}
