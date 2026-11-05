// Agrupa la clase base de las piezas con el resto de modelos del ajedrez.
package com.transcendence.chess.model;

// Lista de piezas que representa las ocupaciones del tablero.
import java.util.List;

// Clase base con datos de posición y comprobaciones compartidas por las piezas.
public class Piece {
    // Tipo concreto de pieza, usado por las subclases y reglas auxiliares.
    public Type type;
    // Coordenadas actuales (col, row) y anteriores (preCol, preRow) de la pieza.
    public int col, row, preCol, preRow;
    // Color de la pieza: 0 para blancas y 1 para negras.
    public int color; // 0 para blancas, 1 para negras
    // Referencia temporal a la pieza encontrada en una casilla de destino o en un trayecto.
    public Piece hittingP;
    // moved registra si se movió; twoStepped registra el avance doble de un peón.
    public boolean moved, twoStepped;

    // Inicializa los datos comunes de una pieza en su casilla inicial.
    public Piece(int color, int col, int row) {
        // Guarda el color recibido.
        this.color = color;
        // Guarda la columna actual.
        this.col = col;
        // Guarda la fila actual.
        this.row = row;
        // Al comenzar, la posición previa coincide con la inicial.
        preCol = col;
        // Al comenzar, la fila previa coincide con la inicial.
        preRow = row;
    }

    // Actualiza la posición y conserva la anterior para las reglas que dependen de ella.
    public void updatePosition(int newCol, int newRow) {
        // Solo los peones pueden registrar el avance especial de dos casillas.
        if (type == Type.PAWN) {
            // Marca el avance doble si la diferencia entre filas es exactamente dos.
            if (Math.abs(newRow - preRow) == 2) {
                twoStepped = true;
            }
        }
        // Conserva la columna actual antes de reemplazarla.
        preCol = this.col;
        // Conserva la fila actual antes de reemplazarla.
        preRow = this.row;
        // Guarda la nueva columna.
        this.col = newCol;
        // Guarda la nueva fila.
        this.row = newRow;
        // Indica que la pieza ya ha realizado al menos un movimiento.
        moved = true;
    }

    // Restaura la ubicación a las coordenadas anteriores almacenadas.
    public void resetPosition() {
        // Recupera la columna anterior.
        col = preCol;
        // Recupera la fila anterior.
        row = preRow;
    }

    // Método base: las subclases deben proporcionar sus propias reglas de movimiento.
    public boolean canMove(int targetCol, int targetRow, List<Piece> pieces) {
        // La clase genérica no representa un tipo concreto capaz de moverse.
        return false;
    }

    // Comprueba que ambas coordenadas destino estén dentro de un tablero de 8 por 8.
    public boolean isWithinBoard(int targetCol, int targetRow) {
        // Los límites son inclusivos porque los índices válidos van de 0 a 7.
        return targetCol >= 0 && targetCol <= 7 && targetRow >= 0 && targetRow <= 7;
    }

    // Comprueba si el destino coincide con la posición anterior de esta pieza.
    public boolean isSameSquare(int targetCol, int targetRow) {
        // Devuelve true solo si coinciden tanto columna como fila.
        return targetCol == preCol && targetRow == preRow;
    }

    // Busca qué pieza, si alguna, ocupa la casilla destino.
    public Piece getHittingP(int targetCol, int targetRow, List<Piece> pieces) {
        // Revisa cada pieza que forma parte del tablero.
        for (Piece piece : pieces) {
            // Ignora esta misma instancia y devuelve otra pieza en las coordenadas buscadas.
            if (piece.col == targetCol && piece.row == targetRow && piece != this) {
                return piece;
            }
        }
        // Si el bucle terminó, la casilla está vacía o solo contiene esta pieza.
        return null;
    }

    // Comprueba si el destino está libre o contiene una pieza rival.
    public boolean isValidSquare(int targetCol, int targetRow, List<Piece> pieces) {
        // Guarda en hittingP la pieza que ocupa el destino, o null si está vacío.
        hittingP = getHittingP(targetCol, targetRow, pieces);
        // Una casilla vacía es válida como destino.
        if (hittingP == null) {
            return true;
        } else {
            // No se puede terminar en una pieza propia; sí se puede capturar una rival.
            return hittingP.color != this.color;
        }
    }

