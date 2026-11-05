// Paquete de objetos que reciben o devuelven datos de la API.
package com.transcendence.chess.dto;

// Cuerpo de mensaje que representa un movimiento con coordenadas de origen y destino.
public class MoveDTO {
    // Columna inicial de la pieza, normalmente entre 0 y 7.
    private int fromCol;
    // Fila inicial de la pieza, normalmente entre 0 y 7.
    private int fromRow;
    // Columna a la que se quiere mover.
    private int toCol;
    // Fila a la que se quiere mover.
    private int toRow;

    // Devuelve la columna de origen.
    public int getFromCol() {
        return fromCol;
    }

    // Actualiza la columna de origen con el dato recibido.
    public void setFromCol(int fromCol) {
        this.fromCol = fromCol;
    }

    // Devuelve la fila de origen.
    public int getFromRow() {
        return fromRow;
    }

    // Actualiza la fila de origen con el dato recibido.
    public void setFromRow(int fromRow) {
        this.fromRow = fromRow;
    }

    // Devuelve la columna de destino.
    public int getToCol() {
        return toCol;
    }

    // Actualiza la columna de destino con el dato recibido.
    public void setToCol(int toCol) {
        this.toCol = toCol;
    }

    // Devuelve la fila de destino.
    public int getToRow() {
        return toRow;
    }

    // Actualiza la fila de destino con el dato recibido.
    public void setToRow(int toRow) {
        this.toRow = toRow;
    }
}
