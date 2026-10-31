package com.transcendence.chess.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import com.transcendence.chess.model.ChatMessage;
import com.transcendence.chess.model.User;
import com.transcendence.chess.repository.UserRepository;

@Controller
public class ChatController {

    private final UserRepository userRepository;

    public ChatController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Angular enviará mensajes a: /app/chat.send
    @MessageMapping("/chat.send")
    @SendTo("/topic/chat") // Reenvía el mensaje a todos los conectados a /topic/chat
    public ChatMessage sendMessage(ChatMessage chatMessage, Principal principal) {
        String content = chatMessage.getContent() == null ? "" : chatMessage.getContent().trim();
        if (content.isEmpty() || content.length() > 280) {
            throw new IllegalArgumentException("El mensaje debe tener entre 1 y 280 caracteres.");
        }
        User sender = userRepository.findById(Long.valueOf(principal.getName()))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        chatMessage.setSender(sender.getUsername());
        chatMessage.setContent(content);
        return chatMessage;
    }
}
