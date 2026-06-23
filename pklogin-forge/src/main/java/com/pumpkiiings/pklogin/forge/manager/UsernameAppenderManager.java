package com.pumpkiiings.pklogin.forge.manager;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UsernameAppenderManager {

    private static final Map<SocketAddress, String> hosts = new ConcurrentHashMap<>();

    public static void setHost(SocketAddress address, String host) {
        hosts.put(address, host);
    }

    public static String getHost(SocketAddress address) {
        return hosts.remove(address);
    }
}
