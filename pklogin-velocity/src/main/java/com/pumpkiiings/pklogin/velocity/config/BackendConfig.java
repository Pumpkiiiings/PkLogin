package com.pumpkiiings.pklogin.velocity.config;

import com.pumpkiiings.pklogin.common.config.ConfigurationVersionManager;
import com.pumpkiiings.pklogin.common.config.MigrationLogger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The proxy's own settings: which servers players log in on, and where they go
 * afterwards.
 *
 * <p>Separate from {@code config.yml} on purpose. That file describes how accounts
 * and passwords work and every server shares it; this one describes the shape of
 * the network and only the proxy has any use for it.</p>
 */
public class BackendConfig {

    private final Path configPath;
    private final MigrationLogger logger;
    private Map<String, Object> data;

    public BackendConfig(Path configPath, MigrationLogger logger) {
        this.configPath = configPath;
        this.logger = logger;
    }

    /**
     * Creates the file if it is missing, brings it up to the current schema, then
     * reads it.
     *
     * <p>The upgrade runs through the same machinery as {@code config.yml}, so an
     * existing file keeps every value its owner set while gaining any option a new
     * version introduced, comments included.</p>
     */
    public void load() throws Exception {
        newManager().load();

        File file = configPath.toFile();
        try (FileInputStream fis = new FileInputStream(file)) {
            Yaml yaml = new Yaml();
            data = yaml.load(fis);
            if (data == null) {
                data = Collections.emptyMap();
            }
        }
    }

    private ConfigurationVersionManager newManager() {
        InputStream defaults = getClass().getClassLoader().getResourceAsStream("backend.yml");
        return BackendMigrations.applyTo(new ConfigurationVersionManager(
                configPath.toFile(),
                defaults,
                ConfigurationVersionManager.VERSION_KEY_BACKEND,
                BackendMigrations.CURRENT_VERSION,
                logger));
    }

    @SuppressWarnings("unchecked")
    public List<String> getAuthServers() {
        return (List<String>) getNested("backend.auth-servers", Collections.singletonList("auth"));
    }

    /**
     * Defaults to on: a network where the check fails is one where premium players
     * silently cannot log in, which is worth a message at startup.
     */
    public boolean isVerifyConnection() {
        return (boolean) getNested("backend.verify-connection", true);
    }

    public boolean isOverrideFirstServer() {
        return (boolean) getNested("redirect.override-first-server", true);
    }

    public boolean isRedirectToLastServer() {
        return (boolean) getNested("redirect.redirect-to-last-server", false);
    }

    @SuppressWarnings("unchecked")
    public List<String> getAfterAuthServers() {
        return (List<String>) getNested("redirect.after-auth.servers", Collections.singletonList("lobby-1"));
    }
    
    public boolean isAfterAuthEnabled() {
        return (boolean) getNested("redirect.after-auth.enabled", true);
    }

    private Object getNested(String path, Object def) {
        String[] keys = path.split("\\.");
        Map<String, Object> current = data;
        for (int i = 0; i < keys.length - 1; i++) {
            Object obj = current.get(keys[i]);
            if (obj instanceof Map) {
                current = (Map<String, Object>) obj;
            } else {
                return def;
            }
        }
        Object result = current.get(keys[keys.length - 1]);
        return result != null ? result : def;
    }
}
