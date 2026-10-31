package com.transcendence.chess.service;

import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import com.transcendence.chess.model.Bishop;
import com.transcendence.chess.model.GameState;
import com.transcendence.chess.model.King;
import com.transcendence.chess.model.Knight;
import com.transcendence.chess.model.Pawn;
import com.transcendence.chess.model.Piece;
import com.transcendence.chess.model.Queen;
import com.transcendence.chess.model.Rook;

public class ChessRules {
    private final com.github.bhlangonijr.chesslib.Board board;

    public ChessRules() {
        board = new com.github.bhlangonijr.chesslib.Board();
    }

    ChessRules(String initialFen) {
        board = new com.github.bhlangonijr.chesslib.Board();
        board.loadFromFen(initialFen);
    }

    public synchronized GameState getState() {
        GameState state = new GameState();
        state.setCurrentColor(board.getSideToMove() == Side.WHITE ? 0 : 1);
        state.setStatus(getStatus());

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Square square = squareAt(col, row);
                com.github.bhlangonijr.chesslib.Piece boardPiece = board.getPiece(square);
                if (boardPiece != com.github.bhlangonijr.chesslib.Piece.NONE) {
                    state.getPieces().add(toModelPiece(boardPiece, col, row));
                }
            }
        }

        return state;
    }

    public synchronized GameState move(int fromCol, int fromRow, int toCol, int toRow) {
        Square from = squareAt(fromCol, fromRow);
        Square to = squareAt(toCol, toRow);
        com.github.bhlangonijr.chesslib.Piece movingPiece = board.getPiece(from);

        if (movingPiece == com.github.bhlangonijr.chesslib.Piece.NONE) {
            throw new IllegalArgumentException("No hay ninguna pieza en la casilla de origen.");
        }
        if (movingPiece.getPieceSide() != board.getSideToMove()) {
            throw new IllegalArgumentException("No es el turno de este jugador.");
        }
        if (board.isMated() || board.isDraw()) {
            throw new IllegalArgumentException("La partida ya ha terminado.");
        }

        com.github.bhlangonijr.chesslib.Piece promotion = com.github.bhlangonijr.chesslib.Piece.NONE;
        if (movingPiece.getPieceType() == PieceType.PAWN
                && ((board.getSideToMove() == Side.WHITE && toRow == 0)
                || (board.getSideToMove() == Side.BLACK && toRow == 7))) {
            promotion = com.github.bhlangonijr.chesslib.Piece.make(board.getSideToMove(), PieceType.QUEEN);
        }

        Move move = new Move(from, to, promotion);
        if (!board.legalMoves().contains(move) || !board.doMove(move, true)) {
            throw new IllegalArgumentException("Movimiento no válido para esta posición.");
        }

        return getState();
    }

    private String getStatus() {
        if (board.isMated()) {
            return "CHECKMATE";
        }
        if (board.isStaleMate()) {
            return "STALEMATE";
        }
        if (board.isDraw()) {
            return "DRAW";
        }
        if (board.isKingAttacked()) {
            return "CHECK";
        }
        return "PLAYING";
    }

    private Square squareAt(int col, int row) {
        if (col < 0 || col > 7 || row < 0 || row > 7) {
            throw new IllegalArgumentException("La casilla está fuera del tablero.");
        }
        return Square.valueOf(String.valueOf((char) ('A' + col)) + (8 - row));
    }

    private Piece toModelPiece(com.github.bhlangonijr.chesslib.Piece piece, int col, int row) {
        int color = piece.getPieceSide() == Side.WHITE ? 0 : 1;
        return switch (piece.getPieceType()) {
            case KING -> new King(color, col, row);
            case QUEEN -> new Queen(color, col, row);
            case ROOK -> new Rook(color, col, row);
            case BISHOP -> new Bishop(color, col, row);
            case KNIGHT -> new Knight(color, col, row);
            case PAWN -> new Pawn(color, col, row);
            default -> throw new IllegalStateException("Tipo de pieza no soportado: " + piece);
        };
    }
}