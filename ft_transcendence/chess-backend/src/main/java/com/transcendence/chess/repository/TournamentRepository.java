package com.transcendence.chess.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transcendence.chess.model.Tournament;

public interface TournamentRepository extends JpaRepository<Tournament, String> {
    List<Tournament> findAllByOrderByCreatedAtDesc();
}