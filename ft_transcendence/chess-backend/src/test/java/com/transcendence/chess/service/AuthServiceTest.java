package com.transcendence.chess.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.transcendence.chess.dto.AccountUpdateRequest;
import com.transcendence.chess.dto.AuthRequest;
import com.transcendence.chess.dto.AuthResponse;
import com.transcendence.chess.model.User;
import com.transcendence.chess.repository.FriendshipRepository;
import com.transcendence.chess.repository.GameRepository;
import com.transcendence.chess.repository.UserRepository;

class AuthServiceTest {

    @Test
    void updatesUsernameAndPasswordOnlyAfterCheckingCurrentPassword() {
        UserRepository userRepository = mock(UserRepository.class);
        GameRepository gameRepository = mock(GameRepository.class);
        FriendshipRepository friendshipRepository = mock(FriendshipRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        MatchmakingService matchmakingService = mock(MatchmakingService.class);
        User user = user(7L, "oldname", "old@example.com", "current-hash");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("newname")).thenReturn(false);
        when(passwordEncoder.matches("current-pass", "current-hash")).thenReturn(true);
        when(passwordEncoder.encode("replacement-pass")).thenReturn("replacement-hash");
        when(userRepository.save(user)).thenReturn(user);

        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setCurrentPassword("current-pass");
        request.setUsername("newname");
        request.setNewPassword("replacement-pass");

        User updated = new AuthService(userRepository, gameRepository, friendshipRepository, passwordEncoder, matchmakingService,
            mock(PresenceService.class))
                .updateAccount(7L, request);

        assertEquals(user, updated);
        verify(user).setUsername("newname");
        verify(user).setPasswordHash("replacement-hash");
        verify(userRepository).save(user);
    }

