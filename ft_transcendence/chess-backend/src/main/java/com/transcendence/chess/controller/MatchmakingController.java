//Agrupa los endpoints HTTP de la cola de emparejamiento.
package com.transcendence.chess.controller;

//Mapea peticiones HTTP DELETE.
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transcendence.chess.dto.MatchmakingStatus;
import com.transcendence.chess.service.AuthService;
import com.transcendence.chess.service.MatchmakingService;

//Expone operaciones REST para entrar, consultar y abandonar la cola.
@RestController
//Prefijo común que forma las rutas /api/matchmaking/...
@RequestMapping("/api/matchmaking")
public class MatchmakingController {
    //Convierte la cabecera de autorización en un id de usuario.
    private final AuthService authService;
    //Gestiona la cola de emparejamiento.
    private final MatchmakingService matchmakingService;

    //Constructor usado por Spring para inyectar ambos servicios.
    public MatchmakingController(AuthService authService, MatchmakingService matchmakingService) {
        //Guarda el servicio de autenticación.
        this.authService = authService;
        //Guarda el servicio de matchmaking.
        this.matchmakingService = matchmakingService;
    }

    //Atiende POST /api/matchmaking/queue para entrar en la cola.
    @PostMapping("/queue")
    //Requiere que el cliente se haya autenticado.
    public MatchmakingStatus joinQueue(@RequestHeader("Authorization") String authorization) {
        //Identifica al usuario y delega su entrada en la cola.
        return matchmakingService.joinQueue(authService.requireUserId(authorization));
    }

    //Atiende GET /api/matchmaking/status para consultar el progreso del usuario.
    @GetMapping("/status")
    //Requiere la cabecera con la sesión del usuario.
    public MatchmakingStatus getStatus(@RequestHeader("Authorization") String authorization) {
        //Resuelve al usuario y devuelve su estado de emparejamiento.
        return matchmakingService.getStatus(authService.requireUserId(authorization));
    }

    //Atiende DELETE /api/matchmaking/queue para salir de la cola.
    @DeleteMapping("/queue")
    //Requiere identificar al usuario que abandona.
    public void leaveQueue(@RequestHeader("Authorization") String authorization) {
        //Resuelve al usuario y delega su salida; no se devuelve un objeto de respuesta.
        matchmakingService.leaveQueue(authService.requireUserId(authorization));
    }
}