package com.pumpkiiings.pklogin.api.event.velocity.auth;

public class VelocityPreLoginEvent {
    private final String username;

    public VelocityPreLoginEvent(String username) {
        this.username = username;
    }

    public String getUsername() { return username; }
}
