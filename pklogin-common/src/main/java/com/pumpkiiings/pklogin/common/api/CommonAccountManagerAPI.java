package com.pumpkiiings.pklogin.common.api;

import com.pumpkiiings.pklogin.api.AccountData;
import com.pumpkiiings.pklogin.api.service.AccountManagerAPI;
import com.pumpkiiings.pklogin.common.PkLogin;
import com.pumpkiiings.pklogin.common.model.Account;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CommonAccountManagerAPI implements AccountManagerAPI {
    
    public CommonAccountManagerAPI() {}

    @Override
    public CompletableFuture<Optional<AccountData>> getAccount(String name) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Account> account = PkLogin.getAccountManagement().search(name);
            return account.map(acc -> acc);
        });
    }

    @Override
    public CompletableFuture<Optional<AccountData>> getAccount(UUID uuid) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public CompletableFuture<Boolean> isRegistered(String name) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Account> account = PkLogin.getAccountManagement().retrieveOrLoad(name);
            return account.isPresent() && account.get().getHashedPassword() != null && !account.get().getHashedPassword().isEmpty();
        });
    }

    @Override
    public CompletableFuture<Void> deleteAccount(String name) {
        return CompletableFuture.runAsync(() -> {
            PkLogin.getAccountManagement().removePassword(name);
        });
    }
}
