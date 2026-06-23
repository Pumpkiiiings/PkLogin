package com.pumpkiiings.pklogin.common.hook;

import java.util.UUID;

public class FloodgateHook {

    private static boolean enabled = false;

    static {
        try {
            Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            enabled = true;
        } catch (ClassNotFoundException e) {
            enabled = false;
        }
    }

    /**
     * Checks if a player is connected through Floodgate (Bedrock).
     * @param uuid the player's UUID
     * @return true if the player is a Bedrock player
     */
    public static boolean isBedrockPlayer(UUID uuid) {
        if (!enabled) return false;
        try {
            return org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(uuid);
        } catch (Exception e) {
            return false;
        }
    }
}
