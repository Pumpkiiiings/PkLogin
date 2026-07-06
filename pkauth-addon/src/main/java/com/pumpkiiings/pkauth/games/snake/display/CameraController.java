package com.pumpkiiings.pkauth.games.snake.display;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCamera;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

public class CameraController {

    private final Player player;
    private org.bukkit.entity.Entity cameraEntity;
    private Location originalLocation;
    private GameMode originalGameMode;

    public CameraController(Player player) {
        this.player = player;
    }

    public void moveToGameView() {
        originalLocation = player.getLocation().clone();
        originalGameMode = player.getGameMode();

        player.setGameMode(GameMode.SPECTATOR);

        Location cameraLoc = new Location(
                player.getWorld(),
                originalLocation.getX(),
                300,
                originalLocation.getZ()
        );
        cameraLoc.setYaw(0f);
        cameraLoc.setPitch(0f);

        cameraEntity = player.getWorld().spawn(cameraLoc, org.bukkit.entity.Interaction.class, interaction -> {
            interaction.setInteractionWidth(0f);
            interaction.setInteractionHeight(0f);
        });

        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user != null) {
            WrapperPlayServerCamera packet = new WrapperPlayServerCamera(cameraEntity.getEntityId());
            user.sendPacket(packet);
        }
    }

    public void resetCamera() {
        if (cameraEntity != null) {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
            if (user != null) {
                WrapperPlayServerCamera resetPacket = new WrapperPlayServerCamera(player.getEntityId());
                user.sendPacket(resetPacket);
            }

            cameraEntity.remove();
            cameraEntity = null;
        }

        if (originalGameMode != null) {
            player.setGameMode(originalGameMode);
        }
    }

    public Location getCameraLocation() {
        return cameraEntity != null ? cameraEntity.getLocation() : player.getEyeLocation();
    }


}
