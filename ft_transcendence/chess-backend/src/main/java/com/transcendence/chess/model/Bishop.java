//Agrupa el modelo de alfil con las demás piezas del juego.
package com.transcendence.chess.model;

//Proporciona la lista de piezas usada para comprobar ocupaciones.
import java.util.List;

//Pieza que se desplaza en diagonal.
public class Bishop extends Piece {
    //Crea un alfil con su color y posición inicial.
    public Bishop(int color, int col, int row) {
        //Inicializa el estado compartido de la pieza.
        super(color, col, row);
        //Registra el tipo concreto de esta pieza.
        type = Type.BISHOP;
    }

    //Comprueba si el alfil puede ir a las coordenadas indicadas.
    @Override
    public boolean canMove(int targetCol, int targetRow, List<Piece> pieces) {
        //El destino debe estar en el tablero y no ser la posición actual.
        if(isWithinBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)){
            //Una diagonal tiene el mismo desplazamiento absoluto en columna y fila.
            if(Math.abs(targetCol - preCol) == Math.abs(targetRow - preRow)) {
                //Acepta el destino si está vacío o tiene rival y no hay piezas bloqueando la diagonal.
                if(isValidSquare(targetCol, targetRow, pieces) && !pieceIsOnDiagonalLine(targetCol, targetRow, pieces)) {
                    return true;
                }
            }
        }
        //El destino no cumple las condiciones de movimiento del alfil.
        return false;
    }
}
