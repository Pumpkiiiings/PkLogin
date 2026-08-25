package com.pumpkiiings.pklogin.paper.packet;

import java.security.PublicKey;
import java.time.Instant;

/**
 * A client's chat signing key, as sent during login on the versions that had
 * one.
 *
 * <p>Used by the ProtocolLib and PacketEvents backends. The native handshake
 * does not read it: it skips the version-specific tail of the login packet
 * rather than parsing it, so clients that answer with a signed nonce fall back
 * to a password there.</p>
 */
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
