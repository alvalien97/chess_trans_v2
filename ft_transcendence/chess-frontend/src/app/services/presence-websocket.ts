// Angular crea este servicio para compartir el canal de presencia entre pantallas.
import { Injectable, inject } from '@angular/core';
// Client mantiene la conexión y IMessage representa cada aviso recibido.
import { Client, IMessage } from '@stomp/stompjs';
// SockJS conecta con el endpoint de tiempo real del backend.
import SockJS from 'sockjs-client';
// El token de autenticación se reutiliza para identificar al jugador.
import { AuthService } from './auth';

// Aviso que indica si una cuenta se ha conectado o desconectado.
export interface PresenceUpdate {
  userId: number; // Cuenta a la que corresponde el cambio.
  online: boolean; // Nuevo estado de conexión.
}

@Injectable({ providedIn: 'root' })
export class PresenceWebsocketService {
  // Acceso al servicio que guarda la sesión actual.
  private readonly auth = inject(AuthService);
  // Cliente de la conexión; todavía no existe antes de conectar.
  private client?: Client;
  // Función de la pantalla que quiere enterarse de cambios de presencia.
  private updateHandler?: (update: PresenceUpdate) => void;

  // Abre el canal de presencia y entrega cada cambio a la función recibida.
  connect(onUpdate: (update: PresenceUpdate) => void) {
    // Los WebSockets solo existen en el navegador, no durante el renderizado del servidor.
    if (typeof window === 'undefined') return;
    // Conserva el receptor de eventos incluso si la conexión tarda en establecerse.
    this.updateHandler = onUpdate;
    // No crea una segunda conexión si ya hay una.
    if (this.client) return;

    // Incluye el token para que el backend pueda asociar la conexión a una cuenta.
    const authorization = this.auth.authorizationHeader;
    this.client = new Client({
      // Abre el WebSocket publicado por el backend en /ws.
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: authorization ? { Authorization: authorization } : {},
      // Reintenta la conexión tres segundos después de una desconexión.
      reconnectDelay: 3000,
      // Evita imprimir información de depuración de STOMP en la consola.
      debug: () => {},
      onConnect: () => {
        // Al conectarse, escucha los avisos globales de presencia.
        this.client?.subscribe('/topic/presence', (message: IMessage) => {
          // Convierte el texto JSON recibido en datos que la aplicación pueda usar.
          this.updateHandler?.(JSON.parse(message.body) as PresenceUpdate);
        });
      },
      // El manejador evita mostrar diagnósticos de protocolo directamente en la consola.
      onStompError: () => {}
    });
    // Inicia la conexión; onConnect se ejecutará cuando esté lista.
    this.client.activate();
  }

  // Cierra la conexión y elimina las referencias a los eventos anteriores.
  disconnect() {
    // Guarda el cliente antes de borrar la referencia local.
    const client = this.client;
    this.client = undefined;
    this.updateHandler = undefined;
    // deactivate cierra el socket; void indica que no se espera el resultado aquí.
    void client?.deactivate();
  }
}