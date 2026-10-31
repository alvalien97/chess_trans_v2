package com.transcendence.chess.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.web.server.ResponseStatusException;

import com.transcendence.chess.dto.TournamentView;
import com.transcendence.chess.model.Game;
import com.transcendence.chess.model.Tournament;
import com.transcendence.chess.model.TournamentMatch;
import com.transcendence.chess.model.TournamentParticipant;
import com.transcendence.chess.model.User;
import com.transcendence.chess.repository.GameRepository;
import com.transcendence.chess.repository.TournamentMatchRepository;
import com.transcendence.chess.repository.TournamentParticipantRepository;
import com.transcendence.chess.repository.TournamentRepository;
import com.transcendence.chess.repository.UserRepository;

class TournamentServiceTest {

    @Test
    void deletesWaitingTournamentAndItsParticipantsForOwner() {
        TournamentRepository tournamentRepository = mock(TournamentRepository.class);
        TournamentParticipantRepository participantRepository = mock(TournamentParticipantRepository.class);
        User owner = player(1L, "owner");
        Tournament tournament = tournament(owner, 2);
        when(tournamentRepository.findById("tournament-1")).thenReturn(Optional.of(tournament));

        TournamentService service = new TournamentService(tournamentRepository, participantRepository,
                mock(TournamentMatchRepository.class), mock(GameRepository.class), mock(UserRepository.class));
        service.delete("tournament-1", 1L);

        verify(participantRepository).deleteByTournament_Id("tournament-1");
        verify(tournamentRepository).delete(tournament);
    }

    @Test
    void refusesToDeleteTournamentForAnotherUser() {
        TournamentRepository tournamentRepository = mock(TournamentRepository.class);
        TournamentParticipantRepository participantRepository = mock(TournamentParticipantRepository.class);
        Tournament tournament = tournament(player(1L, "owner"), 2);
        when(tournamentRepository.findById("tournament-1")).thenReturn(Optional.of(tournament));

        TournamentService service = new TournamentService(tournamentRepository, participantRepository,
                mock(TournamentMatchRepository.class), mock(GameRepository.class), mock(UserRepository.class));
        assertThrows(ResponseStatusException.class, () -> service.delete("tournament-1", 2L));

        verify(participantRepository, org.mockito.Mockito.never()).deleteByTournament_Id("tournament-1");
        verify(tournamentRepository, org.mockito.Mockito.never()).delete(tournament);
    }

    @Test
    void refusesToDeleteTournamentAfterItHasStarted() {
        TournamentRepository tournamentRepository = mock(TournamentRepository.class);
        TournamentParticipantRepository participantRepository = mock(TournamentParticipantRepository.class);
        Tournament tournament = tournament(player(1L, "owner"), 2);
        tournament.setStatus("RUNNING");
        when(tournamentRepository.findById("tournament-1")).thenReturn(Optional.of(tournament));

        TournamentService service = new TournamentService(tournamentRepository, participantRepository,
                mock(TournamentMatchRepository.class), mock(GameRepository.class), mock(UserRepository.class));
        assertThrows(IllegalArgumentException.class, () -> service.delete("tournament-1", 1L));

        verify(participantRepository, org.mockito.Mockito.never()).deleteByTournament_Id("tournament-1");
        verify(tournamentRepository, org.mockito.Mockito.never()).delete(tournament);
    }

