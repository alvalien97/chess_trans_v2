package com.transcendence.chess.model;

import java.util.List;

public class Knight extends Piece {
    public Knight(int color, int col, int row) {
        super(color, col, row);
        type = Type.KNIGHT;
    }

    @Override
    public boolean canMove(int targetCol, int targetRow, List<Piece> pieces) {
        if (isWithinBoard(targetCol, targetRow)) {
            if (Math.abs(targetCol - preCol) * Math.abs(targetRow - preRow) == 2) {
                if (isValidSquare(targetCol, targetRow, pieces)) {
                    return true;
                }
            }
        }
        return false;
    }
}
