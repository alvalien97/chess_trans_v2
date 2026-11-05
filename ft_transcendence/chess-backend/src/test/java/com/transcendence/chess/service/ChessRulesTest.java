// Los tests pertenecen al mismo paquete para probar el constructor FEN de visibilidad de paquete.
package com.transcendence.chess.service;

// Aserciones para comparar resultados, comprobar null y esperar excepciones.
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.transcendence.chess.model.GameState;
import com.transcendence.chess.model.Piece;
import com.transcendence.chess.model.Type;

// Pruebas automatizadas de las reglas que ofrece ChessRules.
class ChessRulesTest {

    // Comprueba una jugada legal, una ilegal y la alternancia de turno.
    @Test
    void appliesLegalMoveAndRejectsIllegalMove() {
        // Crea una partida desde la posición inicial.
        ChessRules rules = new ChessRules();

        // Intentar avanzar un peón tres filas debe fallar y no debe cambiar el turno.
        assertEquals("Movimiento no válido para esta posición.",
            assertThrows(IllegalArgumentException.class, () -> rules.move(4, 6, 4, 3)).getMessage());
        assertEquals(0, rules.getState().getCurrentColor());

        // Avanza el peón blanco dos casillas, que sí es legal en su primer movimiento.
        GameState afterMove = rules.move(4, 6, 4, 4);

        // Tras jugar blancas, el turno corresponde a negras.
        assertEquals(1, afterMove.getCurrentColor());
        // La casilla de origen queda vacía.
        assertNull(pieceAt(afterMove, 4, 6));
        // En el destino hay un peón.
        assertEquals(Type.PAWN, pieceAt(afterMove, 4, 4).type);
        // Las blancas no pueden mover otra vez antes que negras.
        assertEquals("No es el turno de este jugador.",
            assertThrows(IllegalArgumentException.class, () -> rules.move(4, 4, 4, 2)).getMessage());
        assertEquals(1, rules.getState().getCurrentColor());
    }

    // Comprueba que una jugada que expone al propio rey al jaque se rechaza.
    @Test
    void rejectsMoveThatExposesOwnKingToCheck() {
        // Carga una posición FEN donde la torre protege al rey de la torre enemiga.
        ChessRules rules = new ChessRules("k3r3/8/8/8/8/8/4R3/4K3 w - - 0 1");

        // Mover la torre lateralmente abriría la columna y dejaría al rey en jaque.
        assertEquals("Movimiento no válido para esta posición.",
            assertThrows(IllegalArgumentException.class, () -> rules.move(4, 6, 5, 6)).getMessage());
        assertEquals(Type.ROOK, pieceAt(rules.getState(), 4, 6).type);
        assertEquals(0, rules.getState().getCurrentColor());
    }

    // Comprueba que el jaque mate termina el juego y bloquea posteriores movimientos.
    @Test
    void rejectsMoveThatLeavesKingInCheck() {
        // Crea el tablero estándar.
        ChessRules rules = new ChessRules();
        // Ejecuta la secuencia conocida como mate del loco.
        playFoolsMate(rules);

        // Comprueba el estado final.
        assertEquals("CHECKMATE", rules.getState().getStatus());
        // Una partida terminada no permite ejecutar otra jugada.
        assertEquals("La partida ya ha terminado.",
            assertThrows(IllegalArgumentException.class, () -> rules.move(3, 6, 3, 5)).getMessage());
    }

    // Comprueba que el enroque corto desplaza al rey y a la torre.
    @Test
    void performsKingsideCastling() {
        // Prepara las casillas necesarias alternando movimientos legales.
        ChessRules rules = new ChessRules();
        play(rules, 4, 6, 4, 4);
        play(rules, 4, 1, 4, 3);
        play(rules, 6, 7, 5, 5);
        play(rules, 1, 0, 2, 2);
        play(rules, 5, 7, 4, 6);
        play(rules, 6, 0, 5, 2);

        // Ejecuta el enroque corto del rey blanco.
        GameState state = rules.move(4, 7, 6, 7);

        // El rey termina dos columnas a la derecha.
        assertEquals(Type.KING, pieceAt(state, 6, 7).type);
        // La torre pasa a la casilla contigua al rey.
        assertEquals(Type.ROOK, pieceAt(state, 5, 7).type);
    }

    // Comprueba la captura al paso del peón que acaba de avanzar dos casillas.
    @Test
    void performsEnPassantCapture() {
        // Prepara dos peones rivales para la captura especial.
        ChessRules rules = new ChessRules();
        play(rules, 4, 6, 4, 4);
        play(rules, 0, 1, 0, 2);
        play(rules, 4, 4, 4, 3);
        play(rules, 3, 1, 3, 3);

        // El peón blanco captura en diagonal detrás del peón negro.
        GameState state = rules.move(4, 3, 3, 2);

        // El peón blanco ocupa el destino diagonal.
        assertEquals(Type.PAWN, pieceAt(state, 3, 2).type);
        // El peón negro capturado ya no está en su casilla.
        assertNull(pieceAt(state, 3, 3));
    }

    // Comprueba la promoción automática de peón a dama.
    @Test
    void promotesPawnToQueen() {
        // Carga una posición con un peón blanco a una casilla de la última fila.
        ChessRules rules = new ChessRules("7k/P7/8/8/8/8/8/7K w - - 0 1");

        // Avanza el peón hasta la fila final.
        GameState state = rules.move(0, 1, 0, 0);

        // La pieza resultante es una dama.
        assertEquals(Type.QUEEN, pieceAt(state, 0, 0).type);
    }

    // Ejecuta la secuencia del mate del loco usando el helper de movimiento.
    private static void playFoolsMate(ChessRules rules) {
        play(rules, 5, 6, 5, 5);
        play(rules, 4, 1, 4, 3);
        play(rules, 6, 6, 6, 4);
        play(rules, 3, 0, 7, 4);
    }

    // Reduce la repetición al delegar una jugada de coordenadas al motor.
    private static GameState play(ChessRules rules, int fromCol, int fromRow, int toCol, int toRow) {
        return rules.move(fromCol, fromRow, toCol, toRow);
    }

    // Busca y devuelve la pieza que ocupa unas coordenadas, o null si la casilla está vacía.
    private static Piece pieceAt(GameState state, int col, int row) {
        // Recorre las piezas del estado y filtra por columna y fila.
        return state.getPieces().stream()
                // Conserva las piezas situadas en la casilla consultada.
                .filter(piece -> piece.col == col && piece.row == row)
                // Devuelve la primera coincidencia, si existe.
                .findFirst()
                // Usa null para representar una casilla vacía.
                .orElse(null);
    }
}