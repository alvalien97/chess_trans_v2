package com.transcendence.chess.model;

import java.util.List;

public class Rook extends Piece {
    public Rook(int color, int col, int row) {
        super(color, col, row);
        type = Type.ROOK;
    }

    @Override
    public boolean canMove(int targetCol, int targetRow, List<Piece> pieces) {
        if (isWithinBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
            if (targetCol == preCol || targetRow == preRow) {
                if (isValidSquare(targetCol, targetRow, pieces) && !pieceIsOnstraightLine(targetCol, targetRow, pieces)) {
                    return true;
                }
            }
        }
        return false;
    }
}

