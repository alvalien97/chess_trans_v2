package com.transcendence.chess.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transcendence.chess.model.Friendship;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    List<Friendship> findByUserIdOrderByIdAsc(Long userId);
    boolean existsByUserIdAndFriendId(Long userId, Long friendId);
    void deleteByUserId(Long userId);
    void deleteByFriendId(Long friendId);
    void deleteByUserIdAndFriendId(Long userId, Long friendId);
}
