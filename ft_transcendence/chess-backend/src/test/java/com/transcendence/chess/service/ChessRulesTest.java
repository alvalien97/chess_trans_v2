package com.transcendence.chess.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.transcendence.chess.model.GameState;
import com.transcendence.chess.model.Piece;
import com.transcendence.chess.model.Type;

class ChessRulesTest {

    @Test
    void appliesLegalMoveAndRejectsIllegalMove() {
        ChessRules rules = new ChessRules();

        assertEquals("Movimiento no válido para esta posición.",
            assertThrows(IllegalArgumentException.class, () -> rules.move(4, 6, 4, 3)).getMessage());
        assertEquals(0, rules.getState().getCurrentColor());

        GameState afterMove = rules.move(4, 6, 4, 4);

        assertEquals(1, afterMove.getCurrentColor());
        assertNull(pieceAt(afterMove, 4, 6));
        assertEquals(Type.PAWN, pieceAt(afterMove, 4, 4).type);
        assertEquals("No es el turno de este jugador.",
            assertThrows(IllegalArgumentException.class, () -> rules.move(4, 4, 4, 2)).getMessage());
        assertEquals(1, rules.getState().getCurrentColor());
    }

    @Test
    void rejectsMoveThatExposesOwnKingToCheck() {
        ChessRules rules = new ChessRules("k3r3/8/8/8/8/8/4R3/4K3 w - - 0 1");

        assertEquals("Movimiento no válido para esta posición.",
            assertThrows(IllegalArgumentException.class, () -> rules.move(4, 6, 5, 6)).getMessage());
        assertEquals(Type.ROOK, pieceAt(rules.getState(), 4, 6).type);
        assertEquals(0, rules.getState().getCurrentColor());
    }

    @Test
    void rejectsMoveThatLeavesKingInCheck() {
        ChessRules rules = new ChessRules();
        playFoolsMate(rules);

        assertEquals("CHECKMATE", rules.getState().getStatus());
        assertEquals("La partida ya ha terminado.",
            assertThrows(IllegalArgumentException.class, () -> rules.move(3, 6, 3, 5)).getMessage());
    }

    @Test
    void performsKingsideCastling() {
        ChessRules rules = new ChessRules();
        play(rules, 4, 6, 4, 4);
        play(rules, 4, 1, 4, 3);
        play(rules, 6, 7, 5, 5);
        play(rules, 1, 0, 2, 2);
        play(rules, 5, 7, 4, 6);
        play(rules, 6, 0, 5, 2);

        GameState state = rules.move(4, 7, 6, 7);

        assertEquals(Type.KING, pieceAt(state, 6, 7).type);
        assertEquals(Type.ROOK, pieceAt(state, 5, 7).type);
    }

    @Test
    void performsEnPassantCapture() {
        ChessRules rules = new ChessRules();
        play(rules, 4, 6, 4, 4);
        play(rules, 0, 1, 0, 2);
        play(rules, 4, 4, 4, 3);
        play(rules, 3, 1, 3, 3);

        GameState state = rules.move(4, 3, 3, 2);

        assertEquals(Type.PAWN, pieceAt(state, 3, 2).type);
        assertNull(pieceAt(state, 3, 3));
    }

    @Test
    void promotesPawnToQueen() {
        ChessRules rules = new ChessRules("7k/P7/8/8/8/8/8/7K w - - 0 1");

        GameState state = rules.move(0, 1, 0, 0);

        assertEquals(Type.QUEEN, pieceAt(state, 0, 0).type);
    }

    private static void playFoolsMate(ChessRules rules) {
        play(rules, 5, 6, 5, 5);
        play(rules, 4, 1, 4, 3);
        play(rules, 6, 6, 6, 4);
        play(rules, 3, 0, 7, 4);
    }

    private static GameState play(ChessRules rules, int fromCol, int fromRow, int toCol, int toRow) {
        return rules.move(fromCol, fromRow, toCol, toRow);
    }

    private static Piece pieceAt(GameState state, int col, int row) {
        return state.getPieces().stream()
                .filter(piece -> piece.col == col && piece.row == row)
                .findFirst()
                .orElse(null);
    }
}