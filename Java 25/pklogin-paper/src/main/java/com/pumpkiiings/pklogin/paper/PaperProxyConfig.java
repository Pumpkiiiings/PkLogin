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

    /**
     * True when this server sits behind a proxy, whichever kind. Decides whether
     * PkLogin checks premium accounts here, and is not a preference: behind any
     * forwarding mode the login handshake belongs to the proxy, leaving the
     * backend nothing to verify with. Read from the server's own configuration
     * rather than asked for a second time, so the two cannot disagree.
     */
    public static boolean isBehindProxy() {
        return isVelocityForwardingEnabled() || isBungeeForwardingEnabled();
    }

    /**
     * True when {@code settings.bungeecord} is on in {@code spigot.yml}, the flag
     * BungeeCord's legacy forwarding needs.
     */
    public static boolean isBungeeForwardingEnabled() {
        try {
            return org.bukkit.Bukkit.spigot().getConfig().getBoolean("settings.bungeecord", false);
        } catch (Throwable unavailable) {
            // Not a Spigot-derived server, or called before the server is up.
            return false;
        }
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
