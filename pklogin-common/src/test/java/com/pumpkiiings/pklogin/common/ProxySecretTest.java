package com.pumpkiiings.pklogin.common;

import com.pumpkiiings.pklogin.common.security.ProxyMessageSecurity;
import com.pumpkiiings.pklogin.common.security.ProxySecretResolver;
import com.pumpkiiings.pklogin.common.security.ProxyVerifyProtocol;

/**
 * Checks how the proxy signing key is chosen and that a message signed with it on
 * one side verifies on the other.
 *
 * <p>Run with {@code ./gradlew :pklogin-common:checkProxySecret}.</p>
 */
public final class ProxySecretTest {

    private static int failures = 0;
    private static int checks = 0;

    public static void main(String[] args) {
        derivationIsDeterministic();
        derivationHidesTheForwardingSecret();
        forwardingSecretIsUsedAutomatically();
        nothingAvailableYieldsNothing();
        signedMessageVerifiesAcrossSides();
        connectionCheckRoundTrips();
        connectionCheckRejectsAForeignNetwork();

        System.out.println();
        System.out.println(checks + " check(s), " + failures + " failure(s)");
        if (failures > 0) System.exit(1);
        System.out.println("Proxy secret resolution behaves as promised.");
    }

    /** Both sides derive independently, so the same input must give the same key. */
    private static void derivationIsDeterministic() {
        String a = ProxySecretResolver.derive("Y2FlOGM4ZThi");
        String b = ProxySecretResolver.derive("Y2FlOGM4ZThi");
        check("derivation is deterministic", a.equals(b));
        check("different secrets give different keys",
                !a.equals(ProxySecretResolver.derive("something-else")));
    }

    /** The forwarding secret must not be recoverable from what PkLogin uses. */
    private static void derivationHidesTheForwardingSecret() {
        String forwarding = "Y2FlOGM4ZThi";
        String derived = ProxySecretResolver.derive(forwarding);
        check("derived key is not the forwarding secret", !derived.equals(forwarding));
        check("derived key does not contain it", !derived.contains(forwarding));
    }

    /** The forwarding secret is picked up on its own; there is nothing to configure. */
    private static void forwardingSecretIsUsedAutomatically() {
        ProxySecretResolver.Resolution resolution = ProxySecretResolver.resolve("Y2FlOGM4ZThi");
        check("resolves without any configuration", resolution.isPresent());
        check("uses the derived key",
                ProxySecretResolver.derive("Y2FlOGM4ZThi").equals(resolution.getKey()));
        check("reports the forwarding secret as its source",
                resolution.getSource() != null && resolution.getSource().contains("forwarding"));
    }

    /** No forwarding secret means no key — callers must fail closed. */
    private static void nothingAvailableYieldsNothing() {
        check("no key when nothing is available", !ProxySecretResolver.resolve(null).isPresent());
        check("blank forwarding secret is not a key", !ProxySecretResolver.resolve("   ").isPresent());
    }

    /** The whole point: proxy signs, backend verifies, using only shared inputs. */
    private static void signedMessageVerifiesAcrossSides() {
        String forwardingSecret = "Y2FlOGM4ZThiZGVhZGJlZWY=";

        // Proxy side.
        String proxyKey = ProxySecretResolver.resolve(forwardingSecret).getKey();
        long timestamp = System.currentTimeMillis();
        String nonce = ProxyMessageSecurity.newNonce();
        String signature = ProxyMessageSecurity.sign(proxyKey,
                "PremiumAutoLogin", "Steve", "uuid-1", Long.toString(timestamp), nonce);

        // Backend side, deriving the key from the same forwarding secret.
        String backendKey = ProxySecretResolver.resolve(forwardingSecret).getKey();
        check("both sides derive the same key", proxyKey.equals(backendKey));
        check("signature verifies on the backend",
                ProxyMessageSecurity.verify(backendKey, signature, timestamp, nonce,
                        "PremiumAutoLogin", "Steve", "uuid-1", Long.toString(timestamp), nonce));

        // A backend with a different forwarding secret must reject it.
        String strangerKey = ProxySecretResolver.resolve("a-different-network").getKey();
        check("a foreign network rejects the message",
                !ProxyMessageSecurity.verify(strangerKey, signature, timestamp, ProxyMessageSecurity.newNonce(),
                        "PremiumAutoLogin", "Steve", "uuid-1", Long.toString(timestamp), nonce));
    }

    /**
     * The connection check in full: the proxy challenges, the backend answers, and
     * the proxy accepts the answer.
     */
    private static void connectionCheckRoundTrips() {
        String key = ProxySecretResolver.resolve("Y2FlOGM4ZThiZGVhZGJlZWY=").getKey();

        long sentAt = System.currentTimeMillis();
        String challenge = ProxyMessageSecurity.newNonce();
        String request = ProxyMessageSecurity.sign(key,
                ProxyVerifyProtocol.requestParts("auth", sentAt, challenge));

        check("the backend accepts the check",
                ProxyMessageSecurity.verify(key, request, sentAt, challenge,
                        ProxyVerifyProtocol.requestParts("auth", sentAt, challenge)));

        long repliedAt = System.currentTimeMillis();
        String replyNonce = ProxyMessageSecurity.newNonce();
        String reply = ProxyMessageSecurity.sign(key,
                ProxyVerifyProtocol.replyParts("auth", "2.0.0", challenge, repliedAt, replyNonce));

        check("the proxy accepts the answer",
                ProxyMessageSecurity.verify(key, reply, repliedAt, replyNonce,
                        ProxyVerifyProtocol.replyParts("auth", "2.0.0", challenge, repliedAt, replyNonce)));
    }

    /** A backend on a different forwarding secret must fail the check, not pass it. */
    private static void connectionCheckRejectsAForeignNetwork() {
        String proxyKey = ProxySecretResolver.resolve("Y2FlOGM4ZThiZGVhZGJlZWY=").getKey();
        String backendKey = ProxySecretResolver.resolve("a-different-network").getKey();

        long repliedAt = System.currentTimeMillis();
        String challenge = ProxyMessageSecurity.newNonce();
        String replyNonce = ProxyMessageSecurity.newNonce();
        String reply = ProxyMessageSecurity.sign(backendKey,
                ProxyVerifyProtocol.replyParts("auth", "2.0.0", challenge, repliedAt, replyNonce));

        check("a mismatched key fails the connection check",
                !ProxyMessageSecurity.verify(proxyKey, reply, repliedAt, replyNonce,
                        ProxyVerifyProtocol.replyParts("auth", "2.0.0", challenge, repliedAt, replyNonce)));
    }

    private static void check(String what, boolean ok) {
        checks++;
        if (ok) {
            System.out.println("  PASS  " + what);
        } else {
            failures++;
            System.out.println("  FAIL  " + what);
        }
    }
}
