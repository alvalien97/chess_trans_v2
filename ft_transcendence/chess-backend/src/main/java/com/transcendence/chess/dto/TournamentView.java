package com.transcendence.chess.dto;

import java.util.List;

public record TournamentView(
        String id,
        String name,
        String status,
        String owner,
        int maxPlayers,
        List<String> participants,
        List<MatchView> matches
) {
    public record MatchView(
            String id,
            int round,
            int number,
            Long whitePlayerId,
            String whitePlayer,
            Long blackPlayerId,
            String blackPlayer,
            String winner,
            String gameId,
            String status
    ) {
    }
}