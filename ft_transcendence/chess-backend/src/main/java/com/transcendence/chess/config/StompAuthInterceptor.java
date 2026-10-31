package com.transcendence.chess.config;

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

@Component
public class StompAuthInterceptor implements ChannelInterceptor {
    private static final Pattern GAME_TOPIC = Pattern.compile("^/topic/game/([^/]+)$");

    private final AuthService authService;
    private final GameService gameService;

    public StompAuthInterceptor(AuthService authService, GameService gameService) {
        this.authService = authService;
        this.gameService = gameService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorization = accessor.getFirstNativeHeader("Authorization");
            Long userId = authService.requireUserId(authorization);
            if (accessor.getSessionAttributes() != null) {
                accessor.getSessionAttributes().put("authToken", authorization.substring(7));
            }
            accessor.setUser(() -> userId.toString());
            return message;
        }

        if (StompCommand.SEND.equals(accessor.getCommand())
                || StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            Principal principal = accessor.getUser();
            if (principal == null) {
                throw new MessageDeliveryException(message, "Autenticación requerida.");
            }

            if (!authService.hasActiveSession(Long.valueOf(principal.getName()))) {
                throw new MessageDeliveryException(message, "La sesión ya no está activa.");
            }

            if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                String destination = accessor.getDestination();
                Matcher gameTopic = destination == null ? null : GAME_TOPIC.matcher(destination);
                if (gameTopic != null && gameTopic.matches()) {
                    gameService.assertPlayer(gameTopic.group(1), Long.valueOf(principal.getName()));
                }
            }
        }

        return message;
    }
}