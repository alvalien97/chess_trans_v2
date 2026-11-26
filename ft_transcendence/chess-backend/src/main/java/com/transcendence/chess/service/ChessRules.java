//Agrupa las reglas de ajedrez dentro de la capa de servicios del backend.
package com.transcendence.chess.service;

//Tipo de pieza usado para reconocer peones y promociones.
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import com.transcendence.chess.model.Bishop;
import com.transcendence.chess.model.GameState;
import com.transcendence.chess.model.King;
import com.transcendence.chess.model.Knight;
import com.transcendence.chess.model.Pawn;
import com.transcendence.chess.model.Piece;
import com.transcendence.chess.model.Queen;
import com.transcendence.chess.model.Rook;

//Adapta el tablero de chesslib al estado y coordenadas usados por el resto del backend.
public class ChessRules {
    //Motor que mantiene la posición y calcula legalidad, jaque y tablas.
    private final com.github.bhlangonijr.chesslib.Board board;

    //Construye un tablero con la posición inicial estándar.
    public ChessRules() {
        //El constructor sin FEN inicializa la posición normal de ajedrez.
        board = new com.github.bhlangonijr.chesslib.Board();
    }

    //Construye una posición desde FEN; solo pueden usarlo clases del mismo paquete.
    ChessRules(String initialFen) {
        //Crea la estructura del tablero de chesslib.
        board = new com.github.bhlangonijr.chesslib.Board();
        //Carga las piezas, el turno y otros datos codificados en la cadena FEN.
        board.loadFromFen(initialFen);
    }

    //Sincroniza las lecturas con las modificaciones concurrentes del tablero.
    public synchronized GameState getState() {
        //Prepara el estado de dominio que se devolverá al cliente.
        GameState state = new GameState();
        //Traduce el lado activo a la convención local: blancas=0 y negras=1.
        if (board.getSideToMove() == Side.WHITE) {
            state.setCurrentColor(0);
        } else {
            state.setCurrentColor(1);
        }
        //Copia a la respuesta la etiqueta calculada según el estado del motor.
        state.setStatus(getStatus());

        //Recorre las ocho filas de la matriz local.
        for (int row = 0; row < 8; row++) {
            //Recorre las ocho columnas de cada fila.
            for (int col = 0; col < 8; col++) {
                //Convierte las coordenadas locales a una casilla de chesslib.
                Square square = squareAt(col, row);
                //Consulta qué pieza ocupa esa casilla en el motor.
                com.github.bhlangonijr.chesslib.Piece boardPiece = board.getPiece(square);
                //NONE representa una casilla vacía.
                if (boardPiece != com.github.bhlangonijr.chesslib.Piece.NONE) {
                    //Convierte la pieza ocupante y la añade al estado de dominio.
                    state.getPieces().add(toModelPiece(boardPiece, col, row));
                }
            }
        }

        //Devuelve una instantánea del tablero y el turno actual.
        return state;
    }

