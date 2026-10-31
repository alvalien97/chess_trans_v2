package com.transcendence.chess.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TournamentRequest(@NotBlank @Size(max = 80) String name, int maxPlayers) {
}