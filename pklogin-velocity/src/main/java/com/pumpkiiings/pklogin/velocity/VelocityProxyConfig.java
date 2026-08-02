package com.pumpkiiings.pklogin.velocity;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Reads the proxy's own forwarding configuration.
 *
 * <p>Velocity's public API exposes plenty of {@code ProxyConfig} but not the
 * forwarding secret, so it is read from disk the same way Velocity itself does:
 * the {@code VELOCITY_FORWARDING_SECRET} environment variable takes precedence,
 * otherwise {@code velocity.toml} points at the file holding it.</p>
 */
public final class VelocityProxyConfig {

    private static final String ENV_SECRET = "VELOCITY_FORWARDING_SECRET";
    private static final String DEFAULT_SECRET_FILE = "forwarding.secret";

    private VelocityProxyConfig() {}

    /**
     * @param dataDirectory the plugin's data directory ({@code plugins/pklogin})
     * @return the proxy root directory
     */
    public static Path proxyRoot(Path dataDirectory) {
        // plugins/pklogin -> plugins -> root
        Path plugins = dataDirectory.getParent();
        return plugins == null ? dataDirectory : (plugins.getParent() == null ? plugins : plugins.getParent());
    }

    /**
     * @return the modern-forwarding secret, or null when it is not configured or
     *         the proxy is not using modern forwarding
     */
    public static String readForwardingSecret(Path dataDirectory) {
        String fromEnv = System.getenv(ENV_SECRET);
        if (fromEnv != null && !fromEnv.trim().isEmpty()) {
            return fromEnv.trim();
        }

        Path root = proxyRoot(dataDirectory);
        File toml = root.resolve("velocity.toml").toFile();
        if (!toml.isFile()) {
            return null;
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(toml.toPath(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }

        // Older builds inline the secret; newer ones point at a file.
        String inline = findValue(lines, "forwarding-secret");
        if (inline != null && !inline.isEmpty()) {
            return inline;
        }

        String secretFile = findValue(lines, "forwarding-secret-file");
        if (secretFile == null || secretFile.isEmpty()) {
            secretFile = DEFAULT_SECRET_FILE;
        }

        File file = root.resolve(secretFile).toFile();
        if (!file.isFile()) {
            return null;
        }
        try {
            String secret = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).trim();
            return secret.isEmpty() ? null : secret;
        } catch (Exception e) {
            return null;
        }
    }

    /** True when {@code velocity.toml} selects modern player info forwarding. */
    public static boolean isModernForwarding(Path dataDirectory) {
        File toml = proxyRoot(dataDirectory).resolve("velocity.toml").toFile();
        if (!toml.isFile()) return false;
        try {
            String mode = findValue(Files.readAllLines(toml.toPath(), StandardCharsets.UTF_8),
                    "player-info-forwarding-mode");
            return mode != null && mode.equalsIgnoreCase("modern");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Pulls a top-level {@code key = "value"} out of velocity.toml.
     *
     * <p>Matches the whole key before the {@code =} so that, for example,
     * {@code forwarding-secret-file} is never mistaken for
     * {@code forwarding-secret}, and skips commented-out lines.</p>
     */
    private static String findValue(List<String> lines, String key) {
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            int equals = line.indexOf('=');
            if (equals < 0) continue;
            if (!line.substring(0, equals).trim().equals(key)) continue;

            String value = line.substring(equals + 1).trim();
            // Strip an inline comment outside of quotes, then the quotes.
            if (!value.startsWith("\"") && !value.startsWith("'")) {
                int hash = value.indexOf('#');
                if (hash >= 0) value = value.substring(0, hash).trim();
            }
            if (value.length() >= 2
                    && ((value.startsWith("\"") && value.endsWith("\""))
                     || (value.startsWith("'") && value.endsWith("'")))) {
                value = value.substring(1, value.length() - 1);
            }
            return value.trim();
        }
        return null;
    }
}
