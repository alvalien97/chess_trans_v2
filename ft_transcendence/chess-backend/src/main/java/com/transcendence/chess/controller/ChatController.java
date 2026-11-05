// Agrupa el manejador de mensajes del chat en tiempo real.
package com.transcendence.chess.controller;

// Identidad autenticada asociada a la conexión WebSocket.
import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import com.transcendence.chess.model.ChatMessage;
import com.transcendence.chess.model.User;
import com.transcendence.chess.repository.UserRepository;

// Controlador de mensajes WebSocket, no de endpoints REST tradicionales.
@Controller
public class ChatController {

    // Dependencia que permite consultar a los usuarios registrados.
    private final UserRepository userRepository;

    // Constructor de inyección de dependencias.
    public ChatController(UserRepository userRepository) {
        // Guarda el repositorio para utilizarlo al procesar mensajes.
        this.userRepository = userRepository;
    }

    // Angular enviará mensajes a: /app/chat.send
    // Escucha mensajes que el cliente publica en el destino /app/chat.send.
    @MessageMapping("/chat.send")
    @SendTo("/topic/chat") // Reenvía el mensaje a todos los conectados a /topic/chat
    // Recibe el contenido y la identidad autenticada de quien lo envía.
    public ChatMessage sendMessage(ChatMessage chatMessage, Principal principal) {
        // Sustituye el contenido nulo por texto vacío y elimina espacios al principio y al final.
        String content = chatMessage.getContent() == null ? "" : chatMessage.getContent().trim();
        // Rechaza mensajes vacíos o que superen el máximo de 280 caracteres.
        if (content.isEmpty() || content.length() > 280) {
            // Informa del límite aceptado para los mensajes del chat.
            throw new IllegalArgumentException("El mensaje debe tener entre 1 y 280 caracteres.");
        }
        // Busca al usuario cuyo id se obtiene de la identidad autenticada de la conexión.
        User sender = userRepository.findById(Long.valueOf(principal.getName()))
                // Si la identidad no corresponde a un usuario, no se puede atribuir el mensaje.
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        // Usa el nombre almacenado en el servidor como remitente, en vez de confiar en el cliente.
        chatMessage.setSender(sender.getUsername());
        // Guarda el contenido ya normalizado y validado.
        chatMessage.setContent(content);
        // Spring publica este mensaje de retorno en /topic/chat.
        return chatMessage;
    }
}
