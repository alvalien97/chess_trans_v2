// Paquete de objetos que reciben o devuelven datos de la API.
package com.transcendence.chess.dto;

// Evento inmutable de presencia: identifica al usuario y comunica si está conectado.
public record PresenceUpdate(Long userId, boolean online) {
}