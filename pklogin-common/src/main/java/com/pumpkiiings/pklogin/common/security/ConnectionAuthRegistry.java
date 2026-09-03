package com.pumpkiiings.pklogin.common.security;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authentication state bound to a concrete live connection.
 *
 * <p>Names and offline UUIDs are reusable, so neither identifies a login
 * session. Values are deliberately compared by identity: a replacement
 * connection may represent the same account but must authenticate again.</p>
 */
public final class ConnectionAuthRegistry<C> {

    private final ConcurrentHashMap<String, C> active = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, C> authenticated = new ConcurrentHashMap<>();

    public synchronized void begin(String name, C connection) {
        String key = key(name);
        active.put(key, connection);
        authenticated.remove(key);
    }

    public synchronized boolean authenticate(String name, C connection) {
        String key = key(name);
        if (active.get(key) != connection) return false;
        authenticated.put(key, connection);
        return true;
    }

    public boolean isAuthenticated(String name, C connection) {
        String key = key(name);
        return active.get(key) == connection && authenticated.get(key) == connection;
    }

    public synchronized boolean end(String name, C connection) {
        String key = key(name);
        if (!active.remove(key, connection)) return false;
        authenticated.remove(key, connection);
        return true;
    }

    public synchronized void invalidate(String name) {
        authenticated.remove(key(name));
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
