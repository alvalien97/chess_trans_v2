package com.transcendence.chess.dto;

import java.time.Instant;

public record PublicProfileView(Long id, String username, String avatarUrl, Instant joinedAt, boolean online) {
}