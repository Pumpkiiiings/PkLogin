package com.pumpkiiings.pklogin.common;

import com.pumpkiiings.pklogin.common.security.ProxyMessageSecurity;
import com.pumpkiiings.pklogin.common.security.ProxySecretResolver;
import com.pumpkiiings.pklogin.common.settings.Settings;

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
        forwardingSecretIsUsedWhenNoOverride();
        explicitOverrideWins();
        nothingConfiguredYieldsNothing();
        signedMessageVerifiesAcrossSides();

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

    /** With no override, the forwarding secret is picked up automatically. */
    private static void forwardingSecretIsUsedWhenNoOverride() {
        Settings.clear();
        Settings.define(Settings.PROXY_SECRET, "");

        ProxySecretResolver.Resolution resolution = ProxySecretResolver.resolve("Y2FlOGM4ZThi");
        check("resolves without any configuration", resolution.isPresent());
        check("uses the derived key",
                ProxySecretResolver.derive("Y2FlOGM4ZThi").equals(resolution.getKey()));
        check("reports the forwarding secret as its source",
                resolution.getSource() != null && resolution.getSource().contains("forwarding"));
    }

    /** An explicit proxy-secret takes precedence over auto-detection. */
    private static void explicitOverrideWins() {
        Settings.clear();
        Settings.define(Settings.PROXY_SECRET, "  my-manual-secret  ");

        ProxySecretResolver.Resolution resolution = ProxySecretResolver.resolve("Y2FlOGM4ZThi");
        check("override wins over forwarding secret", "my-manual-secret".equals(resolution.getKey()));
        check("override is reported as the source",
                resolution.getSource() != null && resolution.getSource().contains("config.yml"));
    }

    /** Neither source available means no key — callers must fail closed. */
    private static void nothingConfiguredYieldsNothing() {
        Settings.clear();
        Settings.define(Settings.PROXY_SECRET, "");

        check("no key when nothing is available", !ProxySecretResolver.resolve(null).isPresent());
        check("blank forwarding secret is not a key", !ProxySecretResolver.resolve("   ").isPresent());
    }

    /** The whole point: proxy signs, backend verifies, using only shared inputs. */
    private static void signedMessageVerifiesAcrossSides() {
        Settings.clear();
        Settings.define(Settings.PROXY_SECRET, "");

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
