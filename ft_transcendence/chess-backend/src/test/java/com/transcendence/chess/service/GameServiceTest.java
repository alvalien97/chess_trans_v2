// Los tests pertenecen al mismo paquete que GameService.
package com.transcendence.chess.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.transcendence.chess.model.Game;
import com.transcendence.chess.model.GameState;
import com.transcendence.chess.model.User;
import com.transcendence.chess.repository.GameRepository;

// Verifica turnos, permisos, rendiciones y ofertas de tablas.
class GameServiceTest {
    private GameService gameService;
    private GameRepository gameRepository;
    private TournamentService tournamentService;
    private Game game;

    // Prepara antes de cada escenario una partida de prueba con dos jugadores.
    @BeforeEach
    void setUp() {
        // Simula los repositorios y el servicio de torneos.
        gameRepository = mock(GameRepository.class);
        tournamentService = mock(TournamentService.class);
        // Asigna ids distintos a los jugadores de blancas y negras.
        User whitePlayer = player(11L);
        User blackPlayer = player(22L);
        // Crea una partida activa y configura que la búsqueda por id la devuelva.
        game = new Game();
        game.setId("game-1");
        game.setWhitePlayer(whitePlayer);
        game.setBlackPlayer(blackPlayer);
        game.setStatus("PLAYING");
        when(gameRepository.findById("game-1")).thenReturn(Optional.of(game));
        gameService = new GameService(gameRepository, tournamentService);
    }

    // Cada jugador puede mover solo sus piezas y únicamente en su turno.
    @Test
    void playerCanMoveOnlyTheirAssignedColor() {
        // Blancas mueve primero desde la posición inicial.
        GameState afterWhiteMove = gameService.processMove("game-1", 11L, 4, 6, 4, 4);

        // Comprueba que ahora juegan negras y que blancas no pueden mover otra vez.
        assertEquals(1, afterWhiteMove.getCurrentColor());
        assertEquals("No es tu turno.", assertThrows(IllegalArgumentException.class,
                () -> gameService.processMove("game-1", 11L, 4, 1, 4, 3)).getMessage());

        // Negras hace su movimiento legal.
        GameState afterBlackMove = gameService.processMove("game-1", 22L, 4, 1, 4, 3);
        // Tras la jugada negra, vuelve el turno blanco.
        assertEquals(0, afterBlackMove.getCurrentColor());
    }

    // Un usuario que no participa recibe HTTP 403 al consultar la partida.
    @Test
    void rejectsUserWhoIsNotAssignedToGame() {
        // Intenta acceder con un id distinto al de ambos jugadores.
        org.springframework.web.server.ResponseStatusException exception = assertThrows(
            org.springframework.web.server.ResponseStatusException.class,
            () -> gameService.getGame("game-1", 33L));
        // Comprueba que el rechazo corresponde a falta de permiso.
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    // Rendirse otorga la victoria al rival y actualiza la partida y el torneo.
    @Test
    void resignationAwardsTheGameToTheOpponent() {
        // El jugador blanco se rinde.
        GameState state = gameService.resign("game-1", 11L);

        // Verifica el resultado devuelto, guardado y comunicado al torneo.
        assertEquals("RESIGNED", state.getStatus());
        assertEquals(22L, state.getWinnerId());
        assertEquals("RESIGNED", game.getStatus());
        verify(gameRepository).save(game);
        verify(tournamentService).completeGame("game-1", 22L);
    }

    // La aceptación de tablas termina la partida sin declarar ganador.
    @Test
    void acceptedDrawEndsGameWithoutAWinner() {
        // Blancas ofrece tablas.
        GameState offer = gameService.offerDraw("game-1", 11L);
        assertEquals(11L, game.getDrawOfferer().getId());
        // Usa una instancia nueva para probar el comportamiento con reglas reconstruidas.
        GameService restartedService = new GameService(gameRepository, tournamentService);
        // Negras acepta la oferta.
        GameState result = restartedService.respondToDraw("game-1", 22L, true);

        // El resultado debe ser tablas y notificarse al servicio de torneos.
        assertEquals(11L, offer.getDrawOfferByUserId());
        assertEquals("DRAW", result.getStatus());
        assertEquals("Ambos jugadores acordaron tablas.", result.getResultMessage());
        verify(tournamentService).completeGame("game-1", null);
    }

    // Aceptar sin que haya oferta pendiente inicia una nueva oferta y no cierra el juego.
    @Test
    void acceptingAfterLostOfferCreatesANewOfferInsteadOfEndingTheGame() {
        // Negras intenta aceptar cuando no hay oferta almacenada.
        GameState state = gameService.respondToDraw("game-1", 22L, true);

        // La respuesta indica que la partida sigue y que se envió una oferta nueva.
        assertEquals("PLAYING", state.getStatus());
        assertEquals(22L, state.getDrawOfferByUserId());
        assertEquals("La oferta anterior ya no estaba pendiente; se ha enviado una nueva oferta.",
                state.getResultMessage());
        verify(gameRepository).save(game);
        verify(tournamentService, never()).completeGame("game-1", null);
    }

    // Rechazar una oferta mantiene el juego activo y permite volver a proponer tablas.
    @Test
    void rejectedDrawLeavesGameInProgressAndCanBeOfferedAgain() {
        // Blancas ofrece y negras rechaza.
        gameService.offerDraw("game-1", 11L);
        GameState result = gameService.respondToDraw("game-1", 22L, false);

        // Comprueba que no hay oferta pendiente y que la partida no terminó.
        assertEquals("PLAYING", result.getStatus());
        assertEquals(null, result.getDrawOfferByUserId());
        assertEquals("La oferta de tablas fue rechazada.", result.getResultMessage());
        assertEquals("PLAYING", gameService.offerDraw("game-1", 22L).getStatus());
        verify(tournamentService, never()).completeGame("game-1", null);
    }

    // Solo los jugadores de una partida pueden ofrecer tablas.
    @Test
    void onlyGameParticipantsCanOfferDraws() {
        // Un tercero debe recibir error de acceso prohibido.
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN,
                assertThrows(org.springframework.web.server.ResponseStatusException.class,
                        () -> gameService.offerDraw("game-1", 33L)).getStatusCode());
    }

    // La jugada del rival invalida una propuesta de tablas pendiente.
    @Test
    void opponentMoveRejectsPendingDrawOffer() {
        // Blancas crea la oferta y realiza una jugada.
        gameService.offerDraw("game-1", 11L);
        GameService separateSession = gameService;
        separateSession.processMove("game-1", 11L, 4, 6, 4, 4);
        // La respuesta del rival es una jugada, no una aceptación de tablas.
        GameState response = separateSession.processMove("game-1", 22L, 4, 1, 4, 3);

        // Comprueba que la partida continúa sin oferta pendiente.
        assertEquals(null, response.getDrawOfferByUserId());
        assertEquals("PLAYING", response.getStatus());
    }

    // Construye un usuario mockeado que devuelve el id indicado.
    private static User player(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }
}