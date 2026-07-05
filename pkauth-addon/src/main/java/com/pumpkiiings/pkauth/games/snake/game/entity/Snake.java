package com.pumpkiiings.pkauth.games.snake.game.entity;

import com.pumpkiiings.pkauth.games.snake.game.Direction;
import com.pumpkiiings.pkauth.games.snake.game.GameBoard;

import java.util.LinkedList;
import java.util.Objects;

public class Snake {

    private final LinkedList<Position> body;

    public Snake(int startX, int startY) {
        body = new LinkedList<>();
        body.add(new Position(startX, startY));
        body.add(new Position(startX - 1, startY));
        body.add(new Position(startX - 2, startY));
    }

    public boolean move(Direction direction, GameBoard board) {
        Position head = body.getFirst();
        Position newHead = new Position(
                head.x + direction.getDx(),
                head.y + direction.getDy()
        );

        if (!board.isInBounds(newHead.x, newHead.y)) {
            return false;
        }

        body.addFirst(newHead);
        body.removeLast();

        return true;
    }

    public boolean checkCollision() {
        Position head = body.getFirst();
        return body.stream().skip(1).anyMatch(pos -> pos.equals(head));
    }

    public void grow() {
        Position tail = body.getLast();
        body.addLast(new Position(tail.x, tail.y));
    }

    public Position getHead() {
        return body.getFirst();
    }

    public LinkedList<Position> getBody() {
        return body;
    }

    public static class Position {
        public final int x;
        public final int y;

        public Position(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Position position)) return false;
            return x == position.x && y == position.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }
}
