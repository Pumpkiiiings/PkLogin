package com.pumpkiiings.pklogin.api.event.bukkit.auth;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PreLoginEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;

    public PreLoginEvent(Player player) {
        super(true); // Async
        this.player = player;
    }

    public Player getPlayer() { return player; }
    
    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
