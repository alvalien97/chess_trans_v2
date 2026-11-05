// Paquete que contiene la lógica de negocio del backend.
package com.transcendence.chess.service;

// Reloj y duraciones para controlar el tiempo de espera sin depender directamente del sistema.
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

// Administra usuarios en espera y crea partidas al encontrar un rival.
@Service
public class MatchmakingService {
    // Tiempo máximo sin consultar el estado antes de retirar a un usuario de la cola.
    private static final Duration QUEUE_TIMEOUT = Duration.ofSeconds(15);

    // Lista ordenada de ids de usuario que esperan rival.
    private final List<Long> waitingPlayers = new ArrayList<>();
    // Conjunto auxiliar para comprobar rápidamente si un usuario ya está en la cola.
    private final Set<Long> queuedPlayers = new HashSet<>();
    // Último momento en que cada usuario de la cola comprobó su estado.
    private final Map<Long, Instant> lastSeen = new HashMap<>();
    // Resultado de emparejamiento aún pendiente de recoger por cada usuario.
    private final Map<Long, MatchmakingStatus> pendingMatches = new HashMap<>();
    // Repositorios para crear partidas y cargar a sus participantes.
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    // Constructor usado por Spring; utiliza el reloj UTC real en producción.
    @Autowired
    public MatchmakingService(GameRepository gameRepository, UserRepository userRepository) {
        // Delega en el constructor que permite proporcionar un reloj explícito.
        this(gameRepository, userRepository, Clock.systemUTC());
    }

    // Constructor de paquete que permite inyectar un reloj controlado en pruebas.
    MatchmakingService(GameRepository gameRepository, UserRepository userRepository, Clock clock) {
        // Conserva el repositorio de partidas.
        this.gameRepository = gameRepository;
        // Conserva el repositorio de usuarios.
        this.userRepository = userRepository;
        // Conserva el reloj usado para medir inactividad.
        this.clock = clock;
    }

    // Entra en la cola o empareja al usuario con alguien que ya esperaba.
    @Transactional
    public synchronized MatchmakingStatus joinQueue(Long userId) {
        // Limpia primero a los usuarios que llevan demasiado tiempo inactivos.
        removeInactivePlayers();
        // Una solicitud nueva consume cualquier resultado anterior no recogido.
        pendingMatches.remove(userId);
        // Evita duplicar al usuario si ya estaba esperando.
        if (queuedPlayers.contains(userId)) {
            // Renueva la actividad mientras el cliente vuelve a entrar o consultar.
            lastSeen.put(userId, clock.instant());
            // Informa de que sigue esperando.
            return waitingStatus();
        }

        // Si nadie espera, añade al usuario como primer integrante de la cola.
        if (waitingPlayers.isEmpty()) {
            // Registra su id en la lista ordenada.
            waitingPlayers.add(userId);
            // Registra también el id en el conjunto de búsqueda rápida.
            queuedPlayers.add(userId);
            // Anota el momento inicial de su actividad.
            lastSeen.put(userId, clock.instant());
            // Informa de que aún no tiene rival.
            return waitingStatus();
        }

        // Elige al azar una posición entre quienes esperan.
        int opponentIndex = ThreadLocalRandom.current().nextInt(waitingPlayers.size());
        // Saca a ese usuario de la cola como rival.
        Long opponentId = waitingPlayers.remove(opponentIndex);
        // Elimina también su marca de usuario en cola.
        queuedPlayers.remove(opponentId);
        // Ya no necesita tiempo de actividad mientras espera.
        lastSeen.remove(opponentId);
        // Carga al usuario que esperaba para asignarlo al lado blanco.
        User whitePlayer = userRepository.findById(opponentId)
                .orElseThrow(() -> new IllegalStateException("El usuario en espera ya no existe."));
        // Carga al usuario actual para asignarlo al lado negro.
        User blackPlayer = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("El usuario autenticado ya no existe."));

    // Crea una partida con identificador aleatorio.
        Game game = new Game();
        game.setId(UUID.randomUUID().toString());
        // Asigna al rival que esperaba las piezas blancas.
        game.setWhitePlayer(whitePlayer);
        // Asigna al usuario que acaba de entrar las piezas negras.
        game.setBlackPlayer(blackPlayer);
        // Marca la partida lista para jugar.
        game.setStatus("PLAYING");
        // Guarda la partida en la base de datos.
        gameRepository.save(game);

        // Guarda para el rival un resultado que podrá consultar después.
        pendingMatches.put(opponentId, statusFor(game, opponentId));
        // Devuelve al usuario actual los datos de su nuevo emparejamiento.
        return statusFor(game, userId);
    }

    // Consulta la cola o recoge un resultado de emparejamiento pendiente.
    @Transactional(readOnly = true)
    public synchronized MatchmakingStatus getStatus(Long userId) {
        // Retira usuarios inactivos antes de contestar.
        removeInactivePlayers();
        // Si el emparejamiento ya ocurrió, consume y devuelve su resultado.
        MatchmakingStatus pendingMatch = pendingMatches.remove(userId);
        if (pendingMatch != null) {
            return pendingMatch;
        }
        // Si el usuario no está esperando, informa de que está libre.
        if (!queuedPlayers.contains(userId)) {
            return new MatchmakingStatus("IDLE", null, null, null);
        }
        // Renueva su última actividad al consultar el estado.
        lastSeen.put(userId, clock.instant());
        // Informa de que sigue en la cola.
        return waitingStatus();
    }

    // Retira al usuario de todos los mapas y listas de matchmaking.
    public synchronized void leaveQueue(Long userId) {
        // Elimina su indicador de pertenencia.
        queuedPlayers.remove(userId);
        // Elimina su posición de espera.
        waitingPlayers.remove(userId);
        // Elimina su marca de actividad.
        lastSeen.remove(userId);
        // Elimina un emparejamiento pendiente asociado a él.
        pendingMatches.remove(userId);
    }

    // Elimina a quienes no han consultado su estado dentro del tiempo límite.
    private void removeInactivePlayers() {
        // Calcula el instante límite restando el tiempo permitido al momento actual.
        Instant cutoff = clock.instant().minus(QUEUE_TIMEOUT);
        // Recopila ids cuya última actividad es anterior al límite.
        List<Long> inactivePlayers = lastSeen.entrySet().stream()
                .filter(entry -> entry.getValue().isBefore(cutoff))
                .map(Map.Entry::getKey)
                .toList();
            // Limpia cada usuario inactivo de todas las estructuras relacionadas con la cola.
        for (Long inactivePlayer : inactivePlayers) {
            queuedPlayers.remove(inactivePlayer);
            waitingPlayers.remove(inactivePlayer);
            lastSeen.remove(inactivePlayer);
        }
    }

    // Construye el resultado de emparejamiento desde el punto de vista de uno de los jugadores.
    private MatchmakingStatus statusFor(Game game, Long userId) {
        // Determina si el usuario es el jugador blanco.
        boolean isWhite = game.getWhitePlayer().getId().equals(userId);
        // Selecciona al otro jugador como rival.
        User opponent = isWhite ? game.getBlackPlayer() : game.getWhitePlayer();
        // Devuelve el id de partida, el color local y el nombre del rival.
        return new MatchmakingStatus("MATCHED", game.getId(), isWhite ? 0 : 1, opponent.getUsername());
    }

    // Construye una respuesta estándar para alguien que sigue esperando.
    private MatchmakingStatus waitingStatus() {
        return new MatchmakingStatus("WAITING", null, null, null);
    }
}