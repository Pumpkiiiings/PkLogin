package com.pumpkiiings.pklogin.forge.ui.title;

import com.pumpkiiings.pklogin.common.model.Title;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

public class TitleAPI {

    private static final TitleAPI instance = new TitleAPI();

    public static TitleAPI getApi() {
        return instance;
    }

    public void send(ServerPlayer player, Title title) {
        if (title == null) return;
        
        player.connection.send(new ClientboundSetTitlesAnimationPacket(title.start, title.duration, title.end));
        
        if (!title.title.isEmpty()) {
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title.title)));
        }
        if (!title.subtitle.isEmpty()) {
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(title.subtitle)));
        }
    }

    public void reset(ServerPlayer player) {
        // A reset can be sending an empty title/subtitle with 0 durations
        player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 0, 0));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("")));
    }
}
