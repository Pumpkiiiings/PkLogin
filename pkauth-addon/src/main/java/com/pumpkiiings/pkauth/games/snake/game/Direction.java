package com.pumpkiiings.pkauth.games.snake.game;

public enum Direction {
    UP(0, -1),
    DOWN(0, 1),
    RIGHT(-1, 0),
    LEFT(1, 0);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public int getDx() {
        return dx;
    }

    public int getDy() {
        return dy;
    }

    public boolean isOpposite(Direction other) {
        return (this == UP && other == DOWN) ||
                (this == DOWN && other == UP) ||
                (this == RIGHT && other == LEFT) ||
                (this == LEFT && other == RIGHT);
    }
}
