//Agrupa el modelo de dama con las demás piezas del juego.
package com.transcendence.chess.model;

//Proporciona la lista de piezas usada para comprobar ocupaciones.
import java.util.List;

//Pieza que combina los movimientos rectos de torre y diagonales de alfil.
public class Queen extends Piece {
    //Crea una dama con su color y posición inicial.
    public Queen(int color, int col, int row) {
        //Inicializa el estado compartido de la pieza.
        super(color, col, row);
        //Registra el tipo concreto de esta pieza.
        type = Type.QUEEN;
    }

    //Comprueba si la dama puede ir a las coordenadas indicadas.
    @Override
    public boolean canMove(int targetCol, int targetRow, List<Piece> pieces) {
        //El destino debe estar en el tablero y no ser la posición actual.
        if (isWithinBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
            //Primero comprueba los movimientos rectos, como los de una torre.
            if (targetCol == preCol || targetRow == preRow) {
                //El destino debe permitir captura o estar vacío, y el trayecto debe estar despejado.
                if (isValidSquare(targetCol, targetRow, pieces) && !pieceIsOnstraightLine(targetCol, targetRow, pieces)) {
                    return true;
                }
            }
            //Después comprueba los movimientos diagonales, como los de un alfil.
            if (Math.abs(targetCol - preCol) == Math.abs(targetRow - preRow)) {
                //La diagonal también debe tener un destino válido y no estar bloqueada.
                if (isValidSquare(targetCol, targetRow, pieces) && !pieceIsOnDiagonalLine(targetCol, targetRow, pieces)) {
                    return true;
                }
            }
        }
        //Ninguno de los dos patrones de movimiento es válido.
        return false;
    }
}

