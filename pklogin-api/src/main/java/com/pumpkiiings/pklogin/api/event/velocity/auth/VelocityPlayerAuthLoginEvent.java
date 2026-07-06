package com.pumpkiiings.pklogin.api.event.velocity.auth;

import com.velocitypowered.api.proxy.Player;

public class VelocityPlayerAuthLoginEvent {
    private final Player player;

    public VelocityPlayerAuthLoginEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() { return player; }
}
