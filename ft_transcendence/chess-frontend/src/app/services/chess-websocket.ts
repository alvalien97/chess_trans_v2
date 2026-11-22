// inject conecta este servicio con Angular; Injectable permite reutilizarlo desde componentes.
import { inject, Injectable } from '@angular/core';
// Client abre la conexión STOMP; IMessage representa mensajes recibidos.
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
// SockJS ofrece una conexión en tiempo real compatible con el servidor.
import SockJS from 'sockjs-client';
// Se reutiliza el servicio de autenticación para enviar el token al conectar.
import { AuthService } from './auth';

@Injectable({
  providedIn: 'root' // Angular comparte la conexión y suscripciones en toda la aplicación.
})
export class ChessWebsocketService {
  // Obtiene el servicio de autenticación que Angular ya conoce.
  private readonly auth = inject(AuthService);
  // Cliente que administra la conexión en tiempo real con el backend.
  private stompClient?: Client;
  // Referencias a los canales abiertos para poder cerrarlos después.
  private subscriptions: StompSubscription[] = [];
  // Función del componente que recibirá los mensajes del chat.
  private chatHandler?: (message: any) => void;
  // Relaciona el id de cada partida con la función que procesa sus actualizaciones.
  private readonly gameHandlers = new Map<string, (gameState: any) => void>();
  // Relaciona cada partida con la función que muestra sus errores.
  private readonly errorHandlers = new Map<string, (message: string) => void>();

  // Prepara el chat para recibir mensajes y establece la conexión si hace falta.
  connectChat(onChat: (message: any) => void) {
    // El servidor no dispone de WebSocket del navegador.
    if (typeof window === 'undefined') {
      return;
    }
    // Conserva la función que debe ejecutarse al llegar un mensaje.
    this.chatHandler = onChat;
    // Abre la conexión solo si todavía no existe.
    this.ensureConnected();
    // Si la conexión ya está lista, abre el canal del chat inmediatamente.
    if (this.stompClient?.connected) {
      this.subscribeChat();
    }
  }

  // Registra los manejadores de una partida y se suscribe a sus actualizaciones y errores.
  joinGame(gameId: string, onGameState: (gameState: any) => void, onError: (message: string) => void) {
    // Guarda qué hacer al recibir un cambio de tablero o un error para esta partida.
    this.gameHandlers.set(gameId, onGameState);
    this.errorHandlers.set(gameId, onError);
    // Asegura que exista una conexión antes de intentar suscribirse.
    this.ensureConnected();
    // Si ya se había conectado, no hace falta esperar al callback de conexión.
    if (this.stompClient?.connected) {
      this.subscribeGame(gameId);
    };
  }

  // Deja de escuchar una partida y olvida las funciones asociadas a ella.
  leaveGame(gameId: string) {
    // Los ids permiten localizar tanto el canal del juego como su canal de errores.
    const subscriptionIds = new Set([`game-${gameId}`, `game-error-${gameId}`]);
    // Mantiene las demás suscripciones y cierra únicamente las de esta partida.
    this.subscriptions = this.subscriptions.filter((subscription) => {
      if (!subscriptionIds.has(subscription.id)) {
        return true;
      }
      subscription.unsubscribe();
      return false;
    });
    // Libera las funciones guardadas para que no se usen tras salir de la partida.
    this.gameHandlers.delete(gameId);
    this.errorHandlers.delete(gameId);
  }

