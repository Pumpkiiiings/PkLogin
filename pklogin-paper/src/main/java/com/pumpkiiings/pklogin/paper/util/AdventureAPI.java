package com.pumpkiiings.pklogin.paper.util;

import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import java.time.Duration;

public class AdventureAPI {

    public static Component parse(String text) {
        if (text == null) return Component.empty();
        return LegacyComponentSerializer.legacySection().deserialize(text);
    }

    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(parse(message));
    }

    public static void showTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L)
        );
        Title adventureTitle = Title.title(parse(title), parse(subtitle), times);
        player.showTitle(adventureTitle);
    }

    public static void clearTitle(Player player) {
        player.clearTitle();
    }
}
