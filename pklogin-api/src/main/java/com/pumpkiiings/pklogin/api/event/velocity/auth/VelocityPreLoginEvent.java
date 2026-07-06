package com.pumpkiiings.pklogin.api.event.velocity.auth;

import com.velocitypowered.api.proxy.Player;

public class VelocityPreLoginEvent {
    private final Player player;

    public VelocityPreLoginEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() { return player; }
}