  private ensureConnected() {
    // Evita abrir varias conexiones simultáneas.
    if (this.stompClient) {
      return;
    }

    // Si hay sesión, el token se envía al servidor al iniciar la conexión.
    const authorization = this.auth.authorizationHeader;
    this.stompClient = new Client({
      // Crea el transporte WebSocket en la ruta que publica el backend.
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: authorization ? { Authorization: authorization } : {},
      // Espera cinco segundos antes de volver a intentar tras una desconexión.
      reconnectDelay: 5000,
      // Desactiva los mensajes de diagnóstico de STOMP en la consola.
      debug: () => {},
      onConnect: () => {
        // Al conectar, vuelve a abrir los canales necesarios, incluso tras reconectar.
        this.subscribeChat();
        this.gameHandlers.forEach((_, gameId) => this.subscribeGame(gameId));
      },
      onStompError: (frame) => {
        // Usa el mensaje del servidor o uno genérico y lo comunica a las partidas activas.
        const message = frame.headers['message'] ?? 'Error en la conexión en tiempo real.';
        this.errorHandlers.forEach((handler) => handler(message));
      },
      onWebSocketClose: () => {
        // El socket cerró; al reconectar se crearán de nuevo las suscripciones.
        this.subscriptions = [];
      }
    });
    this.stompClient.activate();
  }

  private subscribeChat() {
    // No crea el canal si no hay conexión, destinatario o si ya está suscrito.
    if (!this.stompClient?.connected || !this.chatHandler
        || this.subscriptions.some((subscription) => subscription.id === 'chat')) {
      return;
    }
    // Escucha el canal público del chat y entrega el mensaje al componente.
    this.subscriptions.push(this.stompClient.subscribe('/topic/chat', (message: IMessage) => {
      this.chatHandler?.(JSON.parse(message.body));
    }, { id: 'chat' }));
  }

  private subscribeGame(gameId: string) {
    // Evita suscripciones repetidas y espera a que el socket esté conectado.
    if (!this.stompClient?.connected
        || this.subscriptions.some((subscription) => subscription.id === `game-${gameId}`)) {
      return;
    }
    // El canal de la partida distribuye los cambios compartidos del tablero.
    this.subscriptions.push(this.stompClient.subscribe(`/topic/game/${gameId}`, (message: IMessage) => {
      this.gameHandlers.get(gameId)?.(JSON.parse(message.body));
    }, { id: `game-${gameId}` }));
    // Este canal privado recibe errores dirigidos a la persona conectada.
    this.subscriptions.push(this.stompClient.subscribe('/user/queue/errors', (message: IMessage) => {
      this.errorHandlers.get(gameId)?.(message.body);
    }, { id: `game-error-${gameId}` }));
  }

  // Envía al backend las coordenadas de origen y destino de una pieza.
  sendMove(gameId: string, move: { fromCol: number, fromRow: number, toCol: number, toRow: number }): boolean {
    return this.publish(`/app/chess/move/${gameId}`, move);
  }

  // Informa al backend de que el jugador abandona la partida.
  resign(gameId: string): boolean {
    return this.publish(`/app/chess/resign/${gameId}`, {});
  }

  // Envía una oferta de tablas al rival.
  offerDraw(gameId: string): boolean {
    return this.publish(`/app/chess/draw-offer/${gameId}`, {});
  }

  // Envía si el jugador acepta o rechaza la oferta de tablas.
  respondToDraw(gameId: string, accepted: boolean): boolean {
    return this.publish(`/app/chess/draw-response/${gameId}`, { accepted });
  }

  // Publica un mensaje de chat solo si la conexión está activa.
  sendChat(message: { content: string }) {
    if (this.stompClient?.connected) {
      this.stompClient.publish({
        destination: '/app/chat.send',
        body: JSON.stringify(message)
      });
    }
  }

  private publish(destination: string, body: unknown): boolean {
    // Devuelve false para que la interfaz pueda avisar de que no había conexión.
    if (!this.stompClient?.connected) {
      return false;
    }
    this.stompClient.publish({ destination, body: JSON.stringify(body) });
    return true;
  }

  // Cierra los canales y la conexión y libera los manejadores registrados.
  disconnect() {
    this.subscriptions.forEach((subscription) => subscription.unsubscribe());
    this.subscriptions = [];
    this.stompClient?.deactivate();
    this.stompClient = undefined;
    this.chatHandler = undefined;
    this.gameHandlers.clear();
    this.errorHandlers.clear();
  }
}
