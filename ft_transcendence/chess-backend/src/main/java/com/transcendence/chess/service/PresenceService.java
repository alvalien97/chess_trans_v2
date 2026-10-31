package com.transcendence.chess.service;

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

@Service
public class PresenceService {
    private final ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider;
    private final ConcurrentMap<String, Long> userBySession = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> tokenBySession = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Set<String>> sessionsByUser = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> sessionsByToken = new ConcurrentHashMap<>();

    public PresenceService(ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider) {
        this.messagingTemplateProvider = messagingTemplateProvider;
    }

    @EventListener
    public void onSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        String sessionId = accessor.getSessionId();
        if (principal == null || sessionId == null) {
            return;
        }
        Object token = accessor.getSessionAttributes() == null
            ? null
            : accessor.getSessionAttributes().get("authToken");
        registerSession(sessionId, Long.valueOf(principal.getName()), token instanceof String value ? value : null);
    }

    @EventListener
    public void onSessionDisconnected(SessionDisconnectEvent event) {
        unregisterSession(event.getSessionId());
    }

    public synchronized boolean isOnline(Long userId) {
        Set<String> sessions = sessionsByUser.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    public void registerSession(String sessionId, Long userId) {
        registerSession(sessionId, userId, null);
    }

    public synchronized void registerSession(String sessionId, Long userId, String token) {
        if (userBySession.putIfAbsent(sessionId, userId) != null) {
            return;
        }
        if (token != null) {
            tokenBySession.put(sessionId, token);
            sessionsByToken.computeIfAbsent(token, ignored -> ConcurrentHashMap.newKeySet()).add(sessionId);
        }

        AtomicBoolean becameOnline = new AtomicBoolean();
        sessionsByUser.compute(userId, (ignored, sessions) -> {
            Set<String> activeSessions = sessions == null ? ConcurrentHashMap.newKeySet() : sessions;
            becameOnline.set(activeSessions.isEmpty());
            activeSessions.add(sessionId);
            return activeSessions;
        });
        if (becameOnline.get()) {
            publish(userId, true);
        }
    }

    public synchronized void unregisterSession(String sessionId) {
        Long userId = userBySession.remove(sessionId);
        if (userId == null) {
            return;
        }
        removeSessionToken(sessionId);

        AtomicBoolean becameOffline = new AtomicBoolean();
        sessionsByUser.computeIfPresent(userId, (ignored, sessions) -> {
            sessions.remove(sessionId);
            becameOffline.set(sessions.isEmpty());
            return sessions.isEmpty() ? null : sessions;
        });
        if (becameOffline.get()) {
            publish(userId, false);
        }
    }

    public synchronized void forceOffline(Long userId) {
        Set<String> removedSessions = sessionsByUser.remove(userId);
        if (removedSessions == null || removedSessions.isEmpty()) {
            return;
        }
        removedSessions.forEach(sessionId -> {
            userBySession.remove(sessionId);
            removeSessionToken(sessionId);
        });
        publish(userId, false);
    }

    public synchronized void revokeToken(String token) {
        Set<String> sessionIds = sessionsByToken.get(token);
        if (sessionIds == null) {
            return;
        }
        for (String sessionId : new ArrayList<>(sessionIds)) {
            unregisterSession(sessionId);
        }
    }

    private void removeSessionToken(String sessionId) {
        String token = tokenBySession.remove(sessionId);
        if (token != null) {
            sessionsByToken.computeIfPresent(token, (ignored, sessions) -> {
                sessions.remove(sessionId);
                return sessions.isEmpty() ? null : sessions;
            });
        }
    }

    private void publish(Long userId, boolean online) {
        SimpMessagingTemplate messagingTemplate = messagingTemplateProvider.getIfAvailable();
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend("/topic/presence", new PresenceUpdate(userId, online));
        }
    }
}