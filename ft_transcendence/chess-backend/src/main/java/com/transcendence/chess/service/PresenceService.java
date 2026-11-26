//Paquete que contiene la lógica de negocio del backend.
package com.transcendence.chess.service;

//Identidad autenticada de una conexión WebSocket.
import java.security.Principal;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.transcendence.chess.dto.PresenceUpdate;

//Sigue las sesiones para determinar si cada usuario está conectado.
@Service
public class PresenceService {
    //Proveedor opcional del publicador STOMP.
    private final ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider;
    //Relaciona cada id de sesión con el usuario autenticado.
    private final ConcurrentMap<String, Long> userBySession = new ConcurrentHashMap<>();
    //Relaciona cada sesión con el token que la autenticó.
    private final ConcurrentMap<String, String> tokenBySession = new ConcurrentHashMap<>();
    //Relaciona cada usuario con todas sus sesiones WebSocket activas.
    private final ConcurrentMap<Long, Set<String>> sessionsByUser = new ConcurrentHashMap<>();
    //Relaciona cada token con las sesiones que se iniciaron usando ese token.
    private final ConcurrentMap<String, Set<String>> sessionsByToken = new ConcurrentHashMap<>();

    //Spring inyecta un proveedor para acceder al template de mensajería cuando esté disponible.
    public PresenceService(ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider) {
        //Conserva el proveedor para emitir cambios de estado.
        this.messagingTemplateProvider = messagingTemplateProvider;
    }

    //Atiende el evento de conexión completa de una sesión STOMP.
    @EventListener
    public void onSessionConnected(SessionConnectedEvent event) {
        //Extrae cabeceras STOMP del mensaje de conexión.
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        //Lee la identidad asignada por el interceptor de autenticación.
        Principal principal = accessor.getUser();
        //Lee el id único de esta sesión WebSocket.
        String sessionId = accessor.getSessionId();
        //No registra conexiones sin usuario autenticado o sin identificador.
        if (principal == null || sessionId == null) {
            return;
        }
        //Recupera el token guardado por el interceptor durante CONNECT, si hay atributos de sesión.
        Object token = null;
        if (accessor.getSessionAttributes() != null) {
            token = accessor.getSessionAttributes().get("authToken");
        }
        //Registra la sesión y solo trata el atributo como token si su tipo es String.
        String tokenValue = null;
        if (token instanceof String value) {
            tokenValue = value;
        }
        registerSession(sessionId, Long.valueOf(principal.getName()), tokenValue);
    }

    //Atiende el evento de desconexión de una sesión STOMP.
    @EventListener
    public void onSessionDisconnected(SessionDisconnectEvent event) {
        //Elimina la sesión y publica offline si era la última del usuario.
        unregisterSession(event.getSessionId());
    }

    //Indica si el usuario tiene alguna sesión WebSocket registrada.
    public synchronized boolean isOnline(Long userId) {
        //Consulta el conjunto de sesiones del usuario.
        Set<String> sessions = sessionsByUser.get(userId);
        //Está online si existe un conjunto no vacío.
        return sessions != null && !sessions.isEmpty();
    }

    //Registra una sesión sin asociarla a un token conocido.
    public void registerSession(String sessionId, Long userId) {
        //Delega en la variante que también puede guardar un token.
        registerSession(sessionId, userId, null);
    }

    //Registra una conexión y actualiza los índices inversos de usuario y token.
    public synchronized void registerSession(String sessionId, Long userId, String token) {
        //putIfAbsent impide procesar dos veces el mismo id de sesión.
        if (userBySession.putIfAbsent(sessionId, userId) != null) {
            return;
        }
        //Si hay token, recuerda la relación sesión-token y token-sesiones.
        if (token != null) {
            tokenBySession.put(sessionId, token);
            sessionsByToken.computeIfAbsent(token, ignored -> ConcurrentHashMap.newKeySet()).add(sessionId);
        }

        //Permite saber si esta conexión hizo pasar al usuario de offline a online.
        AtomicBoolean becameOnline = new AtomicBoolean();
        //Añade la sesión al conjunto del usuario, creándolo si es la primera.
        sessionsByUser.compute(userId, (ignored, sessions) -> {
            //Reutiliza el conjunto existente o crea uno seguro para concurrencia.
            Set<String> activeSessions = sessions;
            if (activeSessions == null) {
                activeSessions = ConcurrentHashMap.newKeySet();
            }
            //Anota si el conjunto estaba vacío antes de añadir la conexión.
            becameOnline.set(activeSessions.isEmpty());
            //Registra la sesión activa.
            activeSessions.add(sessionId);
            return activeSessions;
        });
        //Publica online solo en la transición de cero sesiones a una.
        if (becameOnline.get()) {
            publish(userId, true);
        }
    }

