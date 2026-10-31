package com.transcendence.chess.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.transcendence.chess.dto.TournamentRequest;
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

@Service
public class TournamentService {
    private final TournamentRepository tournamentRepository;
    private final TournamentParticipantRepository participantRepository;
    private final TournamentMatchRepository matchRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    public TournamentService(TournamentRepository tournamentRepository,
                             TournamentParticipantRepository participantRepository,
                             TournamentMatchRepository matchRepository,
                             GameRepository gameRepository,
                             UserRepository userRepository) {
        this.tournamentRepository = tournamentRepository;
        this.participantRepository = participantRepository;
        this.matchRepository = matchRepository;
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TournamentView create(Long ownerId, TournamentRequest request) {
        if (!List.of(2, 4, 8).contains(request.maxPlayers())) {
            throw new IllegalArgumentException("El cuadro admite 2, 4 u 8 jugadores.");
        }
        User owner = requireUser(ownerId);
        Tournament tournament = new Tournament();
        tournament.setId(UUID.randomUUID().toString());
        tournament.setName(request.name().trim());
        tournament.setOwner(owner);
        tournament.setMaxPlayers(request.maxPlayers());
        tournament.setStatus("WAITING");
        tournamentRepository.save(tournament);
        addParticipant(tournament, owner, 1);
        return toView(tournament);
    }

    @Transactional
    public synchronized TournamentView join(String tournamentId, Long userId) {
        Tournament tournament = requireTournament(tournamentId);
        if (!"WAITING".equals(tournament.getStatus())) {
            throw new IllegalArgumentException("La inscripción de este torneo está cerrada.");
        }
        if (participantRepository.existsByTournament_IdAndUser_Id(tournamentId, userId)) {
            return toView(tournament);
        }
        long count = participantRepository.countByTournament_Id(tournamentId);
        if (count >= tournament.getMaxPlayers()) {
            throw new IllegalArgumentException("El torneo ya está completo.");
        }
        addParticipant(tournament, requireUser(userId), (int) count + 1);
        return toView(tournament);
    }

    @Transactional
    public synchronized TournamentView start(String tournamentId, Long userId) {
        Tournament tournament = requireTournament(tournamentId);
        requireOwner(tournament, userId);
        if (!"WAITING".equals(tournament.getStatus())) {
            throw new IllegalArgumentException("El torneo ya ha comenzado.");
        }
        List<TournamentParticipant> participants = participantRepository
                .findByTournament_IdOrderBySeedAsc(tournamentId);
        if (participants.size() != tournament.getMaxPlayers()) {
            throw new IllegalArgumentException("Deben inscribirse todos los jugadores del cuadro.");
        }

        tournament.setStatus("RUNNING");
        tournamentRepository.save(tournament);
        scheduleRound(tournament, 1, participants.stream().map(TournamentParticipant::getUser).toList());
        return toView(tournament);
    }

    @Transactional(readOnly = true)
    public List<TournamentView> list() {
        return tournamentRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public TournamentView get(String tournamentId) {
        return toView(requireTournament(tournamentId));
    }

    @Transactional
    public void delete(String tournamentId, Long userId) {
        Tournament tournament = requireTournament(tournamentId);
        requireOwner(tournament, userId);
        if (!"WAITING".equals(tournament.getStatus())) {
            throw new IllegalArgumentException("Solo se pueden eliminar torneos pendientes.");
        }
        participantRepository.deleteByTournament_Id(tournamentId);
        tournamentRepository.delete(tournament);
    }

    @Transactional
    public synchronized void completeGame(String gameId, Long winnerId) {
        TournamentMatch match = matchRepository.findByGame_Id(gameId).orElse(null);
        if (match == null || !("PLAYING".equals(match.getStatus()) || "REMATCH".equals(match.getStatus()))) {
            return;
        }
        if (winnerId == null) {
            User winner = ThreadLocalRandom.current().nextBoolean()
                    ? match.getWhitePlayer()
                    : match.getBlackPlayer();
            completeMatch(match, winner);
            return;
        }
        User winner = requireUser(winnerId);
        if (!Objects.equals(winner.getId(), match.getWhitePlayer().getId())
            && !Objects.equals(winner.getId(), match.getBlackPlayer().getId())) {
            throw new IllegalArgumentException("El ganador debe ser uno de los participantes del encuentro.");
        }
        completeMatch(match, winner);
    }

    @Transactional
    public synchronized TournamentView resolveDrawRandomly(String matchId, Long userId) {
        TournamentMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Encuentro no encontrado."));
        boolean isParticipant = Objects.equals(match.getWhitePlayer().getId(), userId)
                || Objects.equals(match.getBlackPlayer().getId(), userId);
        if (!isParticipant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los participantes pueden solicitar revancha.");
        }
        if (!"DRAW".equals(match.getStatus())) {
            throw new IllegalArgumentException("Este encuentro no tiene un empate pendiente.");
        }
        User winner = ThreadLocalRandom.current().nextBoolean()
                ? match.getWhitePlayer()
                : match.getBlackPlayer();
        completeMatch(match, winner);
        return toView(match.getTournament());
    }

    private void completeMatch(TournamentMatch match, User winner) {
        match.setWinner(winner);
        match.setStatus("COMPLETED");
        matchRepository.save(match);

        List<TournamentMatch> round = matchRepository.findByTournament_IdAndRoundNumberOrderByMatchNumberAsc(
                match.getTournament().getId(), match.getRoundNumber());
        if (round.stream().anyMatch(item -> !"COMPLETED".equals(item.getStatus()))) {
            return;
        }

        List<User> winners = round.stream().map(TournamentMatch::getWinner).toList();
        if (winners.size() == 1) {
            Tournament tournament = match.getTournament();
            tournament.setStatus("COMPLETED");
            tournamentRepository.save(tournament);
            return;
        }
        scheduleRound(match.getTournament(), match.getRoundNumber() + 1, winners);
    }

    private void scheduleRound(Tournament tournament, int roundNumber, List<User> players) {
        for (int i = 0; i < players.size(); i += 2) {
            User white = players.get(i);
            User black = players.get(i + 1);
            Game game = new Game();
            game.setId(UUID.randomUUID().toString());
            game.setWhitePlayer(white);
            game.setBlackPlayer(black);
            game.setStatus("PLAYING");
            gameRepository.save(game);

            TournamentMatch match = new TournamentMatch();
            match.setId(UUID.randomUUID().toString());
            match.setTournament(tournament);
            match.setRoundNumber(roundNumber);
            match.setMatchNumber(i / 2 + 1);
            match.setWhitePlayer(white);
            match.setBlackPlayer(black);
            match.setGame(game);
            match.setStatus("PLAYING");
            matchRepository.save(match);
        }
    }

    private TournamentView toView(Tournament tournament) {
        List<String> participants = participantRepository
                .findByTournament_IdOrderBySeedAsc(tournament.getId()).stream()
                .map(participant -> participant.getUser().getUsername())
                .toList();
        List<TournamentView.MatchView> matches = matchRepository
                .findByTournament_IdOrderByRoundNumberAscMatchNumberAsc(tournament.getId()).stream()
                .map(match -> new TournamentView.MatchView(
                        match.getId(),
                        match.getRoundNumber(),
                        match.getMatchNumber(),
                        match.getWhitePlayer().getId(),
                        match.getWhitePlayer().getUsername(),
                        match.getBlackPlayer().getId(),
                        match.getBlackPlayer().getUsername(),
                        match.getWinner() == null ? null : match.getWinner().getUsername(),
                        match.getGame().getId(),
                        match.getStatus()))
                .toList();
        return new TournamentView(tournament.getId(), tournament.getName(), tournament.getStatus(),
                tournament.getOwner().getUsername(), tournament.getMaxPlayers(), participants, matches);
    }

    private Tournament requireTournament(String tournamentId) {
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Torneo no encontrado."));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));
    }

    private void requireOwner(Tournament tournament, Long userId) {
        if (!tournament.getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo la persona organizadora puede hacerlo.");
        }
    }

    private void addParticipant(Tournament tournament, User user, int seed) {
        TournamentParticipant participant = new TournamentParticipant();
        participant.setTournament(tournament);
        participant.setUser(user);
        participant.setSeed(seed);
        participantRepository.save(participant);
    }
}