// Paquete que agrupa interfaces de acceso a la base de datos.
package com.transcendence.chess.repository;

// Colección usada para devolver varias partidas.
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.transcendence.chess.model.Game;

// Spring Data implementa las consultas declaradas por la interfaz.
public interface GameRepository extends JpaRepository<Game, String> {

	// Consulta partidas en curso donde el usuario participa, de la más reciente a la más antigua.
	@Query("select game from Game game where game.status = 'PLAYING' "
			// Limita las partidas a las que tienen al usuario con blancas o negras.
			+ "and (game.whitePlayer.id = :userId or game.blackPlayer.id = :userId) "
			// Ordena por fecha de creación descendente.
			+ "order by game.createdAt desc")
	// Proporciona el id que sustituye al parámetro :userId de JPQL.
	List<Game> findActiveGamesByUser(@Param("userId") Long userId);
}
