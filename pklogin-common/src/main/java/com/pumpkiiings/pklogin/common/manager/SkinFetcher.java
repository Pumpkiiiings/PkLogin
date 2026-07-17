package com.pumpkiiings.pklogin.common.manager;

import com.pumpkiiings.pklogin.common.http.HttpClient;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkinFetcher {
    private static final Map<UUID, SkinData> CACHE = new ConcurrentHashMap<>();

    @Getter
    @AllArgsConstructor
    public static class SkinData {
        private final String value;
        private final String signature;
    }

    public static SkinData fetchSkin(UUID realUuid) {
        if (realUuid == null) return null;
        
        if (CACHE.containsKey(realUuid)) {
            return CACHE.get(realUuid);
        }

        try {
            String url = "https://sessionserver.mojang.com/session/minecraft/profile/" + realUuid.toString().replace("-", "") + "?unsigned=false";
            String response = HttpClient.DEFAULT.get(url);
            
            if (response != null && response.contains("\"name\" : \"textures\"") || response.contains("\"name\":\"textures\"")) {
                String value = extractJsonValue(response, "\"value\"");
                String signature = extractJsonValue(response, "\"signature\"");
                
                if (value != null && signature != null) {
                    SkinData data = new SkinData(value, signature);
                    CACHE.put(realUuid, data);
                    return data;
                }
            }
        } catch (Exception ignored) {
            // Rate limited, offline mojang servers, or player has no skin
        }
        return null;
    }

    private static String extractJsonValue(String json, String key) {
        int index = json.indexOf(key);
        if (index == -1) return null;
        
        // Find the colon after the key
        int colonIndex = json.indexOf(":", index + key.length());
        if (colonIndex == -1) return null;
        
        int quoteStart = json.indexOf("\"", colonIndex);
        if (quoteStart == -1) return null;
        
        int quoteEnd = json.indexOf("\"", quoteStart + 1);
        if (quoteEnd == -1) return null;
        
        return json.substring(quoteStart + 1, quoteEnd);
    }
}
