// Paquete de objetos que reciben o devuelven datos de la API.
package com.transcendence.chess.dto;

// Rechaza un nombre nulo, vacío o compuesto solo por espacios.
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Datos inmutables que el cliente envía al crear un torneo.
public record TournamentRequest(@NotBlank @Size(max = 80) String name, int maxPlayers) {
	// name es obligatorio y no puede superar 80 caracteres; maxPlayers fija el límite de participantes.
}