package com.pumpkiiings.pklogin.forge.serializer.chat;

import net.minecraft.network.chat.Component;

public class ChatComponentSerializer {

    public static Component fromText(String text) {
        return Component.literal(text);
    }

    public static Component fromJson(String json) {
        return Component.Serializer.fromJson(json);
    }
}
