//Agrupa los manejadores de mensajes en tiempo real de las partidas.
package com.transcendence.chess.controller;

//Identidad autenticada asociada a la conexión WebSocket.
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

//Controlador de eventos de juego que usan WebSocket y STOMP.
@Controller
public class ChessController {

    //Servicio que contiene las reglas de negocio de la partida.
    private final GameService gameService;

    //Constructor usado por Spring para inyectar el servicio.
    public ChessController(GameService gameService) {
        //Conserva la dependencia para los manejadores de mensajes.
        this.gameService = gameService;
    }

    //Escucha los movimientos publicados en /app/chess/move/{gameId}.
    @MessageMapping("/chess/move/{gameId}")
    //Publica el nuevo estado de la partida en el canal compartido de esa partida.
    @SendTo("/topic/game/{gameId}")
    //Recibe el id de la partida, el movimiento y la identidad del jugador conectado.
    public GameState handleChessMove(@DestinationVariable String gameId, MoveDTO movePayload, Principal principal) {
        //Delega la jugada con su origen y destino, convirtiendo la identidad autenticada a id numérico.
        return gameService.processMove(
                gameId,
                Long.valueOf(principal.getName()),
                movePayload.getFromCol(),
                movePayload.getFromRow(),
                movePayload.getToCol(),
                movePayload.getToRow()
        );
    }

    //Escucha una petición de rendición para la partida indicada.
    @MessageMapping("/chess/resign/{gameId}")
    //Publica el estado actualizado en el canal de la partida.
    @SendTo("/topic/game/{gameId}")
    //Usa la identidad WebSocket para saber qué jugador se rinde.
    public GameState resign(@DestinationVariable String gameId, Principal principal) {
        //Pide al servicio marcar la rendición y devuelve el resultado.
        return gameService.resign(gameId, Long.valueOf(principal.getName()));
    }

    //Escucha una propuesta de tablas.
    @MessageMapping("/chess/draw-offer/{gameId}")
    //Difunde a ambos jugadores el estado con la propuesta.
    @SendTo("/topic/game/{gameId}")
    //Identifica qué jugador ha ofrecido tablas.
    public GameState offerDraw(@DestinationVariable String gameId, Principal principal) {
        //Delega la propuesta y devuelve el estado actualizado.
        return gameService.offerDraw(gameId, Long.valueOf(principal.getName()));
    }

    //Escucha la respuesta a una propuesta de tablas.
    @MessageMapping("/chess/draw-response/{gameId}")
    //Difunde el estado actualizado de la partida.
    @SendTo("/topic/game/{gameId}")
    //Recibe la aceptación o el rechazo y la identidad del jugador que responde.
    public GameState respondToDraw(@DestinationVariable String gameId, DrawResponseDTO response,
                                   Principal principal) {
        //Delega la respuesta con el id del usuario y el valor booleano de aceptación.
        return gameService.respondToDraw(gameId, Long.valueOf(principal.getName()), response.accepted());
    }

    //Maneja errores de argumento generados por los anteriores controladores de mensajes.
    @MessageExceptionHandler(IllegalArgumentException.class)
    //Envía el mensaje de error únicamente al usuario que originó el mensaje fallido.
    @SendToUser("/queue/errors")
    //Convierte la excepción en texto para que el cliente pueda mostrar el motivo.
    public String handleInvalidMove(IllegalArgumentException exception) {
        //Devuelve el detalle incluido en la excepción.
        return exception.getMessage();
    }
}