    //Desregistra una conexión y publica offline si ya no quedan otras sesiones del usuario.
    public synchronized void unregisterSession(String sessionId) {
        //Quita la sesión del índice principal y obtiene el usuario asociado.
        Long userId = userBySession.remove(sessionId);
        //Una sesión no registrada no requiere ningún otro cambio.
        if (userId == null) {
            return;
        }
        //Elimina también la asociación de esta sesión con su token.
        removeSessionToken(sessionId);

        //Permite detectar si al usuario se le cerró su última conexión.
        AtomicBoolean becameOffline = new AtomicBoolean();
        //Quita la sesión del conjunto de conexiones del usuario.
        sessionsByUser.computeIfPresent(userId, (ignored, sessions) -> {
            sessions.remove(sessionId);
            becameOffline.set(sessions.isEmpty());
            if (sessions.isEmpty()) {
                return null;
            }
            return sessions;
        });
        //Publica offline solo cuando el conjunto de sesiones queda vacío.
        if (becameOffline.get()) {
            publish(userId, false);
        }
    }

    //Cierra todas las sesiones registradas de un usuario y fuerza su estado a offline.
    public synchronized void forceOffline(Long userId) {
        //Quita y obtiene todas las sesiones del usuario.
        Set<String> removedSessions = sessionsByUser.remove(userId);
        //Si no había sesiones, no hay nada que publicar.
        if (removedSessions == null || removedSessions.isEmpty()) {
            return;
        }
        //Limpia cada sesión de los índices usuario y token.
        removedSessions.forEach(sessionId -> {
            userBySession.remove(sessionId);
            removeSessionToken(sessionId);
        });
        //Publica el cambio global del usuario a offline.
        publish(userId, false);
    }

    //Revoca todas las conexiones que se autenticaron con un token concreto.
    public synchronized void revokeToken(String token) {
        //Busca las sesiones asociadas al token.
        Set<String> sessionIds = sessionsByToken.get(token);
        //Si ninguna sesión usa ese token, termina sin cambios.
        if (sessionIds == null) {
            return;
        }
        //Copia el conjunto para poder eliminar elementos durante unregisterSession.
        for (String sessionId : new ArrayList<>(sessionIds)) {
            //Reutiliza la limpieza estándar de sesiones.
            unregisterSession(sessionId);
        }
    }

    //Elimina una sesión de los índices asociados a tokens.
    private void removeSessionToken(String sessionId) {
        //Quita el token del índice sesión-token.
        String token = tokenBySession.remove(sessionId);
        //Si tenía token, actualiza el conjunto inverso.
        if (token != null) {
            //Elimina la sesión y elimina el conjunto si queda vacío.
            sessionsByToken.computeIfPresent(token, (ignored, sessions) -> {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    return null;
                }
                return sessions;
            });
        }
    }

    //Publica una actualización de presencia si el componente STOMP está disponible.
    private void publish(Long userId, boolean online) {
        //Obtiene el publicador o null si no hay configuración de mensajería.
        SimpMessagingTemplate messagingTemplate = messagingTemplateProvider.getIfAvailable();
        //Evita fallar si la aplicación no configuró el publicador.
        if (messagingTemplate != null) {
            //Envía el evento al canal al que pueden suscribirse los clientes.
            messagingTemplate.convertAndSend("/topic/presence", new PresenceUpdate(userId, online));
        }
    }
}