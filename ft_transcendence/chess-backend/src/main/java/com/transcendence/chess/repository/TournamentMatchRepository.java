//Paquete que agrupa interfaces de acceso a la base de datos.
package com.transcendence.chess.repository;

//Tipos para devolver listas y resultados opcionales.
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transcendence.chess.model.TournamentMatch;

//Spring Data genera consultas a partir de los nombres declarados aquí.
public interface TournamentMatchRepository extends JpaRepository<TournamentMatch, String> {
    //Busca el encuentro asociado a una partida.
    Optional<TournamentMatch> findByGame_Id(String gameId);
    //Lista los encuentros del torneo ordenados por ronda y posición.
    List<TournamentMatch> findByTournament_IdOrderByRoundNumberAscMatchNumberAsc(String tournamentId);
    //Lista los encuentros de una ronda específica, ordenados por número.
    List<TournamentMatch> findByTournament_IdAndRoundNumberOrderByMatchNumberAsc(
            String tournamentId, int roundNumber);
}