package com.transcendence.chess.model;

import java.util.List;

public class King extends Piece {
    public Piece castlingP;

    public King(int color, int col, int row) {
        super(color, col, row);
        type = Type.KING;
    }

    private boolean isSquareUnderAttack(int targetCol, int targetRow, List<Piece> pieces) {
        for (Piece piece : pieces) {
            if (piece.color != this.color) {
                if (piece.canMove(targetCol, targetRow, pieces)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean canMove(int targetCol, int targetRow, List<Piece> pieces) {
        if (isWithinBoard(targetCol, targetRow)) {
            if (Math.abs(targetCol - preCol) + Math.abs(targetRow - preRow) == 1 || Math.abs(targetCol - preCol) * Math.abs(targetRow - preRow) == 1) {
                if (isValidSquare(targetCol, targetRow, pieces)) {
                    return true;
                }
            }
            // Lógica de enroque
            if (!moved) {
                if (targetCol == preCol + 2 && targetRow == preRow && !pieceIsOnstraightLine(targetCol, targetRow, pieces)) {
                    if (!isSquareUnderAttack(preCol, preRow, pieces) && !isSquareUnderAttack(preCol + 1, preRow, pieces)) {
                        for (Piece piece : pieces) {
                            if (piece.col == preCol + 3 && piece.row == preRow && !piece.moved) {
                                this.castlingP = piece;
                                return true;
                            }
                        }
                    }
                }
                if (targetCol == preCol - 2 && targetRow == preRow && !pieceIsOnstraightLine(targetCol, targetRow, pieces)) {
                    if (!isSquareUnderAttack(preCol, preRow, pieces) && !isSquareUnderAttack(preCol - 1, preRow, pieces)) {
                        for (Piece piece : pieces) {
                            if (piece.col == preCol - 4 && piece.row == targetRow && !piece.moved) {
                                if (getHittingP(preCol - 1, targetRow, pieces) == null &&
                                        getHittingP(preCol - 2, targetRow, pieces) == null &&
                                        getHittingP(preCol - 3, targetRow, pieces) == null) {
                                    this.castlingP = piece;
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}
