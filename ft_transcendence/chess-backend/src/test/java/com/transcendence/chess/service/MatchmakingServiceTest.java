package com.transcendence.chess.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.transcendence.chess.dto.MatchmakingStatus;
import com.transcendence.chess.model.Game;
import com.transcendence.chess.model.User;
import com.transcendence.chess.repository.GameRepository;
import com.transcendence.chess.repository.UserRepository;

class MatchmakingServiceTest {

    @Test
    void doesNotPairAUserWithThemselvesAndAssignsTwoColors() {
        GameRepository gameRepository = mock(GameRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        User whitePlayer = player(11L, "alvalien");
        User blackPlayer = player(22L, "mmembril");
        when(gameRepository.findActiveGamesByUser(11L)).thenReturn(List.of());
        when(gameRepository.findActiveGamesByUser(22L)).thenReturn(List.of());
        when(userRepository.findById(11L)).thenReturn(Optional.of(whitePlayer));
        when(userRepository.findById(22L)).thenReturn(Optional.of(blackPlayer));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchmakingService matchmaking = new MatchmakingService(gameRepository, userRepository);

        assertEquals("WAITING", matchmaking.joinQueue(11L).status());
        assertEquals("WAITING", matchmaking.joinQueue(11L).status());

        MatchmakingStatus match = matchmaking.joinQueue(22L);

        assertEquals("MATCHED", match.status());
        assertEquals(1, match.color());
        assertEquals("alvalien", match.opponent());

        MatchmakingStatus waitingPlayerStatus = matchmaking.getStatus(11L);
        assertEquals("MATCHED", waitingPlayerStatus.status());
        assertEquals(0, waitingPlayerStatus.color());
        assertEquals("mmembril", waitingPlayerStatus.opponent());
    }

    @Test
    void expiresPlayersThatStopSendingQueueHeartbeats() {
        GameRepository gameRepository = mock(GameRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        MatchmakingService matchmaking = new MatchmakingService(gameRepository, userRepository, clock);

        assertEquals("WAITING", matchmaking.joinQueue(11L).status());
        clock.advance(Duration.ofSeconds(16));

        assertEquals("WAITING", matchmaking.joinQueue(22L).status());
        assertEquals("IDLE", matchmaking.getStatus(11L).status());
        verify(gameRepository, never()).save(any(Game.class));
    }

    @Test
    void allowsAnotherQueueWhenThePlayerAlreadyHasAnActiveGame() {
        GameRepository gameRepository = mock(GameRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        Game activeGame = new Game();
        activeGame.setId("active-game");
        when(gameRepository.findActiveGamesByUser(11L)).thenReturn(List.of(activeGame));
        MatchmakingService matchmaking = new MatchmakingService(gameRepository, userRepository);

        assertEquals("WAITING", matchmaking.joinQueue(11L).status());

        verify(gameRepository, never()).findActiveGamesByUser(11L);
    }

    private static User player(Long id, String username) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getUsername()).thenReturn(username);
        return user;
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}