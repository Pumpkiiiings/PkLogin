package com.pumpkiiings.pkauth.games.snake.display;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import com.pumpkiiings.pkauth.games.snake.game.GameBoard;
import com.pumpkiiings.pkauth.games.snake.game.entity.Food;
import com.pumpkiiings.pkauth.games.snake.game.entity.Snake;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class GameRenderer {

    private final Player player;
    private final GameBoard board;
    private final List<WrapperEntity> borderDisplays;
    private final List<WrapperEntity> snakeDisplays;
    private final List<WrapperEntity> foodDisplays;
    private final List<WrapperEntity> scoreDisplays;
    private final Location renderLocation;

    private Snake.Position lastFoodPosition;
    private int lastScore = 0;
    private int lastSnakeSize = 0;

    private static final double CELL_SIZE = 0.3;
    private static final double DISTANCE_FROM_CAMERA = 5.0;

    public GameRenderer(Player player, GameBoard board) {
        this.player = player;
        this.board = board;
        this.borderDisplays = new ArrayList<>();
        this.snakeDisplays = new ArrayList<>();
        this.foodDisplays = new ArrayList<>();
        this.scoreDisplays = new ArrayList<>();

        Location playerOriginalLoc = player.getLocation();

        double halfWidth = (board.getWidth() - 1) / 2.0;
        double halfHeight = (board.getHeight() - 1) / 2.0;

        this.renderLocation = new Location(
                player.getWorld(),
                playerOriginalLoc.getX() - halfWidth * CELL_SIZE,
                300 + halfHeight * CELL_SIZE,
                playerOriginalLoc.getZ() + DISTANCE_FROM_CAMERA
        );
    }

    public void initialize() {
        renderBorder();
    }


    private void renderBorder() {
        for (int x = -1; x <= board.getWidth(); x++) {
            spawnCell(x, -1, "§7█", borderDisplays);
            spawnCell(x, board.getHeight(), "§7█", borderDisplays);
        }

        for (int y = 0; y < board.getHeight(); y++) {
            spawnCell(-1, y, "§7█", borderDisplays);
            spawnCell(board.getWidth(), y, "§7█", borderDisplays);
        }
    }

    public void render(Snake snake, Food food, int score) {
        LinkedList<Snake.Position> body = snake.getBody();
        Snake.Position currentHead = body.getFirst();
        int currentSize = body.size();

        if (lastSnakeSize == 0) {
            for (Snake.Position segment : body) {
                spawnCell(segment.x, segment.y, "§a█", snakeDisplays, false);
            }
            lastSnakeSize = currentSize;
        } else {
            boolean grewThisTick = currentSize > lastSnakeSize;

            if (!grewThisTick && !snakeDisplays.isEmpty()) {
                WrapperEntity tailDisplay = snakeDisplays.removeLast();
                tailDisplay.despawn();
            }

            spawnCell(currentHead.x, currentHead.y, "§a█", snakeDisplays, true);
            lastSnakeSize = currentSize;
        }

        if (food != null && (lastFoodPosition == null || !lastFoodPosition.equals(food.getPosition()))) {
            foodDisplays.forEach(WrapperEntity::despawn);
            foodDisplays.clear();

            lastFoodPosition = food.getPosition();
            spawnCellTransparent(lastFoodPosition.x, lastFoodPosition.y, "§c♦", foodDisplays);
        }

        if (score >= 0 && lastScore != score) {
            scoreDisplays.forEach(WrapperEntity::despawn);
            scoreDisplays.clear();

            lastScore = score;
            renderScore(lastScore);
        } else if (scoreDisplays.isEmpty() && score >= 0) {
            lastScore = score;
            renderScore(lastScore);
        }
    }

    private void spawnCell(int x, int y, String symbol, List<WrapperEntity> targetList, boolean addToFront) {
        Location loc = renderLocation.clone().add(x * CELL_SIZE, -y * CELL_SIZE, 0);
        com.github.retrooper.packetevents.protocol.world.Location peLocation =
                SpigotConversionUtil.fromBukkitLocation(loc);

        WrapperEntity entity = new WrapperEntity(EntityTypes.TEXT_DISPLAY);
        TextDisplayMeta meta = entity.getEntityMeta(TextDisplayMeta.class);

        meta.setText(Component.text(symbol));
        meta.setBillboardConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
        meta.setViewRange(50f);

        entity.spawn(peLocation);
        entity.addViewer(player.getUniqueId());

        if (addToFront) {
            targetList.add(0, entity);
        } else {
            targetList.add(entity);
        }
    }

    private void spawnCell(int x, int y, String symbol, List<WrapperEntity> targetList) {
        spawnCell(x, y, symbol, targetList, false);
    }

    private void spawnCellTransparent(int x, int y, String symbol, List<WrapperEntity> targetList) {
        Location loc = renderLocation.clone().add(x * CELL_SIZE, -y * CELL_SIZE, 0);
        com.github.retrooper.packetevents.protocol.world.Location peLocation =
                SpigotConversionUtil.fromBukkitLocation(loc);

        WrapperEntity entity = new WrapperEntity(EntityTypes.TEXT_DISPLAY);
        TextDisplayMeta meta = entity.getEntityMeta(TextDisplayMeta.class);

        meta.setText(Component.text(symbol));
        meta.setBillboardConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
        meta.setViewRange(50f);
        meta.setBackgroundColor(0);

        entity.spawn(peLocation);
        entity.addViewer(player.getUniqueId());

        targetList.add(entity);
    }

    private void renderScore(int score) {
        Location scoreLoc = renderLocation.clone().add(0, 1, 0);
        com.github.retrooper.packetevents.protocol.world.Location peLocation =
                SpigotConversionUtil.fromBukkitLocation(scoreLoc);

        WrapperEntity entity = new WrapperEntity(EntityTypes.TEXT_DISPLAY);
        TextDisplayMeta meta = entity.getEntityMeta(TextDisplayMeta.class);

        meta.setText(Component.text("Score: " + score, NamedTextColor.GOLD));
        meta.setBillboardConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
        meta.setViewRange(50f);

        entity.spawn(peLocation);
        entity.addViewer(player.getUniqueId());

        scoreDisplays.add(entity);
    }

    public void cleanup() {
        borderDisplays.forEach(WrapperEntity::despawn);
        borderDisplays.clear();

        snakeDisplays.forEach(WrapperEntity::despawn);
        snakeDisplays.clear();

        foodDisplays.forEach(WrapperEntity::despawn);
        foodDisplays.clear();

        scoreDisplays.forEach(WrapperEntity::despawn);
        scoreDisplays.clear();
    }
}
