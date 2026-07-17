package com.pumpkiiings.pklogin.translator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class TranslationCache {
    private final File cacheFile;
    private final Gson gson;
    private Map<String, String> cache;

    public TranslationCache(File cacheFile) {
        this.cacheFile = cacheFile;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.cache = new HashMap<>();
    }

    public void load() {
        if (!cacheFile.exists()) {
            return;
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(cacheFile), StandardCharsets.UTF_8)) {
            java.lang.reflect.Type type = new TypeToken<Map<String, String>>() {}.getType();
            cache = gson.fromJson(reader, type);
            if (cache == null) {
                cache = new HashMap<>();
            }
        } catch (Exception e) {
            System.err.println("Failed to load cache: " + e.getMessage());
        }
    }

    public void save() {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(cacheFile), StandardCharsets.UTF_8)) {
            gson.toJson(cache, writer);
        } catch (Exception e) {
            System.err.println("Failed to save cache: " + e.getMessage());
        }
    }

    public String getTranslation(String provider, String model, String sourceLang, String targetLang, String originalText) {
        String key = generateKey(provider, model, sourceLang, targetLang, originalText);
        return cache.get(key);
    }

    public void putTranslation(String provider, String model, String sourceLang, String targetLang, String originalText, String translatedText) {
        String key = generateKey(provider, model, sourceLang, targetLang, originalText);
        cache.put(key, translatedText);
    }

    private String generateKey(String provider, String model, String sourceLang, String targetLang, String originalText) {
        String hash = md5(originalText);
        return String.format("%s_%s_%s_%s_%s", provider, model, sourceLang, targetLang, hash);
    }

    private String md5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
