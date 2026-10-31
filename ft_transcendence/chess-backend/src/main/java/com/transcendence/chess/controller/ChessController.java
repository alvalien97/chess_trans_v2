package com.transcendence.chess.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import com.transcendence.chess.dto.DrawResponseDTO;
import com.transcendence.chess.dto.MoveDTO;
import com.transcendence.chess.model.GameState;
import com.transcendence.chess.service.GameService;

@Controller
public class ChessController {

    private final GameService gameService;

    public ChessController(GameService gameService) {
        this.gameService = gameService;
    }

    @MessageMapping("/chess/move/{gameId}")
    @SendTo("/topic/game/{gameId}")
    public GameState handleChessMove(@DestinationVariable String gameId, MoveDTO movePayload, Principal principal) {
        return gameService.processMove(
                gameId,
                Long.valueOf(principal.getName()),
                movePayload.getFromCol(),
                movePayload.getFromRow(),
                movePayload.getToCol(),
                movePayload.getToRow()
        );
    }

    @MessageMapping("/chess/resign/{gameId}")
    @SendTo("/topic/game/{gameId}")
    public GameState resign(@DestinationVariable String gameId, Principal principal) {
        return gameService.resign(gameId, Long.valueOf(principal.getName()));
    }

    @MessageMapping("/chess/draw-offer/{gameId}")
    @SendTo("/topic/game/{gameId}")
    public GameState offerDraw(@DestinationVariable String gameId, Principal principal) {
        return gameService.offerDraw(gameId, Long.valueOf(principal.getName()));
    }

    @MessageMapping("/chess/draw-response/{gameId}")
    @SendTo("/topic/game/{gameId}")
    public GameState respondToDraw(@DestinationVariable String gameId, DrawResponseDTO response,
                                   Principal principal) {
        return gameService.respondToDraw(gameId, Long.valueOf(principal.getName()), response.accepted());
    }

    @MessageExceptionHandler(IllegalArgumentException.class)
    @SendToUser("/queue/errors")
    public String handleInvalidMove(IllegalArgumentException exception) {
        return exception.getMessage();
    }
}
