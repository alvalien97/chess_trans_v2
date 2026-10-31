package com.transcendence.chess.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transcendence.chess.dto.MatchmakingStatus;
import com.transcendence.chess.model.Game;
import com.transcendence.chess.model.User;
import com.transcendence.chess.repository.GameRepository;
import com.transcendence.chess.repository.UserRepository;

@Service
public class MatchmakingService {
    private static final Duration QUEUE_TIMEOUT = Duration.ofSeconds(15);

    private final List<Long> waitingPlayers = new ArrayList<>();
    private final Set<Long> queuedPlayers = new HashSet<>();
    private final Map<Long, Instant> lastSeen = new HashMap<>();
    private final Map<Long, MatchmakingStatus> pendingMatches = new HashMap<>();
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Autowired
    public MatchmakingService(GameRepository gameRepository, UserRepository userRepository) {
        this(gameRepository, userRepository, Clock.systemUTC());
    }

    MatchmakingService(GameRepository gameRepository, UserRepository userRepository, Clock clock) {
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public synchronized MatchmakingStatus joinQueue(Long userId) {
        removeInactivePlayers();
        pendingMatches.remove(userId);
        if (queuedPlayers.contains(userId)) {
            lastSeen.put(userId, clock.instant());
            return waitingStatus();
        }

        if (waitingPlayers.isEmpty()) {
            waitingPlayers.add(userId);
            queuedPlayers.add(userId);
            lastSeen.put(userId, clock.instant());
            return waitingStatus();
        }

        int opponentIndex = ThreadLocalRandom.current().nextInt(waitingPlayers.size());
        Long opponentId = waitingPlayers.remove(opponentIndex);
        queuedPlayers.remove(opponentId);
        lastSeen.remove(opponentId);
        User whitePlayer = userRepository.findById(opponentId)
                .orElseThrow(() -> new IllegalStateException("El usuario en espera ya no existe."));
        User blackPlayer = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("El usuario autenticado ya no existe."));

        Game game = new Game();
        game.setId(UUID.randomUUID().toString());
        game.setWhitePlayer(whitePlayer);
        game.setBlackPlayer(blackPlayer);
        game.setStatus("PLAYING");
        gameRepository.save(game);

        pendingMatches.put(opponentId, statusFor(game, opponentId));
        return statusFor(game, userId);
    }

    @Transactional(readOnly = true)
    public synchronized MatchmakingStatus getStatus(Long userId) {
        removeInactivePlayers();
        MatchmakingStatus pendingMatch = pendingMatches.remove(userId);
        if (pendingMatch != null) {
            return pendingMatch;
        }
        if (!queuedPlayers.contains(userId)) {
            return new MatchmakingStatus("IDLE", null, null, null);
        }
        lastSeen.put(userId, clock.instant());
        return waitingStatus();
    }

    public synchronized void leaveQueue(Long userId) {
        queuedPlayers.remove(userId);
        waitingPlayers.remove(userId);
        lastSeen.remove(userId);
        pendingMatches.remove(userId);
    }

    private void removeInactivePlayers() {
        Instant cutoff = clock.instant().minus(QUEUE_TIMEOUT);
        List<Long> inactivePlayers = lastSeen.entrySet().stream()
                .filter(entry -> entry.getValue().isBefore(cutoff))
                .map(Map.Entry::getKey)
                .toList();
        for (Long inactivePlayer : inactivePlayers) {
            queuedPlayers.remove(inactivePlayer);
            waitingPlayers.remove(inactivePlayer);
            lastSeen.remove(inactivePlayer);
        }
    }

    private MatchmakingStatus statusFor(Game game, Long userId) {
        boolean isWhite = game.getWhitePlayer().getId().equals(userId);
        User opponent = isWhite ? game.getBlackPlayer() : game.getWhitePlayer();
        return new MatchmakingStatus("MATCHED", game.getId(), isWhite ? 0 : 1, opponent.getUsername());
    }

    private MatchmakingStatus waitingStatus() {
        return new MatchmakingStatus("WAITING", null, null, null);
    }
}