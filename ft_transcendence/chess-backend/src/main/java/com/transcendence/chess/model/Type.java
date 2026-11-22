//Agrupa este tipo dentro del paquete que contiene el modelo de datos del juego.
package com.transcendence.chess.model;

//Enumera los seis tipos de pieza posibles; usar un enum evita representar el tipo con texto arbitrario.
public enum Type {
    PAWN, // Peón.
    ROOK, // Torre.
    KNIGHT, // Caballo.
    BISHOP, // Alfil.
    QUEEN, // Dama o reina.
    KING // Rey.
}