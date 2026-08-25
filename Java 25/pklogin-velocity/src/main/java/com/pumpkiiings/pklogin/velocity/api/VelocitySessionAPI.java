package com.pumpkiiings.pklogin.velocity.api;

import com.pumpkiiings.pklogin.api.service.SessionAPI;
import com.pumpkiiings.pklogin.velocity.PkLoginVelocity;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VelocitySessionAPI implements SessionAPI {
    private final PkLoginVelocity plugin;

    public VelocitySessionAPI(PkLoginVelocity plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isAuthenticated(String name) {
        return plugin.getServer().getPlayer(name)
                .map(player -> plugin.getAuthenticatedPlayers().contains(player.getUniqueId()))
                .orElse(false);
    }

    @Override
    public boolean isAuthenticated(UUID uuid) {
        return plugin.getAuthenticatedPlayers().contains(uuid);
    }

    @Override
    public CompletableFuture<Boolean> forceLogin(String name) {
        return CompletableFuture.supplyAsync(() -> {
            plugin.getServer().getPlayer(name).ifPresent(player -> 
                plugin.getAuthenticatedPlayers().add(player.getUniqueId())
            );
            return true;
        });
    }
}