    @Test
    void refusesAccountChangesWhenCurrentPasswordIsWrong() {
        UserRepository userRepository = mock(UserRepository.class);
        GameRepository gameRepository = mock(GameRepository.class);
        FriendshipRepository friendshipRepository = mock(FriendshipRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        User user = user(7L, "oldname", "old@example.com", "current-hash");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-pass", "current-hash")).thenReturn(false);
        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setCurrentPassword("wrong-pass");
        request.setUsername("newname");

        AuthService service = new AuthService(userRepository, gameRepository, friendshipRepository, passwordEncoder,
            mock(MatchmakingService.class), mock(PresenceService.class));
        assertThrows(IllegalArgumentException.class, () -> service.updateAccount(7L, request));

        verify(userRepository, never()).save(user);
    }

    @Test
    void anonymizesAccountRevokesSessionsAndLeavesTheMatchmakingQueue() {
        UserRepository userRepository = mock(UserRepository.class);
        GameRepository gameRepository = mock(GameRepository.class);
        FriendshipRepository friendshipRepository = mock(FriendshipRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        MatchmakingService matchmakingService = mock(MatchmakingService.class);
        PresenceService presenceService = mock(PresenceService.class);
        User user = user(7L, "player", "player@example.com", "current-hash");
        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current-pass", "current-hash")).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("anonymous-hash");
        when(userRepository.save(user)).thenReturn(user);
        AuthRequest login = new AuthRequest();
        login.setEmail("player@example.com");
        login.setPassword("current-pass");
        AuthService service = new AuthService(userRepository, gameRepository, friendshipRepository, passwordEncoder,
            matchmakingService, presenceService);
        AuthResponse session = service.login(login);

        service.deleteAccount(7L, "current-pass");

        assertThrows(ResponseStatusException.class,
                () -> service.requireUserId("Bearer " + session.getToken()));
        verify(user).setUsername(startsWith("deleted_"));
        verify(user).setEmail(anyString());
        verify(user).setAvatarUrl(null);
        verify(friendshipRepository).deleteByUserId(7L);
        verify(friendshipRepository).deleteByFriendId(7L);
        verify(matchmakingService).leaveQueue(7L);
        verify(presenceService).forceOffline(7L);
    }

    @Test
    void doesNotDeleteAccountWithActiveGames() {
        UserRepository userRepository = mock(UserRepository.class);
        GameRepository gameRepository = mock(GameRepository.class);
        FriendshipRepository friendshipRepository = mock(FriendshipRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        MatchmakingService matchmakingService = mock(MatchmakingService.class);
        User user = user(7L, "player", "player@example.com", "current-hash");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current-pass", "current-hash")).thenReturn(true);
        when(gameRepository.findActiveGamesByUser(7L)).thenReturn(java.util.List.of(new com.transcendence.chess.model.Game()));
        AuthService service = new AuthService(userRepository, gameRepository, friendshipRepository, passwordEncoder,
            matchmakingService, mock(PresenceService.class));

        assertThrows(IllegalArgumentException.class, () -> service.deleteAccount(7L, "current-pass"));

        verify(userRepository, never()).save(user);
        verify(matchmakingService, never()).leaveQueue(7L);
    }

    @Test
    void acceptsLargeDataUrlAvatar() {
        UserRepository userRepository = mock(UserRepository.class);
        GameRepository gameRepository = mock(GameRepository.class);
        FriendshipRepository friendshipRepository = mock(FriendshipRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        MatchmakingService matchmakingService = mock(MatchmakingService.class);
        User user = user(7L, "player", "player@example.com", "current-hash");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(user.getAvatarUrl()).thenReturn("data:image/png;base64," + "A".repeat(150000));

        String avatar = "data:image/png;base64," + "A".repeat(150000);
        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setAvatarUrl(avatar);

        User updated = new AuthService(userRepository, gameRepository, friendshipRepository, passwordEncoder, matchmakingService,
            mock(PresenceService.class))
                .updateAccount(7L, request);

        assertEquals(avatar, updated.getAvatarUrl());
        verify(user).setAvatarUrl(avatar);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void searchesUsersByPartialUsernameAndExcludesExistingFriends() {
        UserRepository userRepository = mock(UserRepository.class);
        GameRepository gameRepository = mock(GameRepository.class);
        FriendshipRepository friendshipRepository = mock(FriendshipRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        User existingFriend = user(2L, "Alice", "alice@example.com", "hash2");
        User matchingUser = user(3L, "Alina", "alina@example.com", "hash3");
        User currentUser = user(1L, "me", "me@example.com", "hash");
        com.transcendence.chess.model.Friendship relation = mock(com.transcendence.chess.model.Friendship.class);
        when(relation.getFriend()).thenReturn(existingFriend);
        when(friendshipRepository.findByUserIdOrderByIdAsc(1L)).thenReturn(List.of(relation));
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
        when(userRepository.findTop10ByUsernameContainingIgnoreCaseAndIdNotOrderByUsernameAsc("ali", 1L))
                .thenReturn(List.of(existingFriend, matchingUser));

        AuthService service = new AuthService(userRepository, gameRepository, friendshipRepository,
            passwordEncoder, mock(MatchmakingService.class), mock(PresenceService.class));
        var results = service.searchUsers(1L, " ali ");

        assertEquals(List.of("Alina"), results.stream().map(result -> result.username()).toList());
    }

    @Test
    void findsFriendByUsernameIgnoringCase() {
        UserRepository userRepository = mock(UserRepository.class);
        GameRepository gameRepository = mock(GameRepository.class);
        FriendshipRepository friendshipRepository = mock(FriendshipRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        MatchmakingService matchmakingService = mock(MatchmakingService.class);
        User currentUser = user(1L, "me", "me@example.com", "hash");
        User friend = user(2L, "Alice", "alice@example.com", "hash2");
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
        when(userRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(friend));
        when(friendshipRepository.existsByUserIdAndFriendId(1L, 2L)).thenReturn(false);
        when(friendshipRepository.existsByUserIdAndFriendId(2L, 1L)).thenReturn(false);

        AuthService service = new AuthService(userRepository, gameRepository, friendshipRepository, passwordEncoder,
            matchmakingService, mock(PresenceService.class));
        var result = service.addFriend(1L, "alice");

        assertEquals("Alice", result.username());
        verify(friendshipRepository, atLeastOnce()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void removesFriendshipFromBothUsers() {
        UserRepository userRepository = mock(UserRepository.class);
        FriendshipRepository friendshipRepository = mock(FriendshipRepository.class);
        User currentUser = user(1L, "me", "me@example.com", "hash");
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));

        AuthService service = new AuthService(userRepository, mock(GameRepository.class), friendshipRepository,
            mock(PasswordEncoder.class), mock(MatchmakingService.class), mock(PresenceService.class));
        service.removeFriend(1L, 2L);

        verify(friendshipRepository).deleteByUserIdAndFriendId(1L, 2L);
        verify(friendshipRepository).deleteByUserIdAndFriendId(2L, 1L);
    }

    private static User user(Long id, String username, String email, String passwordHash) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getUsername()).thenReturn(username);
        when(user.getEmail()).thenReturn(email);
        when(user.getPasswordHash()).thenReturn(passwordHash);
        return user;
    }
}