//Paquete que contiene la lógica de negocio del backend.
package com.transcendence.chess.service;

//Utilidad para comparar ids que podrían ser objetos distintos con el mismo valor.
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

//Coordina acciones de partida y su persistencia.
@Service
public class GameService {

    //Conserva en memoria el motor de reglas de cada partida activa.
    private final ConcurrentHashMap<String, ChessRules> activeGames = new ConcurrentHashMap<>();
    //Repositorio que carga y guarda los metadatos de partida.
    private final GameRepository gameRepository;
    //Servicio que actualiza el torneo cuando termina una partida asociada.
    private final TournamentService tournamentService;

    //Spring inyecta el repositorio y el servicio de torneos.
    public GameService(GameRepository gameRepository, TournamentService tournamentService) {
        //Guarda el acceso a la persistencia de partidas.
        this.gameRepository = gameRepository;
        //Guarda el servicio encargado de los efectos sobre torneos.
        this.tournamentService = tournamentService;
    }

    //Ejecuta la consulta dentro de una transacción.
    @Transactional
    //Obtiene el estado solo si la persona solicitante participa en la partida.
    public GameState getGame(String gameId, Long userId) {
        //Busca la partida o responde con error 404.
        Game game = requireGame(gameId);
        //Rechaza a usuarios que no sean jugadores de esta partida.
        assertPlayer(game, userId);
        //Construye el estado que se enviará al cliente.
        return stateFor(game);
    }

    //Serializa las modificaciones en este servicio para evitar dos movimientos simultáneos.
    @Transactional
    public synchronized GameState processMove(String gameId, Long userId, int fromCol, int fromRow, int toCol, int toRow) {
        //Carga la partida solicitada.
        Game game = requireGame(gameId);
        //Comprueba que el autor del movimiento sea uno de los dos jugadores.
        assertPlayer(game, userId);
        //Impide mover cuando la partida ya terminó o todavía no está en juego.
        requirePlaying(game);

        //Determina el color del jugador según su id y la plaza de blancas/negras.
        int playerColor = Objects.equals(game.getWhitePlayer().getId(), userId) ? 0 : 1;
        //Obtiene o crea el motor de reglas asociado a esta partida.
        ChessRules rules = getRules(game);
        //El color al turno del tablero debe coincidir con el jugador que envió la jugada.
        if (rules.getState().getCurrentColor() != playerColor) {
            //Rechaza la jugada fuera de turno.
            throw new IllegalArgumentException("No es tu turno.");
        }

        //Recuerda quién ofreció tablas antes de que el movimiento pueda cancelar esa oferta.
        User drawOfferer = game.getDrawOfferer();
        //Valida y aplica el movimiento sobre el motor de ajedrez.
        GameState state = rules.move(fromCol, fromRow, toCol, toRow);
        //Si movió el jugador contrario al oferente, la oferta pendiente deja de estar vigente.
        if (drawOfferer != null && !drawOfferer.getId().equals(userId)) {
            game.setDrawOfferer(null);
        }
        //Comprueba si el movimiento terminó la partida por mate o tablas.
        if (state.getStatus().equals("CHECKMATE") || state.getStatus().equals("DRAW")
                || state.getStatus().equals("STALEMATE")) {
            //Copia el resultado calculado por ChessRules a la entidad persistente.
            game.setStatus(state.getStatus());
            //En jaque mate, el ganador es el jugador del color contrario al que tiene el turno.
            if (state.getStatus().equals("CHECKMATE")) {
                game.setWinner(state.getCurrentColor() == 1 ? game.getWhitePlayer() : game.getBlackPlayer());
            }
            //Persiste el resultado para que esté disponible después de la operación.
            gameRepository.save(game);
            //Limpia cualquier oferta de tablas que ya no tenga sentido tras finalizar.
            game.setDrawOfferer(null);
            //Notifica al servicio de torneos que la partida terminó y quién ganó, si corresponde.
            tournamentService.completeGame(gameId, game.getWinner() == null ? null : game.getWinner().getId());
        }
        //Devuelve una vista que combina el tablero con los metadatos persistidos de la partida.
        return stateFor(game);
    }

