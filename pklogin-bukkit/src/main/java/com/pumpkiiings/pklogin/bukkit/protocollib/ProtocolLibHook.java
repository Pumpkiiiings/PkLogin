package com.pumpkiiings.pklogin.bukkit.protocollib;

import com.pumpkiiings.pklogin.bukkit.PkLoginBukkit;

public class ProtocolLibHook {
    public static void init(PkLoginBukkit plugin) {
        ProtocolLibListener listener = new ProtocolLibListener(plugin);
        com.comphenix.protocol.ProtocolLibrary.getProtocolManager().getAsynchronousManager()
                .registerAsyncHandler(listener).start();
    }
}
