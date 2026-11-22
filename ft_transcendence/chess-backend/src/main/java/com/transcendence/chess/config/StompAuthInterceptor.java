package com.transcendence.chess.config;

//Identidad autenticada asociada a un mensaje STOMP.
import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import com.transcendence.chess.service.AuthService;
import com.transcendence.chess.service.GameService;

//Protege las conexiones y mensajes STOMP antes de procesarlos.
@Component
public class StompAuthInterceptor implements ChannelInterceptor {
    //Reconoce destinos de suscripción con formato /topic/game/{gameId}.
    private static final Pattern GAME_TOPIC = Pattern.compile("^/topic/game/([^/]+)$");
    //Servicio que valida credenciales y sesiones.
    private final AuthService authService;
    //Servicio que comprueba si el usuario participa en la partida solicitada.
    private final GameService gameService;
    //Constructor usado por Spring para inyectar dependencias.
    public StompAuthInterceptor(AuthService authService, GameService gameService) {
        //Conserva el servicio de autenticación.
        this.authService = authService;
        //Conserva el servicio de partidas.
        this.gameService = gameService;
    }
    //Se ejecuta antes de que un mensaje entrante llegue al destino STOMP.
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        //Obtiene acceso tipado a las cabeceras STOMP del mensaje.
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        //Si el mensaje no tiene cabeceras STOMP reconocibles, lo deja pasar sin cambios.
        if (accessor == null) {
            return message;
        }

        //CONNECT establece la identidad autenticada de la conexión WebSocket.
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            //Lee el token enviado en la cabecera Authorization.
            String authorization = accessor.getFirstNativeHeader("Authorization");
            //Valida el token y extrae el id del usuario.
            Long userId = authService.requireUserId(authorization);
            //Guarda el token en la sesión WebSocket para futuras comprobaciones.
            if (accessor.getSessionAttributes() != null) {
                accessor.getSessionAttributes().put("authToken", authorization.substring(7));
            }
            //Asigna al mensaje una identidad basada en el id verificado del usuario.
            accessor.setUser(() -> userId.toString());
            //Continúa con el mensaje CONNECT ya asociado al usuario.
            return message;
        }

        //SEND publica datos y SUBSCRIBE solicita escuchar un canal; ambos requieren sesión activa.
        if (StompCommand.SEND.equals(accessor.getCommand()) || StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            //Obtiene la identidad asociada a la conexión.
            Principal principal = accessor.getUser();
            //Sin identidad no se permite enviar ni suscribirse.
            if (principal == null) {
                throw new MessageDeliveryException(message, "Autenticación requerida.");
            }

            //Vuelve a verificar que la sesión no se haya cerrado desde la conexión.
            if (!authService.hasActiveSession(Long.valueOf(principal.getName()))) {
                throw new MessageDeliveryException(message, "La sesión ya no está activa.");
            }

            //Las suscripciones a partidas requieren comprobar que el usuario sea participante.
            if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                //Lee el canal al que el cliente intenta suscribirse.
                String destination = accessor.getDestination();
                //Solo crea un matcher si existe un destino.
                Matcher gameTopic = destination == null ? null : GAME_TOPIC.matcher(destination);
                //Si el canal tiene formato de partida, extrae el id y verifica los permisos.
                if (gameTopic != null && gameTopic.matches()) {
                    gameService.assertPlayer(gameTopic.group(1), Long.valueOf(principal.getName()));
                }
            }
        }

        //Si no se lanzó una excepción, permite que el mensaje continúe por el canal.
        return message;
    }
}