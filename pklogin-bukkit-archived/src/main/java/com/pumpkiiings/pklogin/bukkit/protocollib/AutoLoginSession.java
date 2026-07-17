package com.pumpkiiings.pklogin.bukkit.protocollib;

import java.util.UUID;

public class AutoLoginSession {
    private final String username;
    private final byte[] verifyToken;
    private final ClientPublicKey clientKey;
    private boolean verified;
    private UUID premiumUUID;

    public AutoLoginSession(String username, byte[] verifyToken, ClientPublicKey clientKey) {
        this.username = username;
        this.verifyToken = verifyToken;
        this.clientKey = clientKey;
        this.verified = false;
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
