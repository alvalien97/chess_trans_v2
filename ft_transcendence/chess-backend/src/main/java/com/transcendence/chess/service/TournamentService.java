// Paquete que contiene la lógica de negocio del backend.
package com.transcendence.chess.service;

// Colecciones, comparaciones e identificadores aleatorios usados por el torneo.
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

// Coordina torneos, participantes, encuentros y partidas asociadas.
@Service
public class TournamentService {
    // Repositorio de torneos.
    private final TournamentRepository tournamentRepository;
    // Repositorio de inscripciones.
    private final TournamentParticipantRepository participantRepository;
    // Repositorio de encuentros del cuadro.
    private final TournamentMatchRepository matchRepository;
    // Repositorio de partidas de ajedrez que se crean para cada encuentro.
    private final GameRepository gameRepository;
    // Repositorio para cargar usuarios participantes.
    private final UserRepository userRepository;

    // Spring inyecta los repositorios necesarios para gestionar el torneo completo.
    public TournamentService(TournamentRepository tournamentRepository,
                             TournamentParticipantRepository participantRepository,
                             TournamentMatchRepository matchRepository,
                             GameRepository gameRepository,
                             UserRepository userRepository) {
        // Guarda el repositorio de torneos.
        this.tournamentRepository = tournamentRepository;
        // Guarda el repositorio de participantes.
        this.participantRepository = participantRepository;
        // Guarda el repositorio de encuentros.
        this.matchRepository = matchRepository;
        // Guarda el repositorio de partidas.
        this.gameRepository = gameRepository;
        // Guarda el repositorio de usuarios.
        this.userRepository = userRepository;
    }

    // Crea un torneo y registra automáticamente a quien lo organiza.
    @Transactional
    public TournamentView create(Long ownerId, TournamentRequest request) {
        // Este formato de cuadro solo permite 2, 4 u 8 jugadores.
        if (!List.of(2, 4, 8).contains(request.maxPlayers())) {
            throw new IllegalArgumentException("El cuadro admite 2, 4 u 8 jugadores.");
        }
        // Carga al organizador o informa que no existe.
        User owner = requireUser(ownerId);
        // Crea la nueva entidad de torneo.
        Tournament tournament = new Tournament();
        // Usa un UUID como identificador textual.
        tournament.setId(UUID.randomUUID().toString());
        // Elimina espacios externos del nombre.
        tournament.setName(request.name().trim());
        // Asigna al usuario que organiza.
        tournament.setOwner(owner);
        // Guarda el tamaño máximo elegido.
        tournament.setMaxPlayers(request.maxPlayers());
        // El torneo comienza esperando participantes.
        tournament.setStatus("WAITING");
        // Persiste la entidad del torneo.
        tournamentRepository.save(tournament);
        // Inscribe al organizador como primer participante.
        addParticipant(tournament, owner, 1);
        // Devuelve una vista con los datos actuales del torneo.
        return toView(tournament);
    }

    // Inscribe a un usuario si el torneo sigue abierto y tiene espacio.
    @Transactional
    public synchronized TournamentView join(String tournamentId, Long userId) {
        // Carga el torneo solicitado.
        Tournament tournament = requireTournament(tournamentId);
        // Solo se permite inscribirse antes de que el torneo empiece.
        if (!"WAITING".equals(tournament.getStatus())) {
            throw new IllegalArgumentException("La inscripción de este torneo está cerrada.");
        }
        // Si el usuario ya estaba inscrito, devuelve la vista sin duplicarlo.
        if (participantRepository.existsByTournament_IdAndUser_Id(tournamentId, userId)) {
            return toView(tournament);
        }
        // Cuenta los participantes actuales.
        long count = participantRepository.countByTournament_Id(tournamentId);
        // Impide superar el límite configurado.
        if (count >= tournament.getMaxPlayers()) {
            throw new IllegalArgumentException("El torneo ya está completo.");
        }
        // Asigna la siguiente posición y registra la inscripción.
        addParticipant(tournament, requireUser(userId), (int) count + 1);
        // Devuelve la vista actualizada.
        return toView(tournament);
    }

