//Agrupa el modelo de torre con las demás piezas del juego.
package com.transcendence.chess.model;

//Proporciona la lista de piezas usada para comprobar ocupaciones.
import java.util.List;

//Pieza que se desplaza horizontal o verticalmente.
public class Rook extends Piece {
    //Crea una torre con su color y posición inicial.
    public Rook(int color, int col, int row) {
        //Inicializa el estado compartido de la pieza.
        super(color, col, row);
        //Registra el tipo concreto de esta pieza.
        type = Type.ROOK;
    }

    //Comprueba si la torre puede ir a las coordenadas indicadas.
    @Override
    public boolean canMove(int targetCol, int targetRow, List<Piece> pieces) {
        //El destino debe estar en el tablero y no ser la posición actual.
        if (isWithinBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
            //La torre mantiene fija la columna o la fila.
            if (targetCol == preCol || targetRow == preRow) {
                //Acepta casillas vacías o rivales y exige que el trayecto esté despejado.
                if (isValidSquare(targetCol, targetRow, pieces) && !pieceIsOnstraightLine(targetCol, targetRow, pieces)) {
                    return true;
                }
            }
        }
        //El destino no cumple las condiciones de movimiento de la torre.
        return false;
    }
}

