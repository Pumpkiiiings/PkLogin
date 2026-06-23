package com.pumpkiiings.pklogin.paper.protocollib;

import com.pumpkiiings.pklogin.paper.PkLoginPaper;

public class ProtocolLibHook {
    public static void init(PkLoginPaper plugin) {
        ProtocolLibListener listener = new ProtocolLibListener(plugin);
        com.comphenix.protocol.ProtocolLibrary.getProtocolManager().getAsynchronousManager()
                .registerAsyncHandler(listener).start();
    }
}
