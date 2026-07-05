package com.pumpkiiings.pkauth.games.snake.game;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import com.pumpkiiings.pkauth.PkAuthAddon;
import com.pumpkiiings.pkauth.games.snake.display.CameraController;
import com.pumpkiiings.pkauth.games.snake.display.GameRenderer;
import com.pumpkiiings.pkauth.games.snake.game.entity.Food;
import com.pumpkiiings.pkauth.games.snake.game.entity.Snake;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SnakeGame {

    private static final Map<Player, SnakeGame> activeGames = new HashMap<>();

    private final Player player;
    private final GameBoard board;
    private final Snake snake;
    private final Food food;
    private final GameRenderer renderer;
    private final CameraController cameraController;

    private BukkitTask gameTask;
    private Direction currentDirection = Direction.LEFT;
    private Direction nextDirection = Direction.LEFT;
    private boolean isRunning = false;
    private int score = 0;

    public SnakeGame(Player player) {
        this.player = player;
        this.board = new GameBoard(20, 15);
        this.snake = new Snake(board.getWidth() / 2, board.getHeight() / 2);
        this.food = new Food(board);
        this.renderer = new GameRenderer(player, board);
        this.cameraController = new CameraController(player);

        food.spawn(snake);
    }

    public void start() {
        if (activeGames.containsKey(player)) {
            activeGames.get(player).stop();
        }

        activeGames.put(player, this);
        isRunning = true;

        cameraController.moveToGameView();
        renderer.initialize();
        renderer.render(snake, food, score);

        player.sendActionBar("§7Press §a§lShift §7to exit the game");

        gameTask = Bukkit.getScheduler().runTaskTimer(PkAuthAddon.getInstance(),
                this::tick, 20L, 10L);
    }

    private void tick() {
        if (!isRunning) return;

        currentDirection = nextDirection;

        if (!snake.move(currentDirection, board)) {
            gameOver();
            return;
        }

        if (snake.checkCollision()) {
            gameOver();
            return;
        }

        boolean ate = false;
        if (snake.getHead().equals(food.getPosition())) {
            snake.grow();
            score += 10;
            // play for camera location
            player.playSound(cameraController.getCameraLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 1.0f);


            if (snake.getBody().size() >= board.getWidth() * board.getHeight()) {
                win();
                return;
            }

            food.spawn(snake);
            ate = true;
        }

        renderer.render(snake, ate ? food : null, ate ? score : -1);
    }

    private void win() {
        isRunning = false;
        player.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.sendMessage("§aYou Win! Score: " + score);
        stop();
    }

    public void changeDirection(Direction direction) {
        if (direction.isOpposite(currentDirection)) {
            return;
        }
        this.nextDirection = direction;
    }

    private void gameOver() {
        isRunning = false;
        player.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 0.8f);
        player.sendMessage("§cGame Over! Score: " + score);
        stop();
    }

    public void stop() {
        if (isRunning) {
            isRunning = false;
            player.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 0.8f);
            player.sendMessage("§cGame Over! Score: " + score);
        }

        if (gameTask != null) {
            gameTask.cancel();
        }

        renderer.cleanup();
        cameraController.resetCamera();

        activeGames.remove(player);
    }

    public static SnakeGame getGame(Player player) {
        return activeGames.get(player);
    }

    public static void stopAllGames() {
        new ArrayList<>(activeGames.values()).forEach(SnakeGame::stop);
    }

    public boolean isRunning() {
        return isRunning;
    }
}
