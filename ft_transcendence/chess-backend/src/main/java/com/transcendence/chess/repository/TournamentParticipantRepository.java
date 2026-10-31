package com.transcendence.chess.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transcendence.chess.model.TournamentParticipant;

public interface TournamentParticipantRepository extends JpaRepository<TournamentParticipant, Long> {
    List<TournamentParticipant> findByTournament_IdOrderBySeedAsc(String tournamentId);
    boolean existsByTournament_IdAndUser_Id(String tournamentId, Long userId);
    long countByTournament_Id(String tournamentId);
    void deleteByTournament_Id(String tournamentId);
}