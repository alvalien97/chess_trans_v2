// Paquete de objetos que reciben o devuelven datos de la API.
package com.transcendence.chess.dto;

// Colecciones usadas para agrupar participantes y encuentros.
import java.util.List;

// Vista inmutable con la información de torneo que se entrega a los clientes.
public record TournamentView(
        // Identificador único del torneo.
        String id,
        // Nombre visible del torneo.
        String name,
        // Estado actual, por ejemplo abierto o iniciado.
        String status,
        // Nombre del usuario propietario u organizador.
        String owner,
        // Número máximo de participantes permitido.
        int maxPlayers,
        // Nombres de los usuarios inscritos.
        List<String> participants,
        // Encuentros que componen las rondas del torneo.
        List<MatchView> matches
) {
    // Vista inmutable de un encuentro individual del torneo.
    public record MatchView(
            // Identificador único del encuentro.
            String id,
            // Ronda en la que se juega.
            int round,
            // Número del encuentro dentro de la ronda.
            int number,
            // Identificador de la persona asignada al color blanco.
            Long whitePlayerId,
            // Nombre del jugador de blancas.
            String whitePlayer,
            // Identificador de la persona asignada al color negro.
            Long blackPlayerId,
            // Nombre del jugador de negras.
            String blackPlayer,
            // Ganador, si el encuentro ya fue resuelto.
            String winner,
            // Identificador de la partida asociada, si se creó.
            String gameId,
            // Estado actual del encuentro.
            String status
    ) {
    }
}