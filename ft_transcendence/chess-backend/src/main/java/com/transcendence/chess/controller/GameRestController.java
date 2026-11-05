// Agrupa los endpoints REST para consultar partidas.
package com.transcendence.chess.controller;

// Mapea peticiones HTTP GET.
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transcendence.chess.model.GameState;
import com.transcendence.chess.service.AuthService;
import com.transcendence.chess.service.GameService;

// Controlador REST para consultas de partidas.
@RestController
// Prefijo común de las rutas expuestas por esta clase.
@RequestMapping("/api/games")
public class GameRestController {

    // Servicio que administra el estado de las partidas.
    private final GameService gameService;
    // Servicio que extrae el usuario autenticado de la cabecera.
    private final AuthService authService;

    // Constructor con las dependencias que Spring debe proporcionar.
    public GameRestController(GameService gameService, AuthService authService) {
        // Guarda el servicio de partidas.
        this.gameService = gameService;
        // Guarda el servicio de autenticación.
        this.authService = authService;
    }

    // Atiende GET /api/games/{gameId}.
    @GetMapping("/{gameId}")
    // Recibe el id de la partida en la ruta y la autorización en la cabecera.
    public GameState getGame(@PathVariable String gameId, @RequestHeader("Authorization") String authorization) {
        // Resuelve al usuario y pide la partida; el servicio puede comprobar si tiene permiso para verla.
        return gameService.getGame(gameId, authService.requireUserId(authorization));
    }
}
