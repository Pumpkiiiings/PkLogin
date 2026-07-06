package com.pumpkiiings.pklogin.api.service;

import java.util.concurrent.CompletableFuture;

public interface SecurityAPI {
    CompletableFuture<Boolean> changePassword(String name, String newPassword);
    CompletableFuture<Boolean> comparePassword(String name, String rawPassword);
}
