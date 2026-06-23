package com.pumpkiiings.pklogin.velocity.config;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BackendConfig {

    private final Path configPath;
    private Map<String, Object> data;

    public BackendConfig(Path configPath) {
        this.configPath = configPath;
    }

    public void load() throws Exception {
        File file = configPath.toFile();
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("backend.yml")) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                } else {
                    file.createNewFile();
                }
            }
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            Yaml yaml = new Yaml();
            data = yaml.load(fis);
            if (data == null) {
                data = Collections.emptyMap();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getAuthServers() {
        return (List<String>) getNested("backend.auth-servers", Collections.singletonList("auth"));
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
