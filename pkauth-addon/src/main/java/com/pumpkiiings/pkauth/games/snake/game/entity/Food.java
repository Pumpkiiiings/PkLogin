package com.pumpkiiings.pkauth.games.snake.game.entity;

import com.pumpkiiings.pkauth.games.snake.game.GameBoard;

import java.util.Random;

public class Food {

    private final GameBoard board;
    private final Random random;
    private Snake.Position position;

    public Food(GameBoard board) {
        this.board = board;
        this.random = new Random();
    }

    public void spawn(Snake snake) {
        do {
            position = new Snake.Position(
                    random.nextInt(board.getWidth()),
                    random.nextInt(board.getHeight())
            );
        } while (snake.getBody().contains(position));
    }

    public Snake.Position getPosition() {
        return position;
    }
}
