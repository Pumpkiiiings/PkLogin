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

        String actionBar = PkAuthAddon.getInstance().getConfig().getString("snake.messages.actionbar_exit", "<#AAAAAA>Presiona <#55FF55><b>Shift</b> <#AAAAAA>para salir del juego");
        player.sendActionBar(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(actionBar));

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
            String eatSound = PkAuthAddon.getInstance().getConfig().getString("snake.sounds.eat", "ENTITY_PLAYER_BURP");
            try {
                player.playSound(cameraController.getCameraLocation(), Sound.valueOf(eatSound), 1.0f, 1.0f);
            } catch (Exception ignored) {}


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
        String winSound = PkAuthAddon.getInstance().getConfig().getString("snake.sounds.win", "UI_TOAST_CHALLENGE_COMPLETE");
        try { player.playSound(player, Sound.valueOf(winSound), 1.0f, 1.0f); } catch (Exception ignored) {}
        
        String winMsg = PkAuthAddon.getInstance().getConfig().getString("snake.messages.win", "<#55FF55>¡Ganaste! Puntaje: %score%");
        player.sendRichMessage(winMsg.replace("%score%", String.valueOf(score)));
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
        String goSound = PkAuthAddon.getInstance().getConfig().getString("snake.sounds.game_over", "BLOCK_AMETHYST_BLOCK_BREAK");
        try { player.playSound(player, Sound.valueOf(goSound), 1.0f, 0.8f); } catch (Exception ignored) {}
        
        String goMsg = PkAuthAddon.getInstance().getConfig().getString("snake.messages.game_over", "<#FF5555>¡Juego Terminado! Puntaje: %score%");
        player.sendRichMessage(goMsg.replace("%score%", String.valueOf(score)));
        stop();
    }

    public void stop() {
        if (isRunning) {
            isRunning = false;
            String goSound = PkAuthAddon.getInstance().getConfig().getString("snake.sounds.game_over", "BLOCK_AMETHYST_BLOCK_BREAK");
            try { player.playSound(player, Sound.valueOf(goSound), 1.0f, 0.8f); } catch (Exception ignored) {}
            
            String goMsg = PkAuthAddon.getInstance().getConfig().getString("snake.messages.game_over", "<#FF5555>¡Juego Terminado! Puntaje: %score%");
            player.sendRichMessage(goMsg.replace("%score%", String.valueOf(score)));
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
