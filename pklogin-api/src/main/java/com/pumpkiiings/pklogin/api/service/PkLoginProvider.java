package com.pumpkiiings.pklogin.api.service;

import com.pumpkiiings.pklogin.api.exception.PluginNotLoadedException;

public class PkLoginProvider {
    private static AccountManagerAPI accountManager;
    private static SecurityAPI securityAPI;
    private static SessionAPI sessionAPI;

    public static void registerAccountManager(AccountManagerAPI api) { accountManager = api; }
    public static void registerSecurityAPI(SecurityAPI api) { securityAPI = api; }
    public static void registerSessionAPI(SessionAPI api) { sessionAPI = api; }

    public static AccountManagerAPI getAccountManager() {
        if (accountManager == null) throw new PluginNotLoadedException("AccountManagerAPI is not registered yet.");
        return accountManager;
    }

    public static SecurityAPI getSecurityAPI() {
        if (securityAPI == null) throw new PluginNotLoadedException("SecurityAPI is not registered yet.");
        return securityAPI;
    }

    public static SessionAPI getSessionAPI() {
        if (sessionAPI == null) throw new PluginNotLoadedException("SessionAPI is not registered yet.");
        return sessionAPI;
    }
}
