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

class GameServiceTest {
    private GameService gameService;
    private GameRepository gameRepository;
    private TournamentService tournamentService;
    private Game game;

    @BeforeEach
    void setUp() {
        gameRepository = mock(GameRepository.class);
        tournamentService = mock(TournamentService.class);
        User whitePlayer = player(11L);
        User blackPlayer = player(22L);
        game = new Game();
        game.setId("game-1");
        game.setWhitePlayer(whitePlayer);
        game.setBlackPlayer(blackPlayer);
        game.setStatus("PLAYING");
        when(gameRepository.findById("game-1")).thenReturn(Optional.of(game));
        gameService = new GameService(gameRepository, tournamentService);
    }

    @Test
    void playerCanMoveOnlyTheirAssignedColor() {
        GameState afterWhiteMove = gameService.processMove("game-1", 11L, 4, 6, 4, 4);

        assertEquals(1, afterWhiteMove.getCurrentColor());
        assertEquals("No es tu turno.", assertThrows(IllegalArgumentException.class,
                () -> gameService.processMove("game-1", 11L, 4, 1, 4, 3)).getMessage());

        GameState afterBlackMove = gameService.processMove("game-1", 22L, 4, 1, 4, 3);
        assertEquals(0, afterBlackMove.getCurrentColor());
    }

    @Test
    void rejectsUserWhoIsNotAssignedToGame() {
        org.springframework.web.server.ResponseStatusException exception = assertThrows(
            org.springframework.web.server.ResponseStatusException.class,
            () -> gameService.getGame("game-1", 33L));
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void resignationAwardsTheGameToTheOpponent() {
        GameState state = gameService.resign("game-1", 11L);

        assertEquals("RESIGNED", state.getStatus());
        assertEquals(22L, state.getWinnerId());
        assertEquals("RESIGNED", game.getStatus());
        verify(gameRepository).save(game);
        verify(tournamentService).completeGame("game-1", 22L);
    }

    @Test
    void acceptedDrawEndsGameWithoutAWinner() {
        GameState offer = gameService.offerDraw("game-1", 11L);
        assertEquals(11L, game.getDrawOfferer().getId());
        GameService restartedService = new GameService(gameRepository, tournamentService);
        GameState result = restartedService.respondToDraw("game-1", 22L, true);

        assertEquals(11L, offer.getDrawOfferByUserId());
        assertEquals("DRAW", result.getStatus());
        assertEquals("Ambos jugadores acordaron tablas.", result.getResultMessage());
        verify(tournamentService).completeGame("game-1", null);
    }

    @Test
    void acceptingAfterLostOfferCreatesANewOfferInsteadOfEndingTheGame() {
        GameState state = gameService.respondToDraw("game-1", 22L, true);

        assertEquals("PLAYING", state.getStatus());
        assertEquals(22L, state.getDrawOfferByUserId());
        assertEquals("La oferta anterior ya no estaba pendiente; se ha enviado una nueva oferta.",
                state.getResultMessage());
        verify(gameRepository).save(game);
        verify(tournamentService, never()).completeGame("game-1", null);
    }

    @Test
    void rejectedDrawLeavesGameInProgressAndCanBeOfferedAgain() {
        gameService.offerDraw("game-1", 11L);
        GameState result = gameService.respondToDraw("game-1", 22L, false);

        assertEquals("PLAYING", result.getStatus());
        assertEquals(null, result.getDrawOfferByUserId());
        assertEquals("La oferta de tablas fue rechazada.", result.getResultMessage());
        assertEquals("PLAYING", gameService.offerDraw("game-1", 22L).getStatus());
        verify(tournamentService, never()).completeGame("game-1", null);
    }

    @Test
    void onlyGameParticipantsCanOfferDraws() {
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN,
                assertThrows(org.springframework.web.server.ResponseStatusException.class,
                        () -> gameService.offerDraw("game-1", 33L)).getStatusCode());
    }

    @Test
    void opponentMoveRejectsPendingDrawOffer() {
        gameService.offerDraw("game-1", 11L);
        GameService separateSession = gameService;
        separateSession.processMove("game-1", 11L, 4, 6, 4, 4);
        GameState response = separateSession.processMove("game-1", 22L, 4, 1, 4, 3);

        assertEquals(null, response.getDrawOfferByUserId());
        assertEquals("PLAYING", response.getStatus());
    }

    private static User player(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }
}