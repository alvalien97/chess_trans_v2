package com.transcendence.chess.service;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.transcendence.chess.model.Game;
import com.transcendence.chess.model.GameState;
import com.transcendence.chess.model.User;
import com.transcendence.chess.repository.GameRepository;

@Service
public class GameService {

    private final ConcurrentHashMap<String, ChessRules> activeGames = new ConcurrentHashMap<>();
    private final GameRepository gameRepository;
    private final TournamentService tournamentService;

    public GameService(GameRepository gameRepository, TournamentService tournamentService) {
        this.gameRepository = gameRepository;
        this.tournamentService = tournamentService;
    }

    @Transactional
    public GameState getGame(String gameId, Long userId) {
        Game game = requireGame(gameId);
        assertPlayer(game, userId);
        return stateFor(game);
    }

    @Transactional
    public synchronized GameState processMove(String gameId, Long userId, int fromCol, int fromRow, int toCol, int toRow) {
        Game game = requireGame(gameId);
        assertPlayer(game, userId);
        requirePlaying(game);

        int playerColor = Objects.equals(game.getWhitePlayer().getId(), userId) ? 0 : 1;
        ChessRules rules = getRules(game);
        if (rules.getState().getCurrentColor() != playerColor) {
            throw new IllegalArgumentException("No es tu turno.");
        }

        User drawOfferer = game.getDrawOfferer();
        GameState state = rules.move(fromCol, fromRow, toCol, toRow);
        if (drawOfferer != null && !drawOfferer.getId().equals(userId)) {
            game.setDrawOfferer(null);
        }
        if (state.getStatus().equals("CHECKMATE") || state.getStatus().equals("DRAW")
                || state.getStatus().equals("STALEMATE")) {
            game.setStatus(state.getStatus());
            if (state.getStatus().equals("CHECKMATE")) {
                game.setWinner(state.getCurrentColor() == 1 ? game.getWhitePlayer() : game.getBlackPlayer());
            }
            gameRepository.save(game);
            game.setDrawOfferer(null);
            tournamentService.completeGame(gameId, game.getWinner() == null ? null : game.getWinner().getId());
        }
        return stateFor(game);
    }

    @Transactional
    public synchronized GameState resign(String gameId, Long userId) {
        Game game = requireGame(gameId);
        assertPlayer(game, userId);
        requirePlaying(game);
        User winner = game.getWhitePlayer().getId().equals(userId)
                ? game.getBlackPlayer()
                : game.getWhitePlayer();
        game.setWinner(winner);
        game.setStatus("RESIGNED");
        gameRepository.save(game);
        game.setDrawOfferer(null);
        tournamentService.completeGame(gameId, winner.getId());
        return stateFor(game);
    }

    @Transactional
    public synchronized GameState offerDraw(String gameId, Long userId) {
        Game game = requireGame(gameId);
        assertPlayer(game, userId);
        requirePlaying(game);
        User currentOffer = game.getDrawOfferer();
        if (currentOffer != null) {
            throw new IllegalArgumentException(currentOffer.getId().equals(userId)
                    ? "Ya ofreciste tablas; espera la respuesta del rival."
                    : "Responde primero a la oferta de tablas del rival.");
        }
        game.setDrawOfferer(game.getWhitePlayer().getId().equals(userId)
                ? game.getWhitePlayer()
                : game.getBlackPlayer());
        gameRepository.save(game);
        return stateFor(game);
    }

    @Transactional
    public synchronized GameState respondToDraw(String gameId, Long userId, boolean accept) {
        Game game = requireGame(gameId);
        assertPlayer(game, userId);
        requirePlaying(game);
        User offerer = game.getDrawOfferer();
        if (offerer == null && accept) {
            game.setDrawOfferer(game.getWhitePlayer().getId().equals(userId)
                    ? game.getWhitePlayer()
                    : game.getBlackPlayer());
            gameRepository.save(game);
            GameState state = stateFor(game);
            state.setResultMessage("La oferta anterior ya no estaba pendiente; se ha enviado una nueva oferta.");
            return state;
        }
        if (offerer == null || offerer.getId().equals(userId)) {
            throw new IllegalArgumentException("No tienes una oferta de tablas pendiente del rival.");
        }
        game.setDrawOfferer(null);
        if (accept) {
            game.setStatus("DRAW");
            game.setWinner(null);
            gameRepository.save(game);
            tournamentService.completeGame(gameId, null);
        }
        gameRepository.save(game);
        GameState state = stateFor(game);
        state.setResultMessage(accept ? "Ambos jugadores acordaron tablas." : "La oferta de tablas fue rechazada.");
        return state;
    }

    @Transactional(readOnly = true)
    public void assertPlayer(String gameId, Long userId) {
        assertPlayer(requireGame(gameId), userId);
    }

    private Game requireGame(String gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partida no encontrada."));
    }

    private void assertPlayer(Game game, Long userId) {
        boolean isWhite = game.getWhitePlayer() != null && Objects.equals(game.getWhitePlayer().getId(), userId);
        boolean isBlack = game.getBlackPlayer() != null && Objects.equals(game.getBlackPlayer().getId(), userId);
        if (!isWhite && !isBlack) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No participas en esta partida.");
        }
    }

    private void requirePlaying(Game game) {
        if (!"PLAYING".equals(game.getStatus())) {
            throw new IllegalArgumentException("La partida ya ha terminado.");
        }
    }

    private GameState stateFor(Game game) {
        GameState state = getRules(game).getState();
        if (!"PLAYING".equals(game.getStatus())) {
            state.setStatus(game.getStatus());
        }
        state.setDrawOfferByUserId(game.getDrawOfferer() == null ? null : game.getDrawOfferer().getId());
        state.setWinnerId(game.getWinner() == null ? null : game.getWinner().getId());
        if ("RESIGNED".equals(game.getStatus())) {
            state.setResultMessage("La partida terminó por rendición.");
        } else if ("DRAW".equals(game.getStatus()) || "STALEMATE".equals(game.getStatus())) {
            state.setResultMessage("La partida terminó en tablas.");
        }
        return state;
    }

    private ChessRules getRules(Game game) {
        return activeGames.computeIfAbsent(game.getId(), id -> new ChessRules());
    }
}
