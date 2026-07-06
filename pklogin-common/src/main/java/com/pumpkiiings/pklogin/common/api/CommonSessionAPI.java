package com.pumpkiiings.pklogin.common.api;

import com.pumpkiiings.pklogin.api.service.SessionAPI;
import com.pumpkiiings.pklogin.common.PkLogin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CommonSessionAPI implements SessionAPI {
    
    public CommonSessionAPI() {}

    @Override
    public boolean isAuthenticated(String name) {
        return PkLogin.getLoginManagement().isAuthenticated(name);
    }

    @Override
    public boolean isAuthenticated(UUID uuid) {
        return false;
    }

    @Override
    public CompletableFuture<Boolean> forceLogin(String name) {
        return CompletableFuture.supplyAsync(() -> {
            PkLogin.getLoginManagement().setAuthenticated(name);
            return true;
        });
    }
}
