package com.pumpkiiings.pklogin.bukkit.packetevents;

import com.pumpkiiings.pklogin.bukkit.PkLoginBukkit;

public class PacketEventsHook {
    public static void init(PkLoginBukkit plugin) {
        PacketEventsListener listener = new PacketEventsListener(plugin);
        com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager().registerListener(listener);
    }
}
