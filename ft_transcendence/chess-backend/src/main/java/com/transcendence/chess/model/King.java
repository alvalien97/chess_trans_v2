//Agrupa el modelo de rey con las demás piezas del juego.
package com.transcendence.chess.model;

//Proporciona la lista de piezas que representa el tablero.
import java.util.List;

//Pieza que se mueve una casilla y puede realizar el movimiento especial de enroque.
public class King extends Piece {
    //Guarda la torre asociada cuando se considera un enroque.
    public Piece castlingP;

    //Crea un rey con su color y posición inicial.
    public King(int color, int col, int row) {
        //Inicializa el estado compartido de la pieza.
        super(color, col, row);
        //Registra el tipo concreto de esta pieza.
        type = Type.KING;
    }

    //Comprueba si la casilla está amenazada por alguna pieza rival según sus reglas locales.
    private boolean isSquareUnderAttack(int targetCol, int targetRow, List<Piece> pieces) {
        //Revisa todas las piezas presentes en el tablero.
        for (Piece piece : pieces) {
            //Las piezas del mismo color no atacan al rey en este cálculo.
            if (piece.color != this.color) {
                //Pregunta a la pieza rival si puede alcanzar la casilla comprobada.
                if (piece.canMove(targetCol, targetRow, pieces)) {
                    return true;
                }
            }
        }
        //Ninguna pieza rival encontró un movimiento que ataque esta casilla.
        return false;
    }

    //Comprueba un paso normal del rey o uno de los dos enroques.
    @Override
    public boolean canMove(int targetCol, int targetRow, List<Piece> pieces) {
        //El destino debe encontrarse dentro del tablero.
        if (isWithinBoard(targetCol, targetRow)) {
            //Acepta un paso ortogonal o diagonal de una casilla.
            if (Math.abs(targetCol - preCol) + Math.abs(targetRow - preRow) == 1 || Math.abs(targetCol - preCol) * Math.abs(targetRow - preRow) == 1) {
                //El rey no puede terminar en una pieza propia.
                if (isValidSquare(targetCol, targetRow, pieces)) {
                    return true;
                }
            }
            //Lógica de enroque
            //Solo se puede enrocar si el rey todavía no se ha movido.
            if (!moved) {
                //Comprueba el enroque hacia la derecha y que no haya piezas entre rey y destino.
                if (targetCol == preCol + 2 && targetRow == preRow && !pieceIsOnstraightLine(targetCol, targetRow, pieces)) {
                    //El rey no puede estar en jaque ni atravesar una casilla atacada.
                    if (!isSquareUnderAttack(preCol, preRow, pieces) && !isSquareUnderAttack(preCol + 1, preRow, pieces)) {
                        //Busca una torre sin mover en la casilla esperada para el enroque corto.
                        for (Piece piece : pieces) {
                            //Si la encuentra, la registra para que el movimiento pueda moverla también.
                            if (piece.col == preCol + 3 && piece.row == preRow && !piece.moved) {
                                this.castlingP = piece;
                                return true;
                            }
                        }
                    }
                }
                //Comprueba el enroque hacia la izquierda y que no haya piezas entre rey y destino.
                if (targetCol == preCol - 2 && targetRow == preRow && !pieceIsOnstraightLine(targetCol, targetRow, pieces)) {
                    //El rey no puede estar en jaque ni atravesar una casilla atacada.
                    if (!isSquareUnderAttack(preCol, preRow, pieces) && !isSquareUnderAttack(preCol - 1, preRow, pieces)) {
                        //Busca una torre sin mover en la casilla esperada para el enroque largo.
                        for (Piece piece : pieces) {
                            //Confirma la posición y que la torre no se haya movido.
                            if (piece.col == preCol - 4 && piece.row == targetRow && !piece.moved) {
                                //Comprueba que las tres casillas intermedias estén vacías.
                                if (getHittingP(preCol - 1, targetRow, pieces) == null &&
                                        getHittingP(preCol - 2, targetRow, pieces) == null &&
                                        getHittingP(preCol - 3, targetRow, pieces) == null) {
                                        //Registra la torre que debe desplazarse como parte del enroque.
                                    this.castlingP = piece;
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        //No se encontró un movimiento normal ni un enroque válido.
        return false;
    }
}
