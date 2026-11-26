//Los tests comparten paquete con MatchmakingService para acceder a su constructor de prueba.
package com.transcendence.chess.service;

//Tipos de tiempo y colecciones usadas por los escenarios.
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

//Comprueba que la cola empareja usuarios y expira esperas inactivas.
class MatchmakingServiceTest {

    //Un usuario no se empareja consigo mismo y cada jugador recibe un color.
    @Test
    void doesNotPairAUserWithThemselvesAndAssignsTwoColors() {
        //Simula los repositorios usados para crear la partida.
        GameRepository gameRepository = mock(GameRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        //Prepara dos usuarios que representarán colores opuestos.
        User whitePlayer = player(11L, "alvalien");
            //Configura las búsquedas de usuario y el guardado de partida.
        User blackPlayer = player(22L, "mmembril");
        when(gameRepository.findActiveGamesByUser(11L)).thenReturn(List.of());
        when(gameRepository.findActiveGamesByUser(22L)).thenReturn(List.of());
        when(userRepository.findById(11L)).thenReturn(Optional.of(whitePlayer));
        when(userRepository.findById(22L)).thenReturn(Optional.of(blackPlayer));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchmakingService matchmaking = new MatchmakingService(
            gameRepository, userRepository, Clock.systemUTC(), () -> true);

        //La primera entrada espera; repetirla no añade una segunda plaza.
        assertEquals("WAITING", matchmaking.joinQueue(11L).status());
        assertEquals("WAITING", matchmaking.joinQueue(11L).status());

        //El segundo usuario forma pareja con el primero.
        MatchmakingStatus match = matchmaking.joinQueue(22L);

        //El segundo usuario recibe negras y el nombre del rival.
        assertEquals("MATCHED", match.status());
        assertEquals(1, match.color());
        assertEquals("alvalien", match.opponent());

        //El usuario que estaba esperando recoge su resultado pendiente.
        MatchmakingStatus waitingPlayerStatus = matchmaking.getStatus(11L);
        //Desde su perspectiva, tiene blancas y el rival es el segundo usuario.
        assertEquals("MATCHED", waitingPlayerStatus.status());
        assertEquals(0, waitingPlayerStatus.color());
        assertEquals("mmembril", waitingPlayerStatus.opponent());
    }

    @Test
    void canAssignWhiteToThePlayerWhoJoinsTheQueueSecond() {
        GameRepository gameRepository = mock(GameRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        User waitingPlayer = player(11L, "alvalien");
        User joiningPlayer = player(22L, "mmembril");
        when(userRepository.findById(11L)).thenReturn(Optional.of(waitingPlayer));
        when(userRepository.findById(22L)).thenReturn(Optional.of(joiningPlayer));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchmakingService matchmaking = new MatchmakingService(
                gameRepository, userRepository, Clock.systemUTC(), () -> false);

        assertEquals("WAITING", matchmaking.joinQueue(11L).status());
        MatchmakingStatus joiningPlayerStatus = matchmaking.joinQueue(22L);
        MatchmakingStatus waitingPlayerStatus = matchmaking.getStatus(11L);

        assertEquals(0, joiningPlayerStatus.color());
        assertEquals(1, waitingPlayerStatus.color());
    }

    //Un usuario que deja de enviar señales de actividad expira de la cola.
    @Test
    void expiresPlayersThatStopSendingQueueHeartbeats() {
        //Prepara repositorios y un reloj controlable.
        GameRepository gameRepository = mock(GameRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        MatchmakingService matchmaking = new MatchmakingService(gameRepository, userRepository, clock);

        //Inicia la espera y avanza el reloj más allá del tiempo límite.
        assertEquals("WAITING", matchmaking.joinQueue(11L).status());
        clock.advance(Duration.ofSeconds(16));

        //El siguiente usuario permanece esperando sin emparejarse con el expirado.
        assertEquals("WAITING", matchmaking.joinQueue(22L).status());
        //El usuario expirado ya no figura en cola.
        assertEquals("IDLE", matchmaking.getStatus(11L).status());
        //No se creó partida alguna.
        verify(gameRepository, never()).save(any(Game.class));
    }

    //La cola admite a un usuario aunque ya tenga una partida activa.
    @Test
    void allowsAnotherQueueWhenThePlayerAlreadyHasAnActiveGame() {
        //Prepara una partida activa asociada al usuario.
        GameRepository gameRepository = mock(GameRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        Game activeGame = new Game();
        activeGame.setId("active-game");
        when(gameRepository.findActiveGamesByUser(11L)).thenReturn(List.of(activeGame));
        MatchmakingService matchmaking = new MatchmakingService(gameRepository, userRepository);

        //El usuario puede entrar en la cola.
        assertEquals("WAITING", matchmaking.joinQueue(11L).status());

        //El servicio no consulta las partidas activas en este flujo.
        verify(gameRepository, never()).findActiveGamesByUser(11L);
    }

    //Crea un usuario mockeado con id y nombre conocidos.
    private static User player(Long id, String username) {
        //Prepara el mock de usuario.
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getUsername()).thenReturn(username);
        return user;
    }

    //Reloj de prueba cuya hora puede adelantarse sin esperar tiempo real.
    private static final class MutableClock extends Clock {
        //Instante actual que devolverá este reloj.
        private Instant instant;

        //Inicializa el reloj con un instante controlado por el test.
        MutableClock(Instant instant) {
            this.instant = instant;
        }

        //Adelanta el tiempo simulado por la duración indicada.
        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        //Informa de la zona horaria del reloj de prueba.
        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        //Devuelve este mismo reloj en esta implementación de prueba.
        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        //Devuelve el instante simulado actual.
        @Override
        public Instant instant() {
            return instant;
        }
    }
}