    //Registra una rendición y declara ganador al otro participante.
    @Transactional
    public synchronized GameState resign(String gameId, Long userId) {
        //Carga la partida.
        Game game = requireGame(gameId);
        //Solo puede rendirse alguien que juegue la partida.
        assertPlayer(game, userId);
        //Solo se puede rendir una partida en curso.
        requirePlaying(game);
        //El rival del usuario que se rinde pasa a ser ganador.
        User winner = game.getWhitePlayer().getId().equals(userId)
                ? game.getBlackPlayer()
                : game.getWhitePlayer();
        //Guarda quién ganó.
        game.setWinner(winner);
        //Marca el final con el estado de rendición.
        game.setStatus("RESIGNED");
        //Persiste el resultado.
        gameRepository.save(game);
        //Elimina una posible oferta de tablas pendiente.
        game.setDrawOfferer(null);
        //Actualiza el resultado del torneo, si esta partida forma parte de uno.
        tournamentService.completeGame(gameId, winner.getId());
        //Devuelve el estado final.
        return stateFor(game);
    }

    //Registra una propuesta de tablas de uno de los jugadores.
    @Transactional
    public synchronized GameState offerDraw(String gameId, Long userId) {
        //Carga la partida.
        Game game = requireGame(gameId);
        //Verifica que el solicitante sea participante.
        assertPlayer(game, userId);
        //Las propuestas solo se aceptan durante una partida activa.
        requirePlaying(game);
        //Consulta si ya hay una propuesta pendiente.
        User currentOffer = game.getDrawOfferer();
        //No se permiten ofertas duplicadas ni una nueva oferta mientras responde el rival.
        if (currentOffer != null) {
            //Diferencia entre repetir una oferta propia y no responder a la oferta rival.
            throw new IllegalArgumentException(currentOffer.getId().equals(userId)
                    ? "Ya ofreciste tablas; espera la respuesta del rival."
                    : "Responde primero a la oferta de tablas del rival.");
        }
        //Guarda como oferente al jugador identificado por userId.
        game.setDrawOfferer(game.getWhitePlayer().getId().equals(userId)
                ? game.getWhitePlayer()
                : game.getBlackPlayer());
        //Persiste la oferta pendiente.
        gameRepository.save(game);
        //Devuelve el estado que incluye quién propuso tablas.
        return stateFor(game);
    }

    //Procesa la aceptación o el rechazo de una propuesta de tablas.
    @Transactional
    public synchronized GameState respondToDraw(String gameId, Long userId, boolean accept) {
        //Carga la partida.
        Game game = requireGame(gameId);
        //Verifica que responde uno de los jugadores.
        assertPlayer(game, userId);
        //No se pueden responder ofertas después del final.
        requirePlaying(game);
        //Obtiene quién propuso tablas.
        User offerer = game.getDrawOfferer();
        //Si no había oferta y el jugador acepta, interpreta la acción como una nueva propuesta.
        if (offerer == null && accept) {
            //Guarda al jugador actual como oferente de la nueva propuesta.
            game.setDrawOfferer(game.getWhitePlayer().getId().equals(userId)
                    ? game.getWhitePlayer()
                    : game.getBlackPlayer());
            gameRepository.save(game);
            //Construye el estado e indica por qué la aceptación no cerró la partida.
            GameState state = stateFor(game);
            state.setResultMessage("La oferta anterior ya no estaba pendiente; se ha enviado una nueva oferta.");
            return state;
        }
        //Solo el rival puede contestar una oferta existente.
        if (offerer == null || offerer.getId().equals(userId)) {
            throw new IllegalArgumentException("No tienes una oferta de tablas pendiente del rival.");
        }
        //La respuesta resuelve la oferta, tanto si acepta como si rechaza.
        game.setDrawOfferer(null);
        //Si se acepta, marca tablas, elimina un posible ganador y actualiza el torneo.
        if (accept) {
            game.setStatus("DRAW");
            game.setWinner(null);
            //Persiste el resultado de tablas.
            gameRepository.save(game);
            //Notifica al torneo que la partida acabó sin ganador.
            tournamentService.completeGame(gameId, null);
        }
        //Persiste también el borrado de la oferta cuando se rechaza.
        gameRepository.save(game);
        //Crea la respuesta con el tablero y el mensaje explicativo.
        GameState state = stateFor(game);
        state.setResultMessage(accept ? "Ambos jugadores acordaron tablas." : "La oferta de tablas fue rechazada.");
        return state;
    }

