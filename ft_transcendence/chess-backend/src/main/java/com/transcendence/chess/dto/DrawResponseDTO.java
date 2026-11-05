// Paquete de objetos que reciben o devuelven datos de la API.
package com.transcendence.chess.dto;

// Respuesta inmutable a una propuesta de tablas: accepted indica aceptar o rechazar.
public record DrawResponseDTO(boolean accepted) {
}