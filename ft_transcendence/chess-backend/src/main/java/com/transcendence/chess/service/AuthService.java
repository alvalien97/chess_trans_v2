package com.transcendence.chess.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.transcendence.chess.dto.AccountUpdateRequest;
import com.transcendence.chess.dto.AuthRequest;
import com.transcendence.chess.dto.AuthResponse;
import com.transcendence.chess.dto.FriendView;
import com.transcendence.chess.dto.PublicProfileView;
import com.transcendence.chess.model.Friendship;
import com.transcendence.chess.model.User;
import com.transcendence.chess.repository.FriendshipRepository;
import com.transcendence.chess.repository.GameRepository;
import com.transcendence.chess.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final FriendshipRepository friendshipRepository;
    private final PasswordEncoder passwordEncoder;
    private final MatchmakingService matchmakingService;
    private final PresenceService presenceService;
    private final Map<String, Long> sessions = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository, GameRepository gameRepository,
                       FriendshipRepository friendshipRepository,
                       PasswordEncoder passwordEncoder,
                       MatchmakingService matchmakingService,
                       PresenceService presenceService) {
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.friendshipRepository = friendshipRepository;
        this.passwordEncoder = passwordEncoder;
        this.matchmakingService = matchmakingService;
        this.presenceService = presenceService;
    }

    public AuthResponse register(AuthRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Ese email ya está registrado.");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Ese nombre de usuario ya existe.");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        return issueToken(user);
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Email o contraseña incorrectos."));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Email o contraseña incorrectos.");
        }
        return issueToken(user);
    }

    @Transactional
    public User updateAccount(Long userId, AccountUpdateRequest request) {
        User user = requireUser(userId);
        String newPassword = request.getNewPassword();
        if (request.getUsername() != null || (newPassword != null && !newPassword.isBlank())) {
            requireCurrentPassword(user, request.getCurrentPassword());
        }

        if (request.getUsername() != null) {
            String username = request.getUsername().trim();
            if (username.length() < 3 || username.length() > 32) {
                throw new IllegalArgumentException("El nombre debe tener entre 3 y 32 caracteres.");
            }
            if (!username.equals(user.getUsername()) && userRepository.existsByUsername(username)) {
                throw new IllegalArgumentException("Ese nombre de usuario ya existe.");
            }
            user.setUsername(username);
        }

        if (newPassword != null && !newPassword.isBlank()) {
            if (newPassword.length() < 8 || newPassword.length() > 72) {
                throw new IllegalArgumentException("La nueva contraseña debe tener entre 8 y 72 caracteres.");
            }
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }

        if (request.getAvatarUrl() != null) {
            String avatarUrl = request.getAvatarUrl().trim();
            if (avatarUrl.isBlank()) {
                user.setAvatarUrl(null);
            } else if (avatarUrl.length() > 5_000_000 || (!avatarUrl.startsWith("data:image/")
                    && !avatarUrl.startsWith("http://") && !avatarUrl.startsWith("https://"))) {
                throw new IllegalArgumentException("La imagen de perfil no es válida.");
            } else {
                user.setAvatarUrl(avatarUrl);
            }
        }
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<FriendView> getFriends(Long userId) {
        requireUser(userId);
        return friendshipRepository.findByUserIdOrderByIdAsc(userId).stream()
                .map(friendship -> toFriendView(friendship.getFriend()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendView> searchUsers(Long userId, String query) {
        requireUser(userId);
        String cleanQuery = query == null ? "" : query.trim();
        if (cleanQuery.length() < 2) {
            return List.of();
        }

        Set<Long> existingFriendIds = friendshipRepository.findByUserIdOrderByIdAsc(userId).stream()
                .map(friendship -> friendship.getFriend().getId())
                .collect(Collectors.toSet());
        return userRepository.findTop10ByUsernameContainingIgnoreCaseAndIdNotOrderByUsernameAsc(cleanQuery, userId).stream()
                .filter(candidate -> !existingFriendIds.contains(candidate.getId()))
                .map(this::toFriendView)
                .toList();
    }

    @Transactional(readOnly = true)
    public PublicProfileView getPublicProfile(Long userId) {
        User user = requireUser(userId);
        return new PublicProfileView(user.getId(), user.getUsername(), user.getAvatarUrl(),
                user.getCreatedAt(), presenceService.isOnline(userId));
    }

    @Transactional
    public FriendView addFriend(Long userId, String username) {
        User currentUser = requireUser(userId);
        String cleanUsername = username == null ? "" : username.trim();
        if (cleanUsername.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }

        User friend = userRepository.findByUsernameIgnoreCase(cleanUsername)
                .or(() -> userRepository.findByUsername(cleanUsername))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        if (friend.getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("No puedes agregarte a ti mismo.");
        }

        if (!friendshipRepository.existsByUserIdAndFriendId(currentUser.getId(), friend.getId())) {
            Friendship currentRelation = new Friendship();
            currentRelation.setUser(currentUser);
            currentRelation.setFriend(friend);
            friendshipRepository.save(currentRelation);

            Friendship reverseRelation = new Friendship();
            reverseRelation.setUser(friend);
            reverseRelation.setFriend(currentUser);
            if (!friendshipRepository.existsByUserIdAndFriendId(friend.getId(), currentUser.getId())) {
                friendshipRepository.save(reverseRelation);
            }
        }

        return toFriendView(friend);
    }

    @Transactional
    public void removeFriend(Long userId, Long friendId) {
        requireUser(userId);
        friendshipRepository.deleteByUserIdAndFriendId(userId, friendId);
        friendshipRepository.deleteByUserIdAndFriendId(friendId, userId);
    }

    public void logout(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            if (sessions.remove(token) != null) {
                presenceService.revokeToken(token);
            }
        }
    }

    @Transactional
    public void deleteAccount(Long userId, String currentPassword) {
        User user = requireUser(userId);
        requireCurrentPassword(user, currentPassword);
        if (!gameRepository.findActiveGamesByUser(userId).isEmpty()) {
            throw new IllegalArgumentException("Termina tus partidas activas antes de eliminar la cuenta.");
        }
        matchmakingService.leaveQueue(userId);
        friendshipRepository.deleteByUserId(userId);
        friendshipRepository.deleteByFriendId(userId);

        String suffix = userId + "-" + UUID.randomUUID();
        user.setUsername("deleted_" + suffix.substring(0, Math.min(suffix.length(), 24)));
        user.setEmail("deleted+" + suffix + "@deleted.invalid");
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setAvatarUrl(null);
        userRepository.save(user);
        sessions.entrySet().removeIf(session -> session.getValue().equals(userId));
        presenceService.forceOffline(userId);
    }

    public Long requireUserId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticación requerida.");
        }

        Long userId = sessions.get(authorizationHeader.substring(7));
        if (userId == null || userRepository.findById(userId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión no válida.");
        }
        return userId;
    }

    private AuthResponse issueToken(User user) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, user.getId());
        return new AuthResponse(token, user);
    }

    public boolean isUserOnline(Long userId) {
        return userId != null && presenceService.isOnline(userId);
    }

    public boolean hasActiveSession(Long userId) {
        return userId != null && sessions.containsValue(userId);
    }

    private FriendView toFriendView(User user) {
        return new FriendView(user.getId(), user.getUsername(), user.getAvatarUrl(), isUserOnline(user.getId()));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));
    }

    private void requireCurrentPassword(User user, String currentPassword) {
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }
    }
}
