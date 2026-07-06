package com.pumpkiiings.pklogin.api.event.velocity.auth;

import com.velocitypowered.api.proxy.Player;

public class VelocityPlayerPasswordChangeEvent {
    private final Player player;

    public VelocityPlayerPasswordChangeEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() { return player; }
}
