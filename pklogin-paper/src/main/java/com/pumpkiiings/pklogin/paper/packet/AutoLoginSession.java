package com.pumpkiiings.pklogin.paper.packet;

import java.util.UUID;

/**
 * One in-flight or completed premium handshake, keyed by address on the plugin.
 *
 * <p>Written by whichever handshake backend is in use and read by
 * {@code PlayerJoinListeners}, which is what makes this the handover point
 * between the packet layer and the rest of the plugin — and why it belongs to
 * none of the backends in particular.</p>
 */
public class AutoLoginSession {
    private final String username;
    private final byte[] verifyToken;
    private final ClientPublicKey clientKey;
    private final long createdAt;
    private boolean verified;
    private UUID premiumUUID;

    public AutoLoginSession(String username, byte[] verifyToken, ClientPublicKey clientKey) {
        this.username = username;
        this.verifyToken = verifyToken;
        this.clientKey = clientKey;
        this.createdAt = System.currentTimeMillis();
        this.verified = false;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Sessions are keyed by IP address, so one left behind by a player who
     * dropped between the Mojang handshake and joining could later be picked up
     * by anyone sharing that address. They are only valid briefly.
     */
    public boolean isExpired(long ttlMillis) {
        return System.currentTimeMillis() - createdAt > ttlMillis;
    }

    public String getUsername() {
        return username;
    }

    public byte[] getVerifyToken() {
        return verifyToken;
    }

    public ClientPublicKey getClientKey() {
        return clientKey;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public UUID getPremiumUUID() {
        return premiumUUID;
    }

    public void setPremiumUUID(UUID premiumUUID) {
        this.premiumUUID = premiumUUID;
    }
}
