package com.transcendence.chess.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transcendence.chess.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameIgnoreCase(String username);
    List<User> findTop10ByUsernameContainingIgnoreCaseAndIdNotOrderByUsernameAsc(String username, Long id);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