    // Inicia el torneo si lo solicita el propietario y el cuadro está completo.
    @Transactional
    public synchronized TournamentView start(String tournamentId, Long userId) {
        // Carga el torneo.
        Tournament tournament = requireTournament(tournamentId);
        // Comprueba que quien solicita el inicio sea el organizador.
        requireOwner(tournament, userId);
        // Solo se puede iniciar un torneo que aún espera.
        if (!"WAITING".equals(tournament.getStatus())) {
            throw new IllegalArgumentException("El torneo ya ha comenzado.");
        }
        // Carga participantes en el orden asignado al inscribirse.
        List<TournamentParticipant> participants = participantRepository
                .findByTournament_IdOrderBySeedAsc(tournamentId);
        // Exige que se haya alcanzado exactamente el tamaño configurado del cuadro.
        if (participants.size() != tournament.getMaxPlayers()) {
            throw new IllegalArgumentException("Deben inscribirse todos los jugadores del cuadro.");
        }

        // Cambia el estado del torneo a activo y persiste el cambio.
        tournament.setStatus("RUNNING");
        tournamentRepository.save(tournament);
        // Crea los encuentros de la primera ronda con los usuarios inscritos.
        scheduleRound(tournament, 1, participants.stream().map(TournamentParticipant::getUser).toList());
        // Devuelve el cuadro inicial.
        return toView(tournament);
    }

