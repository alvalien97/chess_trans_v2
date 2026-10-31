package com.transcendence.chess.model;

import java.util.List;

public class Pawn extends Piece {
    public Pawn(int color, int col, int row) {
        super(color, col, row);
        type = Type.PAWN;
    }

    @Override
    public boolean canMove(int targetCol, int targetRow, List<Piece> pieces) {
        if (isWithinBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
            int moveValue = (color == 0) ? -1 : 1; // 0 para blancas, 1 para negras

            hittingP = getHittingP(targetCol, targetRow, pieces);

            if (targetCol == preCol && targetRow == preRow + moveValue && hittingP == null) {
                return true;
            }
            if (targetCol == preCol && targetRow == preRow + moveValue * 2 && hittingP == null && !moved && !pieceIsOnstraightLine(targetCol, targetRow, pieces)) {
                return true;
            }
            if (Math.abs(targetCol - preCol) == 1 && targetRow == preRow + moveValue && hittingP != null && hittingP.color != color) {
                return true;
            }
            // En Passant (captura al paso)
            if (Math.abs(targetCol - preCol) == 1 && targetRow == preRow + moveValue) {
                for (Piece piece : pieces) {
                    if (piece.col == targetCol && piece.row == preRow && piece.twoStepped) {
                        hittingP = piece;
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
