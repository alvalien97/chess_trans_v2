package com.transcendence.chess.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.transcendence.chess.dto.PresenceUpdate;

class PresenceServiceTest {

    @Test
    void staysOnlineUntilTheLastConnectionDisconnects() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        PresenceService presenceService = presenceService(messagingTemplate);

        presenceService.registerSession("session-a", 12L);
        presenceService.registerSession("session-b", 12L);
        assertTrue(presenceService.isOnline(12L));

        presenceService.unregisterSession("session-a");
        assertTrue(presenceService.isOnline(12L));
        presenceService.unregisterSession("session-b");
        assertFalse(presenceService.isOnline(12L));

        verify(messagingTemplate).convertAndSend("/topic/presence", new PresenceUpdate(12L, true));
        verify(messagingTemplate).convertAndSend("/topic/presence", new PresenceUpdate(12L, false));
    }

    @Test
    void forceOfflineIsIdempotentAndIgnoresLaterSocketDisconnect() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        PresenceService presenceService = presenceService(messagingTemplate);
        presenceService.registerSession("session-a", 12L);

        presenceService.forceOffline(12L);
        presenceService.forceOffline(12L);
        presenceService.unregisterSession("session-a");

        assertFalse(presenceService.isOnline(12L));
        verify(messagingTemplate).convertAndSend("/topic/presence", new PresenceUpdate(12L, true));
        verify(messagingTemplate).convertAndSend("/topic/presence", new PresenceUpdate(12L, false));
    }

    @Test
    void revokingOneTokenKeepsOtherTokensForTheUserOnline() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        PresenceService presenceService = presenceService(messagingTemplate);
        presenceService.registerSession("session-a", 12L, "token-a");
        presenceService.registerSession("session-b", 12L, "token-b");

        presenceService.revokeToken("token-a");
        assertTrue(presenceService.isOnline(12L));
        presenceService.revokeToken("token-b");
        assertFalse(presenceService.isOnline(12L));

        verify(messagingTemplate).convertAndSend("/topic/presence", new PresenceUpdate(12L, true));
        verify(messagingTemplate).convertAndSend("/topic/presence", new PresenceUpdate(12L, false));
    }

    @SuppressWarnings("unchecked")
    private static PresenceService presenceService(SimpMessagingTemplate messagingTemplate) {
        ObjectProvider<SimpMessagingTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(messagingTemplate);
        return new PresenceService(provider);
    }
}