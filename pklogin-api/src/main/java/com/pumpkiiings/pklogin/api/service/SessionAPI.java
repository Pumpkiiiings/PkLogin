package com.pumpkiiings.pklogin.api.service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface SessionAPI {
    boolean isAuthenticated(String name);
    boolean isAuthenticated(UUID uuid);
    CompletableFuture<Boolean> forceLogin(String name);
}
