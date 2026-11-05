package com.transcendence.chess.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

//Clase de configuración de la comunicación WebSocket/STOMP.
@Configuration
//Intermedieario.
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    //Interceptor que verifica autenticación y permisos de los mensajes entrantes.
    private final StompAuthInterceptor authInterceptor;

    //Spring inyecta el interceptor de autenticación.
    public WebSocketConfig(StompAuthInterceptor authInterceptor) {
        //Guarda el interceptor para registrarlo en el canal de entrada.
        this.authInterceptor = authInterceptor;
    }

    //Configura los prefijos de destinos gestionados por el broker y la aplicación.
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        //Canales de salida a los que los clientes pueden suscribirse.
        config.enableSimpleBroker("/topic", "/queue"); // Canales de salida hacia los clientes
        //Prefijo que identifica los mensajes entrantes dirigidos a métodos de la aplicación.
        config.setApplicationDestinationPrefixes("/app"); // Prefijo para mensajes entrantes
    }

    //Configura el canal por el que entran los mensajes de los clientes.
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        //Ejecuta autenticación y control de acceso antes de procesar cada mensaje.
        registration.interceptors(authInterceptor);
    }

    //Registra la URL de conexión WebSocket/STOMP.
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        //Publica /ws y permite que el navegador use SockJS como alternativa compatible.
        registry.addEndpoint("/ws")
                //Permite conexiones desde cualquier patrón de origen.
                .setAllowedOriginPatterns("*")
                .withSockJS(); // Endpoint al que se conectará Angular
    }
}
