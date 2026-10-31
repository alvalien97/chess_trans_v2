package com.transcendence.chess.model;

import java.util.List;

public class Bishop extends Piece {
    public Bishop(int color, int col, int row) {
        super(color, col, row);
        type = Type.BISHOP;
    }

    @Override
    public boolean canMove(int targetCol, int targetRow, List<Piece> pieces) {
        if(isWithinBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)){
            if(Math.abs(targetCol - preCol) == Math.abs(targetRow - preRow)) {
                if(isValidSquare(targetCol, targetRow, pieces) && !pieceIsOnDiagonalLine(targetCol, targetRow, pieces)) {
                    return true;
                }
            }
        }
        return false;
    }
}
