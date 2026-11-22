//Los tests pertenecen al mismo paquete que TournamentService.
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

//Verifica permisos, inscripción, encuentros y progresión del cuadro.
class TournamentServiceTest {

    //El propietario puede borrar un torneo pendiente y sus inscripciones.
    @Test
    void deletesWaitingTournamentAndItsParticipantsForOwner() {
        //Simula los repositorios necesarios y prepara al propietario.
        TournamentRepository tournamentRepository = mock(TournamentRepository.class);
        TournamentParticipantRepository participantRepository = mock(TournamentParticipantRepository.class);
        User owner = player(1L, "owner");
        //Crea un torneo todavía en espera y configura que la búsqueda lo encuentre.
        Tournament tournament = tournament(owner, 2);
        when(tournamentRepository.findById("tournament-1")).thenReturn(Optional.of(tournament));

        TournamentService service = new TournamentService(tournamentRepository, participantRepository,
                mock(TournamentMatchRepository.class), mock(GameRepository.class), mock(UserRepository.class));
        //Solicita el borrado desde la cuenta propietaria.
        service.delete("tournament-1", 1L);

        //El servicio elimina primero participantes y después el torneo.
        verify(participantRepository).deleteByTournament_Id("tournament-1");
        verify(tournamentRepository).delete(tournament);
    }

    //Un usuario que no es propietario no puede borrar el torneo.
    @Test
    void refusesToDeleteTournamentForAnotherUser() {
        TournamentRepository tournamentRepository = mock(TournamentRepository.class);
        TournamentParticipantRepository participantRepository = mock(TournamentParticipantRepository.class);
        Tournament tournament = tournament(player(1L, "owner"), 2);
        when(tournamentRepository.findById("tournament-1")).thenReturn(Optional.of(tournament));

        TournamentService service = new TournamentService(tournamentRepository, participantRepository,
                mock(TournamentMatchRepository.class), mock(GameRepository.class), mock(UserRepository.class));
        //Espera una excepción HTTP de acceso prohibido.
        assertThrows(ResponseStatusException.class, () -> service.delete("tournament-1", 2L));

        //Comprueba que ningún dato se elimina tras el rechazo.
        verify(participantRepository, org.mockito.Mockito.never()).deleteByTournament_Id("tournament-1");
        verify(tournamentRepository, org.mockito.Mockito.never()).delete(tournament);
    }

    //Un torneo que ya empezó no se puede borrar aunque lo solicite su propietario.
    @Test
    void refusesToDeleteTournamentAfterItHasStarted() {
        TournamentRepository tournamentRepository = mock(TournamentRepository.class);
        TournamentParticipantRepository participantRepository = mock(TournamentParticipantRepository.class);
        //Marca el torneo como iniciado antes de ejecutar la operación.
        Tournament tournament = tournament(player(1L, "owner"), 2);
        tournament.setStatus("RUNNING");
        when(tournamentRepository.findById("tournament-1")).thenReturn(Optional.of(tournament));

        TournamentService service = new TournamentService(tournamentRepository, participantRepository,
                mock(TournamentMatchRepository.class), mock(GameRepository.class), mock(UserRepository.class));
        //La regla de estado debe impedir el borrado.
        assertThrows(IllegalArgumentException.class, () -> service.delete("tournament-1", 1L));

        //Confirma que no se borraron ni participantes ni torneo.
        verify(participantRepository, org.mockito.Mockito.never()).deleteByTournament_Id("tournament-1");
        verify(tournamentRepository, org.mockito.Mockito.never()).delete(tournament);
    }

