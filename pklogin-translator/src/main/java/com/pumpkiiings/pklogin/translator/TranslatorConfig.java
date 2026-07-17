package com.pumpkiiings.pklogin.translator;

import dev.dejvokep.boostedyaml.YamlDocument;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TranslatorConfig {
    private String provider;
    private String apiKey;
    private String model;
    private String sourceLanguage;
    private Map<String, String> files;

    public static TranslatorConfig load(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("Configuration file not found: " + file.getAbsolutePath());
        }

        YamlDocument doc = YamlDocument.create(file);
        TranslatorConfig config = new TranslatorConfig();
        config.provider = doc.getString("provider", "gemini");
        config.apiKey = doc.getString("api-key", "");
        config.model = doc.getString("model", "gemini-1.5-flash");
        config.sourceLanguage = doc.getString("source-language", "en");

        config.files = new HashMap<>();
        if (doc.getSection("files") != null) {
            for (Object key : doc.getSection("files").getKeys()) {
                config.files.put(key.toString(), doc.getString("files." + key));
            }
        }

        return config;
    }

    public String getProvider() {
        return provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public Map<String, String> getFiles() {
        return files;
    }
}
