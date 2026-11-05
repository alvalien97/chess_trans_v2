// Paquete que agrupa interfaces de acceso a la base de datos.
package com.transcendence.chess.repository;

// Colección usada para devolver inscripciones.
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transcendence.chess.model.TournamentParticipant;

// Spring Data genera las operaciones leyendo los nombres de método.
public interface TournamentParticipantRepository extends JpaRepository<TournamentParticipant, Long> {
    // Lista las inscripciones de un torneo ordenadas por posición inicial.
    List<TournamentParticipant> findByTournament_IdOrderBySeedAsc(String tournamentId);
    // Comprueba si el usuario ya se inscribió en el torneo.
    boolean existsByTournament_IdAndUser_Id(String tournamentId, Long userId);
    // Cuenta las inscripciones del torneo.
    long countByTournament_Id(String tournamentId);
    // Elimina todas las inscripciones asociadas al torneo.
    void deleteByTournament_Id(String tournamentId);
}