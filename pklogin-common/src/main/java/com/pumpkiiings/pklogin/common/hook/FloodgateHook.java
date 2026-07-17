package com.pumpkiiings.pklogin.common.hook;

import java.util.UUID;

public class FloodgateHook {

    private static boolean floodgateEnabled = false;
    private static boolean geyserEnabled = false;

    static {
        try {
            Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            floodgateEnabled = true;
        } catch (ClassNotFoundException ignored) {}
        
        try {
            Class.forName("org.geysermc.geyser.api.GeyserApi");
            geyserEnabled = true;
        } catch (ClassNotFoundException ignored) {}
    }

    /**
     * Checks if a player is connected through Bedrock (Floodgate or Geyser).
     * @param uuid the player's UUID
     * @return true if the player is a Bedrock player
     */
    public static boolean isBedrockPlayer(UUID uuid) {
        if (floodgateEnabled) {
            try {
                if (org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(uuid)) {
                    return true;
                }
            } catch (Exception ignored) {}
        }
        
        if (geyserEnabled) {
            try {
                if (org.geysermc.geyser.api.GeyserApi.api().isBedrockPlayer(uuid)) {
                    return true;
                }
            } catch (Exception ignored) {}
        }
        
        return false;
    }
}
