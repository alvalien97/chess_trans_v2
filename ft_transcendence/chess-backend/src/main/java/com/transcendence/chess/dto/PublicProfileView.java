// Paquete de objetos que reciben o devuelven datos de la API.
package com.transcendence.chess.dto;

// Tipo de fecha y hora absoluta usado para la fecha de registro.
import java.time.Instant;

// Vista inmutable con datos públicos, sin credenciales privadas de la cuenta.
public record PublicProfileView(Long id, String username, String avatarUrl, Instant joinedAt, boolean online) {
	// id identifica la cuenta; username y avatarUrl describen el perfil;
	// joinedAt indica el registro y online comunica el estado de presencia.
}