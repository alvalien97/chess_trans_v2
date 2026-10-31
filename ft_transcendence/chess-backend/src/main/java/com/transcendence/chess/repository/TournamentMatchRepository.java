package com.transcendence.chess.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transcendence.chess.model.TournamentMatch;

public interface TournamentMatchRepository extends JpaRepository<TournamentMatch, String> {
    Optional<TournamentMatch> findByGame_Id(String gameId);
    List<TournamentMatch> findByTournament_IdOrderByRoundNumberAscMatchNumberAsc(String tournamentId);
    List<TournamentMatch> findByTournament_IdAndRoundNumberOrderByMatchNumberAsc(
            String tournamentId, int roundNumber);
}