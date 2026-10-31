package com.transcendence.chess.controller;

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

@RestController
@RequestMapping("/api/tournaments")
public class TournamentController {
    private final AuthService authService;
    private final TournamentService tournamentService;

    public TournamentController(AuthService authService, TournamentService tournamentService) {
        this.authService = authService;
        this.tournamentService = tournamentService;
    }

    @GetMapping
    public List<TournamentView> list() {
        return tournamentService.list();
    }

    @PostMapping
    public TournamentView create(@RequestHeader("Authorization") String authorization,
                                 @Valid @RequestBody TournamentRequest request) {
        return tournamentService.create(authService.requireUserId(authorization), request);
    }

    @GetMapping("/{tournamentId}")
    public TournamentView get(@PathVariable String tournamentId) {
        return tournamentService.get(tournamentId);
    }

    @DeleteMapping("/{tournamentId}")
    public void delete(@RequestHeader("Authorization") String authorization,
                       @PathVariable String tournamentId) {
        tournamentService.delete(tournamentId, authService.requireUserId(authorization));
    }

    @PostMapping("/{tournamentId}/join")
    public TournamentView join(@RequestHeader("Authorization") String authorization,
                               @PathVariable String tournamentId) {
        return tournamentService.join(tournamentId, authService.requireUserId(authorization));
    }

    @PostMapping("/{tournamentId}/start")
    public TournamentView start(@RequestHeader("Authorization") String authorization,
                                @PathVariable String tournamentId) {
        return tournamentService.start(tournamentId, authService.requireUserId(authorization));
    }

    @PostMapping("/matches/{matchId}/resolve-draw")
    public TournamentView resolveDraw(@RequestHeader("Authorization") String authorization,
                                      @PathVariable String matchId) {
        return tournamentService.resolveDrawRandomly(matchId, authService.requireUserId(authorization));
    }
}