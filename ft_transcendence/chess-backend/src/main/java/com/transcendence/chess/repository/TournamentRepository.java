//Paquete que agrupa interfaces de acceso a la base de datos.
package com.transcendence.chess.repository;

//Colección usada para devolver varios torneos.
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transcendence.chess.model.Tournament;

//Spring Data proporciona operaciones persistentes para torneos.
public interface TournamentRepository extends JpaRepository<Tournament, String> {
    //Devuelve los torneos ordenados del más nuevo al más antiguo.
    List<Tournament> findAllByOrderByCreatedAtDesc();
}