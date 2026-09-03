package com.pumpkiiings.pklogin.common;

import com.pumpkiiings.pklogin.common.security.ConnectionAuthRegistry;

/** Regression test for the disconnect/reconnect authentication race. */
public final class ConnectionAuthRegistryTest {

    public static void main(String[] args) {
        ConnectionAuthRegistry<Object> registry = new ConnectionAuthRegistry<>();
        Object oldConnection = new Object();
        Object newConnection = new Object();

        registry.begin("Admin", oldConnection);
        require(registry.authenticate("Admin", oldConnection), "old connection should authenticate");
        require(registry.isAuthenticated("admin", oldConnection), "lookup should be case-insensitive");

        registry.begin("ADMIN", newConnection);
        require(!registry.isAuthenticated("Admin", newConnection), "replacement inherited authentication");
        require(!registry.isAuthenticated("Admin", oldConnection), "replaced connection stayed authenticated");
        require(!registry.authenticate("Admin", oldConnection), "late async login authenticated replacement");

        require(registry.authenticate("Admin", newConnection), "replacement could not authenticate normally");
        require(!registry.end("Admin", oldConnection), "late quit was accepted as the current connection");
        require(registry.isAuthenticated("Admin", newConnection), "late quit cleared replacement authentication");

        require(registry.end("Admin", newConnection), "current connection did not end");
        require(!registry.isAuthenticated("Admin", newConnection), "ended connection stayed authenticated");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