    //Una partida empatada en un torneo se resuelve al azar para que avance alguien.
    @Test
    void randomlyAdvancesOnePlayerWhenTournamentGameIsDrawn() {
        //Prepara los repositorios y dos jugadores.
        TournamentRepository tournamentRepository = mock(TournamentRepository.class);
        TournamentParticipantRepository participantRepository = mock(TournamentParticipantRepository.class);
        TournamentMatchRepository matchRepository = mock(TournamentMatchRepository.class);
        GameRepository gameRepository = mock(GameRepository.class);
        User first = player(1L, "first");
        User second = player(2L, "second");
        Tournament tournament = tournament(first, 2);
        //Crea el encuentro y simula que su partida terminó en tablas.
        TournamentMatch match = match(tournament, "drawn-game", 1, 1, first, second);
        match.getGame().setStatus("DRAW");
        when(matchRepository.findByGame_Id("drawn-game")).thenReturn(Optional.of(match));
        when(matchRepository.findByTournament_IdAndRoundNumberOrderByMatchNumberAsc("tournament-1", 1))
            .thenReturn(List.of(match));

        TournamentService service = new TournamentService(tournamentRepository, participantRepository,
                matchRepository, gameRepository, mock(UserRepository.class));
        //null como winnerId indica que el resultado fue tablas.
        service.completeGame("drawn-game", null);

        //Un participante queda como ganador, y el cuadro puede completarse.
        assertEquals("COMPLETED", match.getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(match.getWinner().equals(first) || match.getWinner().equals(second));
        verify(matchRepository).save(match);
        verify(gameRepository, org.mockito.Mockito.never()).save(any(Game.class));
        verify(tournamentRepository).save(tournament);
    }

    //Cualquiera de los participantes puede pedir que se resuelva un empate existente.
    @Test
    void letsEitherParticipantResolveAnExistingDrawByRandomChoice() {
        //Prepara la infraestructura simulada y los usuarios.
        TournamentRepository tournamentRepository = mock(TournamentRepository.class);
        TournamentParticipantRepository participantRepository = mock(TournamentParticipantRepository.class);
        TournamentMatchRepository matchRepository = mock(TournamentMatchRepository.class);
        GameRepository gameRepository = mock(GameRepository.class);
        User first = player(1L, "first");
        User second = player(2L, "second");
        Tournament tournament = tournament(first, 2);
        //Crea un encuentro marcado como empatado.
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

        //El segundo participante solicita resolver el empate.
        TournamentView updated = service.resolveDrawRandomly("match-1", 2L);

        //La vista devuelta y la entidad reflejan un encuentro completado.
        assertEquals("COMPLETED", updated.matches().getFirst().status());
        assertEquals("COMPLETED", match.getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(match.getWinner().equals(first) || match.getWinner().equals(second));
        verify(gameRepository, org.mockito.Mockito.never()).save(any(Game.class));
    }

    //Iniciar un cuadro completo crea la primera ronda siguiendo el orden de seed.
    @Test
    void startsFirstRoundWithSeededPlayerPairs() {
        //Prepara repositorios, organizador y rival.
        TournamentRepository tournamentRepository = mock(TournamentRepository.class);
        TournamentParticipantRepository participantRepository = mock(TournamentParticipantRepository.class);
        TournamentMatchRepository matchRepository = mock(TournamentMatchRepository.class);
        GameRepository gameRepository = mock(GameRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        User owner = player(1L, "owner");
        User opponent = player(2L, "opponent");
        Tournament tournament = tournament(owner, 2);
        when(tournamentRepository.findById("tournament-1")).thenReturn(Optional.of(tournament));
        //Simula dos participantes ordenados por su posición inicial.
        when(participantRepository.findByTournament_IdOrderBySeedAsc("tournament-1"))
                .thenReturn(List.of(participant(owner, 1), participant(opponent, 2)));
        when(matchRepository.findByTournament_IdOrderByRoundNumberAscMatchNumberAsc("tournament-1"))
                .thenReturn(List.of());
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchRepository.save(any(TournamentMatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TournamentService service = new TournamentService(tournamentRepository, participantRepository,
                matchRepository, gameRepository, userRepository);
        //El propietario inicia el torneo.
        service.start("tournament-1", 1L);

        //Captura la partida creada para revisar asignación de colores.
        ArgumentCaptor<Game> gameCaptor = ArgumentCaptor.forClass(Game.class);
        verify(gameRepository).save(gameCaptor.capture());
        assertEquals(owner, gameCaptor.getValue().getWhitePlayer());
        assertEquals(opponent, gameCaptor.getValue().getBlackPlayer());
        //El torneo cambia a RUNNING al crear la primera ronda.
        assertEquals("RUNNING", tournament.getStatus());
    }

    //La siguiente ronda se crea solo cuando todos los encuentros de la actual están completos.
    @Test
    void advancesOnlyAfterEveryMatchInRoundHasAWinner() {
        //Prepara repositorios y cuatro jugadores para formar dos encuentros.
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
        //Crea los dos encuentros de la primera ronda.
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
        //Completar solo el primer encuentro aún no debe abrir una ronda nueva.
        service.completeGame("game-1", 1L);
        verify(gameRepository, org.mockito.Mockito.never()).save(any(Game.class));
        //Al completarse también el segundo encuentro, pasan los dos ganadores.
        service.completeGame("game-2", 3L);

        //Captura la partida y el encuentro creados para la siguiente ronda.
        ArgumentCaptor<Game> gameCaptor = ArgumentCaptor.forClass(Game.class);
        verify(gameRepository).save(gameCaptor.capture());
        assertEquals(first, gameCaptor.getValue().getWhitePlayer());
        assertEquals(third, gameCaptor.getValue().getBlackPlayer());

        ArgumentCaptor<TournamentMatch> matchCaptor = ArgumentCaptor.forClass(TournamentMatch.class);
        verify(matchRepository, org.mockito.Mockito.times(3)).save(matchCaptor.capture());
        //Los dos primeros guardados son resultados de la ronda actual; el tercero es el nuevo encuentro.
        TournamentMatch nextRound = matchCaptor.getAllValues().get(2);
        assertEquals(2, nextRound.getRoundNumber());
        assertEquals(first, nextRound.getWhitePlayer());
        assertEquals(third, nextRound.getBlackPlayer());
    }

    //Crea un torneo de prueba con propietario y número máximo de participantes.
    private static Tournament tournament(User owner, int maxPlayers) {
        //Prepara la entidad con valores conocidos para las aserciones.
        Tournament tournament = new Tournament();
        tournament.setId("tournament-1");
        tournament.setOwner(owner);
        tournament.setName("Cup");
        tournament.setMaxPlayers(maxPlayers);
        tournament.setStatus("WAITING");
        return tournament;
    }

    //Construye una inscripción de prueba con usuario y posición inicial.
    private static TournamentParticipant participant(User user, int seed) {
        TournamentParticipant participant = new TournamentParticipant();
        participant.setUser(user);
        participant.setSeed(seed);
        return participant;
    }

    //Construye un encuentro enlazado a torneo, jugadores y una partida simulada.
    private static TournamentMatch match(Tournament tournament, String gameId, int round, int number,
                                         User white, User black) {
        //Crea la partida asociada.
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

    //Crea un jugador mockeado con id y nombre conocidos.
    private static User player(Long id, String username) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getUsername()).thenReturn(username);
        return user;
    }
}