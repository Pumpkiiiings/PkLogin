package com.pumpkiiings.pklogin.velocity.config;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

public class YamlConfig {

    private Map<String, Object> data;

    public YamlConfig(File file) {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = new FileInputStream(file)) {
            data = yaml.load(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Object get(String path) {
        if (data == null) return null;
        String[] keys = path.split("\\.");
        Map<String, Object> currentMap = data;
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            if (i == keys.length - 1) {
                return currentMap.get(key);
            } else {
                Object next = currentMap.get(key);
                if (next instanceof Map) {
                    currentMap = (Map<String, Object>) next;
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    public String getString(String path) {
        Object obj = get(path);
        return obj != null ? obj.toString() : null;
    }

    public int getInt(String path, int def) {
        Object obj = get(path);
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        return def;
    }

    public boolean isSet(String path) {
        return get(path) != null;
    }

    public java.util.List<String> getStringList(String path) {
        Object obj = get(path);
        if (obj instanceof java.util.List) {
            return (java.util.List<String>) obj;
        }
        return new java.util.ArrayList<>();
    }
}
