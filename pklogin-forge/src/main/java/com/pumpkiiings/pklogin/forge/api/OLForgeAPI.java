package com.pumpkiiings.pklogin.forge.api;

import com.pumpkiiings.pklogin.common.api.PkLoginAPI;
import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.security.hashing.HashStrategyFactory;
import com.pumpkiiings.pklogin.forge.PkLoginForge;
import lombok.NonNull;

import java.util.Optional;

public class OLForgeAPI implements PkLoginAPI {

    private final PkLoginForge plugin;

    public OLForgeAPI(PkLoginForge plugin) {
        this.plugin = plugin;
    }

    @Override
    public AccountManagement getAccountManagement() {
        return plugin.getAccountManagement();
    }

    @Override
    public Optional<Account> getAccount(@NonNull String player) {
        return plugin.getAccountManagement().search(player);
    }

    @Override
    public boolean comparePassword(@NonNull String player, @NonNull String password) {
        AccountManagement accountManagement = plugin.getAccountManagement();
        Optional<Account> account = accountManagement.retrieveOrLoad(player);
        return account.isPresent() && accountManagement.comparePassword(account.get(), password);
    }

    @Override
    public boolean isRegistered(@NonNull String player) {
        return plugin.getAccountManagement().retrieveOrLoad(player).isPresent();
    }

    @Override
    public boolean update(@NonNull String player, @NonNull String password, String address, boolean replace) {
        String hashedPassword = HashStrategyFactory.fromSettings().hash(password);
        return plugin.getAccountManagement().update(player, hashedPassword, address, replace);
    }
}