    // Devuelve todos los torneos en orden cronológico inverso, solo lectura.
    @Transactional(readOnly = true)
    public List<TournamentView> list() {
        // Busca entidades y transforma cada una a la estructura que expone la API.
        return tournamentRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toView).toList();
    }

    // Busca un torneo y construye su vista.
    @Transactional(readOnly = true)
    public TournamentView get(String tournamentId) {
        return toView(requireTournament(tournamentId));
    }

    // Permite al propietario eliminar un torneo que aún no comenzó.
    @Transactional
    public void delete(String tournamentId, Long userId) {
        // Carga el torneo.
        Tournament tournament = requireTournament(tournamentId);
        // Comprueba que quien solicita el borrado sea el propietario.
        requireOwner(tournament, userId);
        // No permite eliminar cuadros que ya comenzaron.
        if (!"WAITING".equals(tournament.getStatus())) {
            throw new IllegalArgumentException("Solo se pueden eliminar torneos pendientes.");
        }
        // Elimina primero las inscripciones asociadas.
        participantRepository.deleteByTournament_Id(tournamentId);
        // Elimina el torneo una vez eliminadas sus relaciones dependientes.
        tournamentRepository.delete(tournament);
    }

    // Marca como terminado un encuentro cuando finaliza su partida.
    @Transactional
    public synchronized void completeGame(String gameId, Long winnerId) {
        // Busca el encuentro asociado a la partida; puede no pertenecer a un torneo.
        TournamentMatch match = matchRepository.findByGame_Id(gameId).orElse(null);
        // Ignora partidas ajenas a torneos y encuentros que ya no están activos.
        if (match == null || !("PLAYING".equals(match.getStatus()) || "REMATCH".equals(match.getStatus()))) {
            return;
        }
        // Si el resultado fue tablas, elige aleatoriamente un ganador para que avance el cuadro.
        if (winnerId == null) {
            User winner = ThreadLocalRandom.current().nextBoolean()
                    ? match.getWhitePlayer()
                    : match.getBlackPlayer();
            completeMatch(match, winner);
            return;
        }
        // Carga al ganador informado.
        User winner = requireUser(winnerId);
        // El ganador debe ser uno de los dos jugadores registrados en el encuentro.
        if (!Objects.equals(winner.getId(), match.getWhitePlayer().getId())
            && !Objects.equals(winner.getId(), match.getBlackPlayer().getId())) {
            throw new IllegalArgumentException("El ganador debe ser uno de los participantes del encuentro.");
        }
        // Completa el encuentro y posiblemente programa la siguiente ronda.
        completeMatch(match, winner);
    }

    // Permite a un participante resolver al azar un encuentro que quedó empatado.
    @Transactional
    public synchronized TournamentView resolveDrawRandomly(String matchId, Long userId) {
        // Busca el encuentro o responde 404.
        TournamentMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Encuentro no encontrado."));
        // Comprueba si quien solicita es jugador de blancas o de negras.
        boolean isParticipant = Objects.equals(match.getWhitePlayer().getId(), userId)
                || Objects.equals(match.getBlackPlayer().getId(), userId);
        if (!isParticipant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los participantes pueden solicitar revancha.");
        }
        // Solo un encuentro marcado DRAW puede resolverse por este método.
        if (!"DRAW".equals(match.getStatus())) {
            throw new IllegalArgumentException("Este encuentro no tiene un empate pendiente.");
        }
        // Decide aleatoriamente qué participante avanza.
        User winner = ThreadLocalRandom.current().nextBoolean()
                ? match.getWhitePlayer()
                : match.getBlackPlayer();
            // Marca al ganador y actualiza el progreso del cuadro.
        completeMatch(match, winner);
            // Devuelve la vista del torneo que contiene el encuentro.
        return toView(match.getTournament());
    }

            // Guarda el ganador y, cuando toda la ronda termina, finaliza o crea la siguiente.
    private void completeMatch(TournamentMatch match, User winner) {
            // Registra quién ganó.
        match.setWinner(winner);
            // Marca el encuentro como resuelto.
        match.setStatus("COMPLETED");
            // Persiste el resultado del encuentro.
        matchRepository.save(match);

            // Carga todos los encuentros de esta ronda en orden.
        List<TournamentMatch> round = matchRepository.findByTournament_IdAndRoundNumberOrderByMatchNumberAsc(
                match.getTournament().getId(), match.getRoundNumber());
            // Espera hasta que todos los encuentros de la ronda estén completados.
        if (round.stream().anyMatch(item -> !"COMPLETED".equals(item.getStatus()))) {
            return;
        }

        // Recopila los ganadores que pasan a la ronda siguiente.
        List<User> winners = round.stream().map(TournamentMatch::getWinner).toList();
        // Un único ganador al final de la ronda significa que ya se completó el torneo.
        if (winners.size() == 1) {
            // Marca el torneo como terminado.
            Tournament tournament = match.getTournament();
            tournament.setStatus("COMPLETED");
            tournamentRepository.save(tournament);
            return;
        }
        // Empareja a los ganadores en la ronda siguiente.
        scheduleRound(match.getTournament(), match.getRoundNumber() + 1, winners);
    }

    // Crea las partidas y registros de encuentro para una ronda eliminatoria.
    private void scheduleRound(Tournament tournament, int roundNumber, List<User> players) {
        // Avanza de dos en dos para formar parejas.
        for (int i = 0; i < players.size(); i += 2) {
            // Asigna al primer usuario de la pareja las blancas.
            User white = players.get(i);
            // Asigna al segundo usuario de la pareja las negras.
            User black = players.get(i + 1);
            // Crea una partida de ajedrez para el encuentro.
            Game game = new Game();
            game.setId(UUID.randomUUID().toString());
            // Configura los participantes de la partida.
            game.setWhitePlayer(white);
            game.setBlackPlayer(black);
            game.setStatus("PLAYING");
            // Persiste la partida antes de enlazarla al encuentro.
            gameRepository.save(game);

            // Crea el registro que sitúa la partida en el cuadro del torneo.
            TournamentMatch match = new TournamentMatch();
            match.setId(UUID.randomUUID().toString());
            // Enlaza el encuentro con el torneo y su numeración.
            match.setTournament(tournament);
            match.setRoundNumber(roundNumber);
            match.setMatchNumber(i / 2 + 1);
            // Guarda quién juega cada color.
            match.setWhitePlayer(white);
            match.setBlackPlayer(black);
            // Enlaza la entidad Game creada arriba.
            match.setGame(game);
            // Marca el encuentro como activo.
            match.setStatus("PLAYING");
            // Persiste el encuentro.
            matchRepository.save(match);
        }
    }

    // Convierte una entidad de torneo y sus relaciones en un DTO de respuesta.
    private TournamentView toView(Tournament tournament) {
        // Obtiene nombres de participantes en orden de posición inicial.
        List<String> participants = participantRepository
                .findByTournament_IdOrderBySeedAsc(tournament.getId()).stream()
                .map(participant -> participant.getUser().getUsername())
                .toList();
            // Obtiene los encuentros ordenados y convierte cada entidad a su vista pública.
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
            // Construye la respuesta completa con el torneo, participantes y encuentros.
        return new TournamentView(tournament.getId(), tournament.getName(), tournament.getStatus(),
                tournament.getOwner().getUsername(), tournament.getMaxPlayers(), participants, matches);
    }

    // Busca un torneo o responde HTTP 404 si no existe.
    private Tournament requireTournament(String tournamentId) {
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Torneo no encontrado."));
    }

    // Busca un usuario o responde HTTP 404 si no existe.
    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));
    }

    // Verifica que el solicitante sea propietario del torneo.
    private void requireOwner(Tournament tournament, Long userId) {
        // Compara el id del propietario con el id autenticado.
        if (!tournament.getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo la persona organizadora puede hacerlo.");
        }
    }

    // Crea y persiste una inscripción con su posición inicial.
    private void addParticipant(Tournament tournament, User user, int seed) {
        // Prepara la entidad de relación.
        TournamentParticipant participant = new TournamentParticipant();
        // Enlaza el torneo.
        participant.setTournament(tournament);
        // Enlaza el usuario inscrito.
        participant.setUser(user);
        // Guarda la posición que determina el orden del cuadro.
        participant.setSeed(seed);
        // Persiste la inscripción.
        participantRepository.save(participant);
    }
}