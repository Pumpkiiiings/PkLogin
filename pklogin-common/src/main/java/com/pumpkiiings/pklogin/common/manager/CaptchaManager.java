package com.pumpkiiings.pklogin.common.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CaptchaManager {

    // Maps player name to the expected Captcha code (if Chat/Map) or simply empty string for Inventory
    private final Map<String, String> pendingCaptcha = new ConcurrentHashMap<>();

    private static CaptchaManager instance;

    public CaptchaManager() {
        instance = this;
    }

    public static CaptchaManager getInstance() {
        if (instance == null) {
            instance = new CaptchaManager();
        }
        return instance;
    }

    /**
     * Mark a player as needing to complete a captcha.
     * @param playerName The player's name
     * @param code The expected code (if applicable, or empty string for Inventory captcha)
     */
    public void addPending(String playerName, String code) {
        pendingCaptcha.put(playerName.toLowerCase(), code);
    }

    public void removePending(String playerName) {
        pendingCaptcha.remove(playerName.toLowerCase());
    }

    public boolean isPending(String playerName) {
        return pendingCaptcha.containsKey(playerName.toLowerCase());
    }

    public String getExpectedCode(String playerName) {
        return pendingCaptcha.get(playerName.toLowerCase());
    }
}
