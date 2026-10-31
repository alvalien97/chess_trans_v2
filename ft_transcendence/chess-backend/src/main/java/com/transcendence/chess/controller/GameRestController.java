package com.transcendence.chess.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transcendence.chess.model.GameState;
import com.transcendence.chess.service.AuthService;
import com.transcendence.chess.service.GameService;

@RestController
@RequestMapping("/api/games")
public class GameRestController {

    private final GameService gameService;
    private final AuthService authService;

    public GameRestController(GameService gameService, AuthService authService) {
        this.gameService = gameService;
        this.authService = authService;
    }

    @GetMapping("/{gameId}")
    public GameState getGame(@PathVariable String gameId, @RequestHeader("Authorization") String authorization) {
        return gameService.getGame(gameId, authService.requireUserId(authorization));
    }
}
