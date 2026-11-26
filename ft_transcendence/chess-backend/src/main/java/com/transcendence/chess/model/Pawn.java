//Agrupa el modelo del peón con las demás piezas.
package com.transcendence.chess.model;

//Proporciona la colección de piezas necesaria para consultar el tablero.
import java.util.List;

//Modelo de peón con comprobaciones locales de sus movimientos característicos.
public class Pawn extends Piece {
    //Crea el peón con su color y posición inicial.
    public Pawn(int color, int col, int row) {
        //Inicializa los campos comunes definidos por Piece.
        super(color, col, row);
        //Marca el tipo concreto para las reglas compartidas de la clase base.
        type = Type.PAWN;
    }

    //Comprueba si el movimiento propuesto es válido según las piezas y coordenadas locales.
    @Override
    public boolean canMove(int targetCol, int targetRow, List<Piece> pieces) {
        //Solo considera destinos del tablero que no sean la casilla de origen.
        if (isWithinBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
            //Las blancas avanzan hacia filas menores y las negras hacia filas mayores.
            int moveValue = 1;
            if (color == 0) {
                moveValue = -1;
            }

            //Averigua si hay una pieza ocupando la casilla de destino.
            hittingP = getHittingP(targetCol, targetRow, pieces);

            //El avance de una casilla hacia delante requiere una casilla vacía.
            if (targetCol == preCol && targetRow == preRow + moveValue && hittingP == null) {
                return true;
            }
            //El primer avance puede ser de dos filas si no se ha movido y el camino está libre.
            if (targetCol == preCol && targetRow == preRow + moveValue * 2 && hittingP == null && !moved && !pieceIsOnstraightLine(targetCol, targetRow, pieces)) {
                return true;
            }
            //La captura diagonal normal exige que el destino contenga una pieza rival.
            if (Math.abs(targetCol - preCol) == 1 && targetRow == preRow + moveValue && hittingP != null && hittingP.color != color) {
                return true;
            }
            //En Passant (captura al paso)
            //Busca la captura especial de un peón rival adyacente que avanzó dos casillas.
            if (Math.abs(targetCol - preCol) == 1 && targetRow == preRow + moveValue) {
                //Examina todas las piezas para localizar al peón adyacente.
                for (Piece piece : pieces) {
                    //El rival debe estar en la columna destino y haber marcado su avance doble.
                    if (piece.col == targetCol && piece.row == preRow && piece.twoStepped) {
                        //Registra la pieza capturada y acepta el movimiento.
                        hittingP = piece;
                        return true;
                    }
                }
            }
        }
        //Ninguna condición anterior aceptó este movimiento.
        return false;
    }
}
