package com.pumpkiiings.pklogin.paper.autologin.protocollib;

import java.security.PublicKey;
import java.time.Instant;

public class ClientPublicKey {

    private final Instant expiry;
    private final PublicKey key;
    private final byte[] signature;

    public ClientPublicKey(Instant expiry, PublicKey key, byte[] signature) {
        this.expiry = expiry;
        this.key = key;
        this.signature = signature;
    }

    public Instant getExpiry() {
        return expiry;
    }

    public PublicKey getKey() {
        return key;
    }

    public byte[] getSignature() {
        return signature;
    }
}