    @Test
    void randomlyAdvancesOnePlayerWhenTournamentGameIsDrawn() {
        TournamentRepository tournamentRepository = mock(TournamentRepository.class);
        TournamentParticipantRepository participantRepository = mock(TournamentParticipantRepository.class);
        TournamentMatchRepository matchRepository = mock(TournamentMatchRepository.class);
        GameRepository gameRepository = mock(GameRepository.class);
        User first = player(1L, "first");
        User second = player(2L, "second");
        Tournament tournament = tournament(first, 2);
        TournamentMatch match = match(tournament, "drawn-game", 1, 1, first, second);
        match.getGame().setStatus("DRAW");
        when(matchRepository.findByGame_Id("drawn-game")).thenReturn(Optional.of(match));
        when(matchRepository.findByTournament_IdAndRoundNumberOrderByMatchNumberAsc("tournament-1", 1))
            .thenReturn(List.of(match));

        TournamentService service = new TournamentService(tournamentRepository, participantRepository,
                matchRepository, gameRepository, mock(UserRepository.class));
        service.completeGame("drawn-game", null);

        assertEquals("COMPLETED", match.getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(match.getWinner().equals(first) || match.getWinner().equals(second));
        verify(matchRepository).save(match);
        verify(gameRepository, org.mockito.Mockito.never()).save(any(Game.class));
        verify(tournamentRepository).save(tournament);
    }

    @Test
    void letsEitherParticipantResolveAnExistingDrawByRandomChoice() {
        TournamentRepository tournamentRepository = mock(TournamentRepository.class);
        TournamentParticipantRepository participantRepository = mock(TournamentParticipantRepository.class);
        TournamentMatchRepository matchRepository = mock(TournamentMatchRepository.class);
        GameRepository gameRepository = mock(GameRepository.class);
        User first = player(1L, "first");
        User second = player(2L, "second");
        Tournament tournament = tournament(first, 2);
        TournamentMatch match = match(tournament, "old-draw", 1, 1, first, second);
        match.setStatus("DRAW");
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(match));
        when(matchRepository.findByTournament_IdOrderByRoundNumberAscMatchNumberAsc("tournament-1"))
            .thenReturn(List.of(match));
        when(participantRepository.findByTournament_IdOrderBySeedAsc("tournament-1"))
            .thenReturn(List.of(participant(first, 1), participant(second, 2)));
        when(matchRepository.findByTournament_IdAndRoundNumberOrderByMatchNumberAsc("tournament-1", 1))
            .thenReturn(List.of(match));
        when(participantRepository.findByTournament_IdOrderBySeedAsc("tournament-1"))
            .thenReturn(List.of(participant(first, 1), participant(second, 2)));
        TournamentService service = new TournamentService(tournamentRepository, participantRepository,
            matchRepository, gameRepository, mock(UserRepository.class));

        TournamentView updated = service.resolveDrawRandomly("match-1", 2L);

        assertEquals("COMPLETED", updated.matches().getFirst().status());
        assertEquals("COMPLETED", match.getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(match.getWinner().equals(first) || match.getWinner().equals(second));
        verify(gameRepository, org.mockito.Mockito.never()).save(any(Game.class));
    }

    @Test
    void startsFirstRoundWithSeededPlayerPairs() {
        TournamentRepository tournamentRepository = mock(TournamentRepository.class);
        TournamentParticipantRepository participantRepository = mock(TournamentParticipantRepository.class);
        TournamentMatchRepository matchRepository = mock(TournamentMatchRepository.class);
        GameRepository gameRepository = mock(GameRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        User owner = player(1L, "owner");
        User opponent = player(2L, "opponent");
        Tournament tournament = tournament(owner, 2);
        when(tournamentRepository.findById("tournament-1")).thenReturn(Optional.of(tournament));
        when(participantRepository.findByTournament_IdOrderBySeedAsc("tournament-1"))
                .thenReturn(List.of(participant(owner, 1), participant(opponent, 2)));
        when(matchRepository.findByTournament_IdOrderByRoundNumberAscMatchNumberAsc("tournament-1"))
                .thenReturn(List.of());
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchRepository.save(any(TournamentMatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TournamentService service = new TournamentService(tournamentRepository, participantRepository,
                matchRepository, gameRepository, userRepository);
        service.start("tournament-1", 1L);

        ArgumentCaptor<Game> gameCaptor = ArgumentCaptor.forClass(Game.class);
        verify(gameRepository).save(gameCaptor.capture());
        assertEquals(owner, gameCaptor.getValue().getWhitePlayer());
        assertEquals(opponent, gameCaptor.getValue().getBlackPlayer());
        assertEquals("RUNNING", tournament.getStatus());
    }

    @Test
    void advancesOnlyAfterEveryMatchInRoundHasAWinner() {
        TournamentRepository tournamentRepository = mock(TournamentRepository.class);
        TournamentParticipantRepository participantRepository = mock(TournamentParticipantRepository.class);
        TournamentMatchRepository matchRepository = mock(TournamentMatchRepository.class);
        GameRepository gameRepository = mock(GameRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        User first = player(1L, "first");
        User second = player(2L, "second");
        User third = player(3L, "third");
        User fourth = player(4L, "fourth");
        Tournament tournament = tournament(first, 4);
        TournamentMatch firstMatch = match(tournament, "game-1", 1, 1, first, second);
        TournamentMatch secondMatch = match(tournament, "game-2", 1, 2, third, fourth);
        when(matchRepository.findByGame_Id("game-1")).thenReturn(Optional.of(firstMatch));
        when(matchRepository.findByGame_Id("game-2")).thenReturn(Optional.of(secondMatch));
        when(matchRepository.findByTournament_IdAndRoundNumberOrderByMatchNumberAsc("tournament-1", 1))
                .thenReturn(List.of(firstMatch, secondMatch));
        when(userRepository.findById(1L)).thenReturn(Optional.of(first));
        when(userRepository.findById(3L)).thenReturn(Optional.of(third));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchRepository.save(any(TournamentMatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TournamentService service = new TournamentService(tournamentRepository, participantRepository,
                matchRepository, gameRepository, userRepository);
        service.completeGame("game-1", 1L);
        verify(gameRepository, org.mockito.Mockito.never()).save(any(Game.class));
        service.completeGame("game-2", 3L);

        ArgumentCaptor<Game> gameCaptor = ArgumentCaptor.forClass(Game.class);
        verify(gameRepository).save(gameCaptor.capture());
        assertEquals(first, gameCaptor.getValue().getWhitePlayer());
        assertEquals(third, gameCaptor.getValue().getBlackPlayer());

        ArgumentCaptor<TournamentMatch> matchCaptor = ArgumentCaptor.forClass(TournamentMatch.class);
        verify(matchRepository, org.mockito.Mockito.times(3)).save(matchCaptor.capture());
        TournamentMatch nextRound = matchCaptor.getAllValues().get(2);
        assertEquals(2, nextRound.getRoundNumber());
        assertEquals(first, nextRound.getWhitePlayer());
        assertEquals(third, nextRound.getBlackPlayer());
    }

    private static Tournament tournament(User owner, int maxPlayers) {
        Tournament tournament = new Tournament();
        tournament.setId("tournament-1");
        tournament.setOwner(owner);
        tournament.setName("Cup");
        tournament.setMaxPlayers(maxPlayers);
        tournament.setStatus("WAITING");
        return tournament;
    }

    private static TournamentParticipant participant(User user, int seed) {
        TournamentParticipant participant = new TournamentParticipant();
        participant.setUser(user);
        participant.setSeed(seed);
        return participant;
    }

    private static TournamentMatch match(Tournament tournament, String gameId, int round, int number,
                                         User white, User black) {
        Game game = new Game();
        game.setId(gameId);
        TournamentMatch match = new TournamentMatch();
        match.setId("match-" + number);
        match.setTournament(tournament);
        match.setRoundNumber(round);
        match.setMatchNumber(number);
        match.setWhitePlayer(white);
        match.setBlackPlayer(black);
        match.setGame(game);
        match.setStatus("PLAYING");
        return match;
    }

    private static User player(Long id, String username) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getUsername()).thenReturn(username);
        return user;
    }
}