package com.transcendence.chess.model;

import java.util.List;

public class Piece {
    public Type type;
    public int col, row, preCol, preRow;
    public int color; // 0 para blancas, 1 para negras
    public Piece hittingP;
    public boolean moved, twoStepped;

    public Piece(int color, int col, int row) {
        this.color = color;
        this.col = col;
        this.row = row;
        preCol = col;
        preRow = row;
    }

    public void updatePosition(int newCol, int newRow) {
        if (type == Type.PAWN) {
            if (Math.abs(newRow - preRow) == 2) {
                twoStepped = true;
            }
        }
        preCol = this.col;
        preRow = this.row;
        this.col = newCol;
        this.row = newRow;
        moved = true;
    }

    public void resetPosition() {
        col = preCol;
        row = preRow;
    }

    public boolean canMove(int targetCol, int targetRow, List<Piece> pieces) {
        return false;
    }

    public boolean isWithinBoard(int targetCol, int targetRow) {
        return targetCol >= 0 && targetCol <= 7 && targetRow >= 0 && targetRow <= 7;
    }

    public boolean isSameSquare(int targetCol, int targetRow) {
        return targetCol == preCol && targetRow == preRow;
    }

    public Piece getHittingP(int targetCol, int targetRow, List<Piece> pieces) {
        for (Piece piece : pieces) {
            if (piece.col == targetCol && piece.row == targetRow && piece != this) {
                return piece;
            }
        }
        return null;
    }

    public boolean isValidSquare(int targetCol, int targetRow, List<Piece> pieces) {
        hittingP = getHittingP(targetCol, targetRow, pieces);
        if (hittingP == null) {
            return true;
        } else {
            return hittingP.color != this.color;
        }
    }

    public boolean pieceIsOnstraightLine(int targetCol, int targetRow, List<Piece> pieces) {
        for (int c = preCol - 1; c > targetCol; c--) {
            for (Piece piece : pieces) {
                if (piece.col == c && piece.row == targetRow) {
                    hittingP = piece;
                    return true;
                }
            }
        }
        for (int c = preCol + 1; c < targetCol; c++) {
            for (Piece piece : pieces) {
                if (piece.col == c && piece.row == targetRow) {
                    hittingP = piece;
                    return true;
                }
            }
        }
        for (int r = preRow - 1; r > targetRow; r--) {
            for (Piece piece : pieces) {
                if (piece.col == targetCol && piece.row == r) {
                    hittingP = piece;
                    return true;
                }
            }
        }
        for (int r = preRow + 1; r < targetRow; r++) {
            for (Piece piece : pieces) {
                if (piece.col == targetCol && piece.row == r) {
                    hittingP = piece;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean pieceIsOnDiagonalLine(int targetCol, int targetRow, List<Piece> pieces) {
        if (targetRow < preRow) {
            for (int c = preCol - 1; c > targetCol; c--) {
                int diff = Math.abs(c - preCol);
                for (Piece piece : pieces) {
                    if (piece.col == c && piece.row == preRow - diff) {
                        hittingP = piece;
                        return true;
                    }
                }
            }
            for (int c = preCol + 1; c < targetCol; c++) {
                int diff = Math.abs(c - preCol);
                for (Piece piece : pieces) {
                    if (piece.col == c && piece.row == preRow - diff) {
                        hittingP = piece;
                        return true;
                    }
                }
            }
        }
        if (targetRow > preRow) {
            for (int c = preCol - 1; c > targetCol; c--) {
                int diff = Math.abs(c - preCol);
                for (Piece piece : pieces) {
                    if (piece.col == c && piece.row == preRow + diff) {
                        hittingP = piece;
                        return true;
                    }
                }
            }
            for (int c = preCol + 1; c < targetCol; c++) {
                int diff = Math.abs(c - preCol);
                for (Piece piece : pieces) {
                    if (piece.col == c && piece.row == preRow + diff) {
                        hittingP = piece;
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
