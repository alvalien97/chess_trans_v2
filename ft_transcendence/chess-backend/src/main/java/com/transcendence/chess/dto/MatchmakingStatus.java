// Coloca este objeto de intercambio en el paquete de DTOs.
package com.transcendence.chess.dto;

// Registro inmutable que describe al cliente el resultado actual del emparejamiento.
public record MatchmakingStatus(String status, String gameId, Integer color, String opponent) {
	// status comunica la fase; gameId identifica la partida; color indica el bando asignado;
	// opponent contiene la información del rival cuando el emparejamiento ya se ha completado.
}