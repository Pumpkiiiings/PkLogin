package com.pumpkiiings.pklogin.forge.task;

import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.common.settings.Settings;
import com.pumpkiiings.pklogin.forge.PkLoginForge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ForgeLoginQueue {

    private static final Map<String, QueueData> QUEUE = new ConcurrentHashMap<>();
    private static int tickCounter = 0;

    public static void addToQueue(String name, boolean registered) {
        QUEUE.put(name, new QueueData(System.currentTimeMillis(), registered));
    }

    public static void removeFromQueue(String name) {
        QUEUE.remove(name);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        tickCounter++;
        // Run every 20 ticks (1 second roughly)
        if (tickCounter % 20 != 0) return;

        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        long currentTime = System.currentTimeMillis();

        QUEUE.entrySet().removeIf(entry -> {
            String name = entry.getKey();
            QueueData data = entry.getValue();

            // Check if player is still online
            ServerPlayer player = server.getPlayerList().getPlayerByName(name);
            if (player == null || PkLoginForge.getInstance().getLoginManagement().isAuthenticated(name)) {
                return true; // remove from queue
            }

            int delay = Settings.TIME_TO_LOGIN.asInt();
            long elapsedSeconds = (currentTime - data.joinTime) / 1000;
            long remaining = delay - elapsedSeconds;
            
            if (remaining <= 0) {
                String kickMessage = data.registered ? Messages.DELAY_KICK_LOGIN.asString() : Messages.DELAY_KICK_REGISTER.asString();
                player.connection.disconnect(Component.literal(kickMessage));
                return true;
            }

            // Send actionbar message
            if (Settings.UI_ACTION_BAR.asBoolean()) {
                String actionbarMessage = Messages.QUEUE_MESSAGE.asString()
                        .replace("{0}", String.valueOf(remaining))
                        .replace("{1}", data.registered ? "login" : "register");
                com.pumpkiiings.pklogin.forge.ui.chat.ActionbarAPI.getApi().send(player, actionbarMessage);
            }

            return false;
        });
    }

    private static class QueueData {
        long joinTime;
        boolean registered;

        QueueData(long joinTime, boolean registered) {
            this.joinTime = joinTime;
            this.registered = registered;
        }
    }
}
