package com.pumpkiiings.pklogin.common.api;

import com.pumpkiiings.pklogin.api.service.SessionAPI;
import com.pumpkiiings.pklogin.common.PkLogin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Name-only session lookups shared by platforms that have no player registry.
 *
 * @deprecated authentication state is keyed by player name, so this class cannot
 *             answer {@link #isAuthenticated(UUID)}. Platforms must register
 *             their own implementation (see {@code BukkitSessionAPI} and
 *             {@code VelocitySessionAPI}) instead of this one.
 */
@Deprecated
public class CommonSessionAPI implements SessionAPI {

    public CommonSessionAPI() {}

    @Override
    public boolean isAuthenticated(String name) {
        return PkLogin.getLoginManagement().isAuthenticated(name);
    }

    /**
     * @throws UnsupportedOperationException always — resolving a UUID to a player
     *         requires the platform's registry, which this class does not have.
     *         Silently returning {@code false} here made callers treat every
     *         authenticated player as unauthenticated.
     */
    @Override
    public boolean isAuthenticated(UUID uuid) {
        throw new UnsupportedOperationException(
                "UUID lookups need a platform-specific SessionAPI; register BukkitSessionAPI or VelocitySessionAPI.");
    }

    @Override
    public CompletableFuture<Boolean> forceLogin(String name) {
        return CompletableFuture.supplyAsync(() -> {
            PkLogin.getLoginManagement().setAuthenticated(name);
            return true;
        });
    }
}
