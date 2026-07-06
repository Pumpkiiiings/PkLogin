package com.pumpkiiings.pklogin.api.service;

import com.pumpkiiings.pklogin.api.AccountData;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface AccountManagerAPI {
    CompletableFuture<Optional<AccountData>> getAccount(String name);
    CompletableFuture<Optional<AccountData>> getAccount(UUID uuid);
    CompletableFuture<Boolean> isRegistered(String name);
    CompletableFuture<Void> deleteAccount(String name);
}