    // Busca piezas que bloqueen el recorrido horizontal o vertical entre origen y destino.
    public boolean pieceIsOnstraightLine(int targetCol, int targetRow, List<Piece> pieces) {
        // Recorre las columnas entre origen y destino cuando se avanza hacia la izquierda.
        for (int c = preCol - 1; c > targetCol; c--) {
            // Comprueba cada pieza para ver si ocupa la columna intermedia en la fila destino.
            for (Piece piece : pieces) {
                // Si la encuentra, guarda el obstáculo y termina la búsqueda.
                if (piece.col == c && piece.row == targetRow) {
                    hittingP = piece;
                    return true;
                }
            }
        }
        // Recorre las columnas intermedias cuando se avanza hacia la derecha.
        for (int c = preCol + 1; c < targetCol; c++) {
            // Examina las piezas de la lista en cada columna intermedia.
            for (Piece piece : pieces) {
                // Detecta y registra una pieza que obstruye la fila del destino.
                if (piece.col == c && piece.row == targetRow) {
                    hittingP = piece;
                    return true;
                }
            }
        }
        // Recorre las filas intermedias cuando se avanza hacia una fila menor.
        for (int r = preRow - 1; r > targetRow; r--) {
            // Examina las piezas de la lista en cada fila intermedia.
            for (Piece piece : pieces) {
                // Detecta una pieza situada en la columna destino y fila intermedia.
                if (piece.col == targetCol && piece.row == r) {
                    hittingP = piece;
                    return true;
                }
            }
        }
        // Recorre las filas intermedias cuando se avanza hacia una fila mayor.
        for (int r = preRow + 1; r < targetRow; r++) {
            // Examina las piezas de la lista en cada fila intermedia.
            for (Piece piece : pieces) {
                // Detecta una pieza situada en la columna destino y fila intermedia.
                if (piece.col == targetCol && piece.row == r) {
                    hittingP = piece;
                    return true;
                }
            }
        }
        // No se encontró ninguna pieza en las casillas intermedias.
        return false;
    }

    // Busca si alguna pieza bloquea el recorrido diagonal entre el origen y el destino.
    public boolean pieceIsOnDiagonalLine(int targetCol, int targetRow, List<Piece> pieces) {
        // Gestiona diagonales que avanzan hacia filas menores.
        if (targetRow < preRow) {
            // Recorre columnas intermedias hacia la izquierda.
            for (int c = preCol - 1; c > targetCol; c--) {
                // Calcula cuántas columnas separan esta casilla del origen.
                int diff = Math.abs(c - preCol);
                // Comprueba todas las piezas en la casilla diagonal correspondiente.
                for (Piece piece : pieces) {
                    // Si está ocupada, registra la pieza que bloquea el movimiento.
                    if (piece.col == c && piece.row == preRow - diff) {
                        hittingP = piece;
                        return true;
                    }
                }
            }
            // Recorre columnas intermedias hacia la derecha.
            for (int c = preCol + 1; c < targetCol; c++) {
                // Calcula cuántas columnas separan esta casilla del origen.
                int diff = Math.abs(c - preCol);
                // Comprueba todas las piezas en la casilla diagonal correspondiente.
                for (Piece piece : pieces) {
                    // Si está ocupada, registra la pieza que bloquea el movimiento.
                    if (piece.col == c && piece.row == preRow - diff) {
                        hittingP = piece;
                        return true;
                    }
                }
            }
        }
        // Gestiona diagonales que avanzan hacia filas mayores.
        if (targetRow > preRow) {
            // Recorre columnas intermedias hacia la izquierda.
            for (int c = preCol - 1; c > targetCol; c--) {
                // Calcula cuántas columnas separan esta casilla del origen.
                int diff = Math.abs(c - preCol);
                // Comprueba todas las piezas en la casilla diagonal correspondiente.
                for (Piece piece : pieces) {
                    // Si está ocupada, registra la pieza que bloquea el movimiento.
                    if (piece.col == c && piece.row == preRow + diff) {
                        hittingP = piece;
                        return true;
                    }
                }
            }
            // Recorre columnas intermedias hacia la derecha.
            for (int c = preCol + 1; c < targetCol; c++) {
                // Calcula cuántas columnas separan esta casilla del origen.
                int diff = Math.abs(c - preCol);
                // Comprueba todas las piezas en la casilla diagonal correspondiente.
                for (Piece piece : pieces) {
                    // Si está ocupada, registra la pieza que bloquea el movimiento.
                    if (piece.col == c && piece.row == preRow + diff) {
                        hittingP = piece;
                        return true;
                    }
                }
            }
        }
        // No se encontró ninguna pieza entre el origen y el destino diagonal.
        return false;
    }
}
