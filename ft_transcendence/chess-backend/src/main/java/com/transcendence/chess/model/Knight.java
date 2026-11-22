//Agrupa el modelo de caballo con las demás piezas del juego.
package com.transcendence.chess.model;

//Proporciona la lista de piezas usada para comprobar ocupaciones.
import java.util.List;

//Pieza que se desplaza en forma de L y puede saltar sobre otras piezas.
public class Knight extends Piece {
    //Crea un caballo con su color y posición inicial.
    public Knight(int color, int col, int row) {
        //Inicializa el estado compartido de la pieza.
        super(color, col, row);
        //Registra el tipo concreto de esta pieza.
        type = Type.KNIGHT;
    }

    //Comprueba si el caballo puede ir a las coordenadas indicadas.
    @Override
    public boolean canMove(int targetCol, int targetRow, List<Piece> pieces) {
        //El destino debe pertenecer al tablero.
        if (isWithinBoard(targetCol, targetRow)) {
            //El producto de los desplazamientos absolutos es 2 para movimientos de 1x2 o 2x1.
            if (Math.abs(targetCol - preCol) * Math.abs(targetRow - preRow) == 2) {
                //El destino debe estar vacío o contener una pieza del color contrario.
                if (isValidSquare(targetCol, targetRow, pieces)) {
                    return true;
                }
            }
        }
        //El movimiento no tiene forma de L válida o el destino contiene una pieza propia.
        return false;
    }
}