    //Valida y aplica un movimiento, y devuelve la posición resultante.
    public synchronized GameState move(int fromCol, int fromRow, int toCol, int toRow) {
        //Convierte la coordenada inicial a una casilla del motor.
        Square from = squareAt(fromCol, fromRow);
        //Convierte la coordenada final a una casilla del motor.
        Square to = squareAt(toCol, toRow);
        //Consulta la pieza que se intenta mover.
        com.github.bhlangonijr.chesslib.Piece movingPiece = board.getPiece(from);

        //No se puede mover desde una casilla vacía.
        if (movingPiece == com.github.bhlangonijr.chesslib.Piece.NONE) {
            //Comunica el motivo del rechazo a quien llamó al servicio.
            throw new IllegalArgumentException("No hay ninguna pieza en la casilla de origen.");
        }
        //La pieza debe pertenecer al lado que tiene el turno.
        if (movingPiece.getPieceSide() != board.getSideToMove()) {
            //Rechaza una jugada realizada por el color incorrecto.
            throw new IllegalArgumentException("No es el turno de este jugador.");
        }
        //Un mate o unas tablas indican que la partida ya terminó.
        if (board.isMated() || board.isDraw()) {
            //Impide que se continúe jugando después del final.
            throw new IllegalArgumentException("La partida ya ha terminado.");
        }

        //NONE significa que el movimiento no promociona ninguna pieza.
        com.github.bhlangonijr.chesslib.Piece promotion = com.github.bhlangonijr.chesslib.Piece.NONE;
        //Comprueba si el peón llega a la fila final correspondiente a su color.
        if (movingPiece.getPieceType() == PieceType.PAWN
                && ((board.getSideToMove() == Side.WHITE && toRow == 0)
                || (board.getSideToMove() == Side.BLACK && toRow == 7))) {
            //La promoción se realiza automáticamente a dama.
            promotion = com.github.bhlangonijr.chesslib.Piece.make(board.getSideToMove(), PieceType.QUEEN);
        }

        //Crea el movimiento con origen, destino y posible pieza de promoción.
        Move move = new Move(from, to, promotion);
        //Comprueba que el movimiento sea legal y que el motor pueda aplicarlo.
        if (!board.legalMoves().contains(move) || !board.doMove(move, true)) {
            //Rechaza movimientos que no respetan las reglas de la posición actual.
            throw new IllegalArgumentException("Movimiento no válido para esta posición.");
        }

        //Devuelve la nueva posición después de ejecutar la jugada.
        return getState();
    }

    //Traduce los estados internos del motor a etiquetas que usa la aplicación.
    private String getStatus() {
        //El jaque mate termina la partida.
        if (board.isMated()) {
            return "CHECKMATE";
        }
        //El ahogado ocurre cuando no hay movimientos legales y el rey no está en jaque.
        if (board.isStaleMate()) {
            return "STALEMATE";
        }
        //Comprueba otras condiciones de tablas reconocidas por chesslib.
        if (board.isDraw()) {
            return "DRAW";
        }
        //Indica que el rey del jugador activo está amenazado.
        if (board.isKingAttacked()) {
            return "CHECK";
        }
        //Si no se cumple ningún estado especial, el juego sigue en curso.
        return "PLAYING";
    }

    //Traduce coordenadas locales de 0 a 7 a una casilla como A8 para chesslib.
    private Square squareAt(int col, int row) {
        //Comprueba que la columna y la fila pertenezcan al tablero.
        if (col < 0 || col > 7 || row < 0 || row > 7) {
            //Rechaza coordenadas que no representan una casilla real.
            throw new IllegalArgumentException("La casilla está fuera del tablero.");
        }
        //Convierte la columna a letra y revierte la fila para ajustarse a la notación algebraica.
        return Square.valueOf(String.valueOf((char) ('A' + col)) + (8 - row));
    }

    //Convierte una pieza de chesslib a la subclase del modelo que usa la aplicación.
    private Piece toModelPiece(com.github.bhlangonijr.chesslib.Piece piece, int col, int row) {
        //Traduce el lado a la convención local de colores.
        int color = 1;
        if (piece.getPieceSide() == Side.WHITE) {
            color = 0;
        }
        //Elige la clase concreta de pieza según el tipo recibido.
        return switch (piece.getPieceType()) {
            //Crea el modelo de rey.
            case KING -> new King(color, col, row);
            //Crea el modelo de dama.
            case QUEEN -> new Queen(color, col, row);
            //Crea el modelo de torre.
            case ROOK -> new Rook(color, col, row);
            //Crea el modelo de alfil.
            case BISHOP -> new Bishop(color, col, row);
            //Crea el modelo de caballo.
            case KNIGHT -> new Knight(color, col, row);
            //Crea el modelo de peón.
            case PAWN -> new Pawn(color, col, row);
            //Falla explícitamente si se encuentra un tipo no contemplado.
            default -> throw new IllegalStateException("Tipo de pieza no soportado: " + piece);
        };
    }
}