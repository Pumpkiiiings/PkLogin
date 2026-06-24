package com.pumpkiiings.pklogin.bukkit.api;

import com.pumpkiiings.pklogin.api.AccountData;
import com.pumpkiiings.pklogin.api.PkLoginAPI;
import com.pumpkiiings.pklogin.bukkit.PkLoginBukkit;
import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.security.hashing.HashStrategyFactory;

import java.util.Map;
import java.util.Optional;

public class OLBukkitAPI implements PkLoginAPI {

    private final PkLoginBukkit plugin;

    public OLBukkitAPI(PkLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isAuthenticated(String player) {
        return plugin.getLoginManagement().isAuthenticated(player);
    }

    @Override
    public boolean forceLogin(String player) {
        plugin.getLoginManagement().setAuthenticated(player);
        return true;
    }

    @Override
    public boolean changePassword(String player, String newPassword) {
        String hashedPassword = HashStrategyFactory.fromSettings().hash(newPassword);
        return plugin.getAccountManagement().update(player, hashedPassword, null, true);
    }

    @Override
    public boolean performRegister(String player, String password) {
        String hashedPassword = HashStrategyFactory.fromSettings().hash(password);
        return plugin.getAccountManagement().update(player, hashedPassword, null, false);
    }

    @Override
    public boolean performUnregister(String player) {
        return plugin.getAccountManagement().removePassword(player);
    }

    @Override
    public Map<String, Long> getAccountsByIp(String ip) {
        return plugin.getAccountManagement().getAccountsByIp(ip);
    }

    @Override
    public long getAccountCount() {
        return plugin.getRegisteredUsers();
    }

    @Override
    public Optional<AccountData> getAccount(String player) {
        return plugin.getAccountManagement().search(player).map(acc -> acc);
    }

    @Override
    public boolean comparePassword(String player, String password) {
        AccountManagement accountManagement = plugin.getAccountManagement();
        Optional<Account> account = accountManagement.retrieveOrLoad(player);
        return account.isPresent() && accountManagement.comparePassword(account.get(), password);
    }

    @Override
    public boolean isRegistered(String player) {
        Optional<Account> accountOpt = plugin.getAccountManagement().retrieveOrLoad(player);
        return accountOpt.isPresent() && accountOpt.get().getHashedPassword() != null && !accountOpt.get().getHashedPassword().isEmpty();
    }
}
