package com.transcendence.chess.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.transcendence.chess.model.Game;

public interface GameRepository extends JpaRepository<Game, String> {

	@Query("select game from Game game where game.status = 'PLAYING' "
			+ "and (game.whitePlayer.id = :userId or game.blackPlayer.id = :userId) "
			+ "order by game.createdAt desc")
	List<Game> findActiveGamesByUser(@Param("userId") Long userId);
}
