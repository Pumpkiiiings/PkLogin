package com.pumpkiiings.pklogin.paper.packetevents;

import com.pumpkiiings.pklogin.paper.PkLoginPaper;

public class PacketEventsHook {
    public static void init(PkLoginPaper plugin) {
        PacketEventsListener listener = new PacketEventsListener(plugin);
        com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager().registerListener(listener);
    }
}
