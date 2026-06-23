package com.pumpkiiings.pklogin.forge.ui.chat;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ActionbarAPI {

    private static final ActionbarAPI instance = new ActionbarAPI();

    public static ActionbarAPI getApi() {
        return instance;
    }

    public void send(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal(message), true);
    }
}
