// Agrupa los endpoints REST de torneos.
package com.transcendence.chess.controller;

// Permite responder con una colección de torneos.
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transcendence.chess.dto.TournamentRequest;
import com.transcendence.chess.dto.TournamentView;
import com.transcendence.chess.service.AuthService;
import com.transcendence.chess.service.TournamentService;

import jakarta.validation.Valid;

// Controlador HTTP que serializa sus respuestas como JSON.
@RestController
// Prefijo común de las rutas /api/tournaments.
@RequestMapping("/api/tournaments")
public class TournamentController {
    // Servicio para verificar la identidad del solicitante.
    private final AuthService authService;
    // Servicio que implementa las operaciones de torneo.
    private final TournamentService tournamentService;

    // Constructor de inyección de dependencias de Spring.
    public TournamentController(AuthService authService, TournamentService tournamentService) {
        // Conserva la dependencia de autenticación.
        this.authService = authService;
        // Conserva la dependencia de torneos.
        this.tournamentService = tournamentService;
    }

    // Atiende GET /api/tournaments para obtener el listado.
    @GetMapping
    // Devuelve todas las vistas de torneo disponibles.
    public List<TournamentView> list() {
        // Delega la consulta al servicio de torneos.
        return tournamentService.list();
    }

    // Atiende POST /api/tournaments para crear un torneo.
    @PostMapping
    // Recibe la autorización y un cuerpo JSON validado con los datos del torneo.
    public TournamentView create(@RequestHeader("Authorization") String authorization,
                                 @Valid @RequestBody TournamentRequest request) {
        // Identifica al creador y delega el alta con los datos recibidos.
        return tournamentService.create(authService.requireUserId(authorization), request);
    }

    // Atiende GET /api/tournaments/{tournamentId}.
    @GetMapping("/{tournamentId}")
    // Devuelve la vista del torneo cuyo id aparece en la ruta.
    public TournamentView get(@PathVariable String tournamentId) {
        // Pide al servicio que busque el torneo.
        return tournamentService.get(tournamentId);
    }

    // Atiende DELETE /api/tournaments/{tournamentId}.
    @DeleteMapping("/{tournamentId}")
    // Recibe el id del torneo y la identidad de quien solicita borrarlo.
    public void delete(@RequestHeader("Authorization") String authorization,
                       @PathVariable String tournamentId) {
        // Delega la eliminación; el servicio puede comprobar que el usuario tenga permiso.
        tournamentService.delete(tournamentId, authService.requireUserId(authorization));
    }

    // Atiende POST /api/tournaments/{tournamentId}/join para unirse al torneo.
    @PostMapping("/{tournamentId}/join")
    // Recibe la autorización y el identificador del torneo.
    public TournamentView join(@RequestHeader("Authorization") String authorization,
                               @PathVariable String tournamentId) {
        // Identifica al participante y delega su inscripción.
        return tournamentService.join(tournamentId, authService.requireUserId(authorization));
    }

    // Atiende POST /api/tournaments/{tournamentId}/start para comenzar el torneo.
    @PostMapping("/{tournamentId}/start")
    // Recibe quién inicia y cuál torneo debe comenzar.
    public TournamentView start(@RequestHeader("Authorization") String authorization,
                                @PathVariable String tournamentId) {
        // Identifica al solicitante y delega el inicio del torneo.
        return tournamentService.start(tournamentId, authService.requireUserId(authorization));
    }

    // Atiende POST /api/tournaments/matches/{matchId}/resolve-draw.
    @PostMapping("/matches/{matchId}/resolve-draw")
    // Recibe la identidad del solicitante y el identificador del encuentro empatado.
    public TournamentView resolveDraw(@RequestHeader("Authorization") String authorization,
                                      @PathVariable String matchId) {
        // Delega en el servicio la resolución aleatoria del empate.
        return tournamentService.resolveDrawRandomly(matchId, authService.requireUserId(authorization));
    }
}