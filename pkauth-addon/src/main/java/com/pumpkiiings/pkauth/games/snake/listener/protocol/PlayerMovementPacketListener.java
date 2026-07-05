package com.pumpkiiings.pkauth.games.snake.listener.protocol;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerInput;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import com.pumpkiiings.pkauth.PkAuthAddon;
import com.pumpkiiings.pkauth.games.snake.game.Direction;
import com.pumpkiiings.pkauth.games.snake.game.SnakeGame;

import java.util.HashMap;
import java.util.Map;

public class PlayerMovementPacketListener extends PacketListenerAbstract {

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_INPUT) {
            Player player = event.getPlayer();
            SnakeGame game = SnakeGame.getGame(player);

            if (game == null || !game.isRunning()) {
                return;
            }

            WrapperPlayClientPlayerInput wrapper = new WrapperPlayClientPlayerInput(event);

            if (wrapper.isShift()) {
                Bukkit.getScheduler().runTask(PkAuthAddon.getInstance(), game::stop);
                return;
            }

            Direction direction = null;

            if (wrapper.isForward()) {
                direction = Direction.UP;
            } else if (wrapper.isBackward()) {
                direction = Direction.DOWN;
            } else if (wrapper.isLeft()) {
                direction = Direction.LEFT;
            } else if (wrapper.isRight()) {
                direction = Direction.RIGHT;
            }

            if (direction != null) {
                Direction finalDirection = direction;

                Bukkit.getScheduler().runTask(PkAuthAddon.getInstance(), () -> {
                    game.changeDirection(finalDirection);
                });
            }
        }
    }
}