    //Verifica el permiso de participación para el código que solo necesita comprobar acceso.
    @Transactional(readOnly = true)
    public void assertPlayer(String gameId, Long userId) {
        //Reutiliza las comprobaciones comunes de carga y pertenencia.
        assertPlayer(requireGame(gameId), userId);
    }

    //Recupera una partida por id y traduce la ausencia a HTTP 404.
    private Game requireGame(String gameId) {
        //findById devuelve Optional; orElseThrow interrumpe el flujo si no se encontró.
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partida no encontrada."));
    }

    //Comprueba que el usuario sea el jugador de blancas o de negras.
    private void assertPlayer(Game game, Long userId) {
        //Compara el id con el jugador blanco y tolera una relación todavía nula.
        boolean isWhite = game.getWhitePlayer() != null && Objects.equals(game.getWhitePlayer().getId(), userId);
        //Compara el id con el jugador negro y tolera una relación todavía nula.
        boolean isBlack = game.getBlackPlayer() != null && Objects.equals(game.getBlackPlayer().getId(), userId);
        //Si no coincide con ninguno, el usuario no puede acceder a la partida.
        if (!isWhite && !isBlack) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No participas en esta partida.");
        }
    }

    //Exige que el estado persistido sea PLAYING.
    private void requirePlaying(Game game) {
        //Compara poniendo la constante a la izquierda para tolerar un estado null.
        if (!"PLAYING".equals(game.getStatus())) {
            throw new IllegalArgumentException("La partida ya ha terminado.");
        }
    }

    //Compone el estado de tablero y los metadatos de finalización/oferta de la entidad Game.
    private GameState stateFor(Game game) {
        //Obtiene la posición actual del motor de reglas.
        GameState state = getRules(game).getState();
        //Si la base de datos ya marca el final, sustituye el estado calculado del tablero.
        if (!"PLAYING".equals(game.getStatus())) {
            state.setStatus(game.getStatus());
        }
        //Copia al DTO el id de quien ofreció tablas, o null si no hay oferta.
        state.setDrawOfferByUserId(game.getDrawOfferer() == null ? null : game.getDrawOfferer().getId());
        //Copia al DTO el id del ganador, o null si no lo hay.
        state.setWinnerId(game.getWinner() == null ? null : game.getWinner().getId());
        //Añade el mensaje correspondiente a una rendición.
        if ("RESIGNED".equals(game.getStatus())) {
            state.setResultMessage("La partida terminó por rendición.");
        } else if ("DRAW".equals(game.getStatus()) || "STALEMATE".equals(game.getStatus())) {
            //Añade un mensaje común para resultados que terminan en tablas.
            state.setResultMessage("La partida terminó en tablas.");
        }
        //Devuelve el estado enriquecido con datos persistentes de la partida.
        return state;
    }

    //Recupera el motor en memoria o crea uno inicial para este id de partida.
    private ChessRules getRules(Game game) {
        //computeIfAbsent evita crear más de una entrada para la misma clave concurrente.
        return activeGames.computeIfAbsent(game.getId(), id -> new ChessRules());
    }
}
