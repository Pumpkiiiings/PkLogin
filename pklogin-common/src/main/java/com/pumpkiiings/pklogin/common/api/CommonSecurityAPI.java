package com.pumpkiiings.pklogin.common.api;

import com.pumpkiiings.pklogin.api.service.SecurityAPI;
import com.pumpkiiings.pklogin.common.PkLogin;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.security.hashing.HashStrategyFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CommonSecurityAPI implements SecurityAPI {
    
    public CommonSecurityAPI() {}

    @Override
    public CompletableFuture<Boolean> changePassword(String name, String newPassword) {
        return CompletableFuture.supplyAsync(() -> {
            String hashedPassword = HashStrategyFactory.fromSettings().hash(newPassword);
            return PkLogin.getAccountManagement().update(name, hashedPassword, null, true);
        });
    }

    @Override
    public CompletableFuture<Boolean> comparePassword(String name, String rawPassword) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Account> account = PkLogin.getAccountManagement().retrieveOrLoad(name);
            return account.isPresent() && PkLogin.getAccountManagement().comparePassword(account.get(), rawPassword);
        });
    }
}
