// Agrupa esta clase con las estructuras que representan el estado del juego.
package com.transcendence.chess.model;

import java.util.ArrayList;
import java.util.List;

// Guarda en memoria la información actual de una partida de ajedrez.
public class GameState {
    // Mantiene las piezas en una lista; final impide sustituir la lista, pero permite modificar su contenido.
    private final List<Piece> pieces = new ArrayList<>();
    // Indica el color al que le toca jugar: 0 representa blancas y 1 representa negras.
    private int currentColor = 0; // 0 = Blancas, 1 = Negras
    // Describe la situación de la partida; inicialmente sigue en curso.
    private String status = "PLAYING";
    // Guarda el identificador de quien ofreció tablas, o null si no hay oferta.
    private Long drawOfferByUserId;
    // Guarda el identificador del ganador, o null mientras no haya ganador.
    private Long winnerId;
    // Conserva un texto explicativo del resultado final, si ya existe.
    private String resultMessage;

    // Vacía la posición actual y coloca las 32 piezas en sus casillas iniciales.
    public void initializeBoard() {
        // Elimina cualquier pieza anterior para que el método pueda reiniciar el tablero.
        pieces.clear();

        // Coloca la torre negra en la columna 0 de la fila inicial negra.
        pieces.add(new Rook(1, 0, 0));
        // Coloca el caballo negro junto a la torre.
        pieces.add(new Knight(1, 1, 0));
        // Coloca el alfil negro junto al caballo.
        pieces.add(new Bishop(1, 2, 0));
        // Coloca la dama negra en la columna 3.
        pieces.add(new Queen(1, 3, 0));
        // Coloca el rey negro en la columna 4.
        pieces.add(new King(1, 4, 0));
        // Coloca el segundo alfil negro.
        pieces.add(new Bishop(1, 5, 0));
        // Coloca el segundo caballo negro.
        pieces.add(new Knight(1, 6, 0));
        // Coloca la segunda torre negra.
        pieces.add(new Rook(1, 7, 0));
        // Recorre las ocho columnas de la fila de peones negros.
        for (int i = 0; i < 8; i++) {
            // Crea un peón negro en la columna i y en la fila 1.
            pieces.add(new Pawn(1, i, 1));
        }

        // Recorre las ocho columnas de la fila de peones blancos.
        for (int i = 0; i < 8; i++) {
            // Crea un peón blanco en la columna i y en la fila 6.
            pieces.add(new Pawn(0, i, 6));
        }
        // Coloca la torre blanca en la columna 0 de la fila inicial blanca.
        pieces.add(new Rook(0, 0, 7));
        // Coloca el caballo blanco junto a la torre.
        pieces.add(new Knight(0, 1, 7));
        // Coloca el alfil blanco junto al caballo.
        pieces.add(new Bishop(0, 2, 7));
        // Coloca la dama blanca en la columna 3.
        pieces.add(new Queen(0, 3, 7));
        // Coloca el rey blanco en la columna 4.
        pieces.add(new King(0, 4, 7));
        // Coloca el segundo alfil blanco.
        pieces.add(new Bishop(0, 5, 7));
        // Coloca el segundo caballo blanco.
        pieces.add(new Knight(0, 6, 7));
        // Coloca la segunda torre blanca.
        pieces.add(new Rook(0, 7, 7));
    }

    // Devuelve la lista real de piezas para que otros componentes puedan consultarla o modificarla.
    public List<Piece> getPieces() {
        // Entrega la lista almacenada en este estado de partida.
        return pieces;
    }

    // Devuelve el color que tiene el turno actual.
    public int getCurrentColor() {
        // Entrega el valor 0 para blancas o 1 para negras.
        return currentColor;
    }

    // Cambia el color que tiene el turno actual.
    public void setCurrentColor(int currentColor) {
        // Guarda el nuevo color del turno en el campo de esta partida.
        this.currentColor = currentColor;
    }

    // Devuelve el estado textual de la partida, por ejemplo PLAYING o FINISHED.
    public String getStatus() {
        // Entrega el valor guardado en el campo status.
        return status;
    }

    // Actualiza el estado textual de la partida.
    public void setStatus(String status) {
        // Guarda el parámetro en el campo del objeto; this distingue el campo del parámetro homónimo.
        this.status = status;
    }

    // Devuelve quién ofreció tablas, o null si no hay una oferta pendiente.
    public Long getDrawOfferByUserId() { return drawOfferByUserId; }
    // Guarda el identificador del usuario que ha ofrecido tablas.
    public void setDrawOfferByUserId(Long drawOfferByUserId) { this.drawOfferByUserId = drawOfferByUserId; }
    // Devuelve el identificador del ganador, si se ha determinado.
    public Long getWinnerId() { return winnerId; }
    // Guarda el identificador del ganador de la partida.
    public void setWinnerId(Long winnerId) { this.winnerId = winnerId; }
    // Devuelve el mensaje que explica el resultado, si está disponible.
    public String getResultMessage() { return resultMessage; }
    // Guarda el mensaje explicativo del resultado de la partida.
    public void setResultMessage(String resultMessage) { this.resultMessage = resultMessage; }
}
