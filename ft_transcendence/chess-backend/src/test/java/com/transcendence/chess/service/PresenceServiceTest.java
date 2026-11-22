//El test comparte paquete con PresenceService.
package com.transcendence.chess.service;

//Aserciones booleanas para comprobar los estados de presencia.
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.transcendence.chess.dto.PresenceUpdate;

//Comprueba que sesiones WebSocket se reflejan correctamente como presencia.
class PresenceServiceTest {

    //Un usuario sigue online mientras le quede al menos una sesión conectada.
    @Test
    void staysOnlineUntilTheLastConnectionDisconnects() {
        //Simula el componente que publicaría eventos al canal STOMP.
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        //Construye el servicio con ese publicador simulado.
        PresenceService presenceService = presenceService(messagingTemplate);

        //Registra dos sesiones diferentes para el mismo usuario.
        presenceService.registerSession("session-a", 12L);
        presenceService.registerSession("session-b", 12L);
        //El usuario está online mientras ambas sesiones existan.
        assertTrue(presenceService.isOnline(12L));

        //Cerrar la primera sesión no debe desconectar al usuario.
        presenceService.unregisterSession("session-a");
        assertTrue(presenceService.isOnline(12L));
        //Al cerrar la última sesión, el usuario pasa a offline.
        presenceService.unregisterSession("session-b");
        assertFalse(presenceService.isOnline(12L));

        //Solo se publican las transiciones globales offline->online y online->offline.
        verify(messagingTemplate).convertAndSend("/topic/presence", new PresenceUpdate(12L, true));
        verify(messagingTemplate).convertAndSend("/topic/presence", new PresenceUpdate(12L, false));
    }

    //Forzar offline varias veces y recibir luego un disconnect no duplica el evento.
    @Test
    void forceOfflineIsIdempotentAndIgnoresLaterSocketDisconnect() {
        //Prepara el publicador y el servicio.
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        PresenceService presenceService = presenceService(messagingTemplate);
        //Registra una sesión que se cerrará manualmente.
        presenceService.registerSession("session-a", 12L);

        //Fuerza la desconexión dos veces y simula el evento socket tardío.
        presenceService.forceOffline(12L);
        presenceService.forceOffline(12L);
        presenceService.unregisterSession("session-a");

    //El estado final es offline y cada transición se publicó una sola vez.
        assertFalse(presenceService.isOnline(12L));
        verify(messagingTemplate).convertAndSend("/topic/presence", new PresenceUpdate(12L, true));
        verify(messagingTemplate).convertAndSend("/topic/presence", new PresenceUpdate(12L, false));
    }

    //Revocar un token cierra sus sesiones, pero no las sesiones de otros tokens del usuario.
    @Test
    void revokingOneTokenKeepsOtherTokensForTheUserOnline() {
        //Prepara el publicador y el servicio.
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        PresenceService presenceService = presenceService(messagingTemplate);
        //Crea dos sesiones del mismo usuario con tokens distintos.
        presenceService.registerSession("session-a", 12L, "token-a");
        presenceService.registerSession("session-b", 12L, "token-b");

    //Revocar solo token-a deja activa la sesión asociada a token-b.
        presenceService.revokeToken("token-a");
        assertTrue(presenceService.isOnline(12L));
    //Revocar el segundo token cierra la última sesión.
        presenceService.revokeToken("token-b");
        assertFalse(presenceService.isOnline(12L));

        verify(messagingTemplate).convertAndSend("/topic/presence", new PresenceUpdate(12L, true));
        verify(messagingTemplate).convertAndSend("/topic/presence", new PresenceUpdate(12L, false));
    }

    //Construye PresenceService con un ObjectProvider simulado para no iniciar Spring.
    @SuppressWarnings("unchecked")
    private static PresenceService presenceService(SimpMessagingTemplate messagingTemplate) {
        //Simula el proveedor del bean de mensajería.
        ObjectProvider<SimpMessagingTemplate> provider = mock(ObjectProvider.class);
        //Configura el proveedor para devolver el mock de template.
        when(provider.getIfAvailable()).thenReturn(messagingTemplate);
        //Devuelve el servicio conectado al proveedor simulado.
        return new PresenceService(provider);
    }
}
/*
asserttrue coprueba si un usuario esta online o no, mock when y verify crean objetos falsos, los test son pruebas 
de JUnit y se pueden eliminar 
 */