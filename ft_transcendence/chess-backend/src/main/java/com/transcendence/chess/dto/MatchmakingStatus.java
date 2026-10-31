package com.transcendence.chess.dto;

public record MatchmakingStatus(String status, String gameId, Integer color, String opponent) {
}