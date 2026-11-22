//Paquete que agrupa interfaces de acceso a la base de datos.
package com.transcendence.chess.repository;

//Tipos para resultados únicos opcionales y colecciones.
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transcendence.chess.model.User;

//Spring Data implementa automáticamente estas operaciones sobre User.
public interface UserRepository extends JpaRepository<User, Long> {
    //Busca una cuenta por correo y permite que no exista.
    Optional<User> findByEmail(String email);
    //Busca una coincidencia exacta del nombre de usuario.
    Optional<User> findByUsername(String username);
    //Busca ignorando mayúsculas y minúsculas.
    Optional<User> findByUsernameIgnoreCase(String username);
    //Devuelve como máximo diez coincidencias; excluye al usuario que busca y ordena alfabéticamente.
    List<User> findTop10ByUsernameContainingIgnoreCaseAndIdNotOrderByUsernameAsc(String username, Long id);
    //Comprueba si ya existe una cuenta con ese correo.
    boolean existsByEmail(String email);
    //Comprueba si ya existe una cuenta con ese nombre.
    boolean existsByUsername(String username);
}
