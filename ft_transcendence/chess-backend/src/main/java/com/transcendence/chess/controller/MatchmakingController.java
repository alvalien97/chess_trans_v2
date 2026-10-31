package com.transcendence.chess.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transcendence.chess.dto.MatchmakingStatus;
import com.transcendence.chess.service.AuthService;
import com.transcendence.chess.service.MatchmakingService;

@RestController
@RequestMapping("/api/matchmaking")
public class MatchmakingController {
    private final AuthService authService;
    private final MatchmakingService matchmakingService;

    public MatchmakingController(AuthService authService, MatchmakingService matchmakingService) {
        this.authService = authService;
        this.matchmakingService = matchmakingService;
    }

    @PostMapping("/queue")
    public MatchmakingStatus joinQueue(@RequestHeader("Authorization") String authorization) {
        return matchmakingService.joinQueue(authService.requireUserId(authorization));
    }

    @GetMapping("/status")
    public MatchmakingStatus getStatus(@RequestHeader("Authorization") String authorization) {
        return matchmakingService.getStatus(authService.requireUserId(authorization));
    }

    @DeleteMapping("/queue")
    public void leaveQueue(@RequestHeader("Authorization") String authorization) {
        matchmakingService.leaveQueue(authService.requireUserId(authorization));
    }
}