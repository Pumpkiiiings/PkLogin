package com.pumpkiiings.pklogin.paper;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * Reads the proxy settings the server itself is configured with.
 *
 * <p>Paper keeps these in {@code config/paper-global.yml}, relative to the server
 * directory. They tell us two things PkLogin needs: whether this backend is
 * actually behind Velocity, and the modern-forwarding secret it shares with the
 * proxy, which doubles as the key for signing PkLogin's own messages.</p>
 */
public final class PaperProxyConfig {

    private static final File PAPER_GLOBAL = new File("config/paper-global.yml");

    private PaperProxyConfig() {}

    /** True if this file exists at all; older servers configure proxies elsewhere. */
    public static boolean isAvailable() {
        return PAPER_GLOBAL.isFile();
    }

    /**
     * @return the Velocity modern-forwarding secret, or null when forwarding is
     *         off, unconfigured, or the file cannot be read
     */
    public static String readVelocityForwardingSecret() {
        YamlConfiguration config = load();
        if (config == null) return null;
        if (!config.getBoolean("proxies.velocity.enabled", false)) return null;

        String secret = config.getString("proxies.velocity.secret", "");
        return secret == null || secret.trim().isEmpty() ? null : secret.trim();
    }

    /** True when this backend is set up to accept Velocity modern forwarding. */
    public static boolean isVelocityForwardingEnabled() {
        YamlConfiguration config = load();
        return config != null && config.getBoolean("proxies.velocity.enabled", false);
    }

    private static YamlConfiguration load() {
        if (!PAPER_GLOBAL.isFile()) return null;
        try {
            return YamlConfiguration.loadConfiguration(PAPER_GLOBAL);
        } catch (Exception ignored) {
            return null;
        }
    }
}
