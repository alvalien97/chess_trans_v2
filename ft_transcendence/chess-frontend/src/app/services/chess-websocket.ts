import { inject, Injectable } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';//importo para establecer conexion bidireccional
import { AuthService } from './auth';

@Injectable({
  providedIn: 'root'//servicio global
})
export class ChessWebsocketService {
  private readonly auth = inject(AuthService);
  private stompClient?: Client;
  private subscriptions: StompSubscription[] = [];//canales de comunic.
  private chatHandler?: (message: any) => void;//chat
  private readonly gameHandlers = new Map<string, (gameState: any) => void>();//asicio cada partida con su manejo de errores
  private readonly errorHandlers = new Map<string, (message: string) => void>();//mapa de rrores

  connectChat(onChat: (message: any) => void) {
    if (typeof window === 'undefined') {
      return;
    }
    this.chatHandler = onChat;
    this.ensureConnected();
    if (this.stompClient?.connected) {
      this.subscribeChat();
    }
  }

  joinGame(gameId: string, onGameState: (gameState: any) => void, onError: (message: string) => void) {
    this.gameHandlers.set(gameId, onGameState);
    this.errorHandlers.set(gameId, onError);
    this.ensureConnected();
    if (this.stompClient?.connected) {
      this.subscribeGame(gameId);
    };
  }

  leaveGame(gameId: string) {
    const subscriptionIds = new Set([`game-${gameId}`, `game-error-${gameId}`]);
    this.subscriptions = this.subscriptions.filter((subscription) => {
      if (!subscriptionIds.has(subscription.id)) {
        return true;
      }
      subscription.unsubscribe();
      return false;
    });
    this.gameHandlers.delete(gameId);
    this.errorHandlers.delete(gameId);
  }

  private ensureConnected() {
    if (this.stompClient) {
      return;
    }

    const authorization = this.auth.authorizationHeader;
    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: authorization ? { Authorization: authorization } : {},
      reconnectDelay: 5000,
      debug: () => {},
      onConnect: () => {
        this.subscribeChat();
        this.gameHandlers.forEach((_, gameId) => this.subscribeGame(gameId));
      },
      onStompError: (frame) => {
        const message = frame.headers['message'] ?? 'Error en la conexión en tiempo real.';
        this.errorHandlers.forEach((handler) => handler(message));
      },
      onWebSocketClose: () => {
        this.subscriptions = [];
      }
    });
    this.stompClient.activate();
  }

  private subscribeChat() {
    if (!this.stompClient?.connected || !this.chatHandler
        || this.subscriptions.some((subscription) => subscription.id === 'chat')) {
      return;
    }
    this.subscriptions.push(this.stompClient.subscribe('/topic/chat', (message: IMessage) => {
      this.chatHandler?.(JSON.parse(message.body));
    }, { id: 'chat' }));
  }

  private subscribeGame(gameId: string) {
    if (!this.stompClient?.connected
        || this.subscriptions.some((subscription) => subscription.id === `game-${gameId}`)) {
      return;
    }
    this.subscriptions.push(this.stompClient.subscribe(`/topic/game/${gameId}`, (message: IMessage) => {
      this.gameHandlers.get(gameId)?.(JSON.parse(message.body));
    }, { id: `game-${gameId}` }));
    this.subscriptions.push(this.stompClient.subscribe('/user/queue/errors', (message: IMessage) => {
      this.errorHandlers.get(gameId)?.(message.body);
    }, { id: `game-error-${gameId}` }));
  }

  sendMove(gameId: string, move: { fromCol: number, fromRow: number, toCol: number, toRow: number }): boolean {
    return this.publish(`/app/chess/move/${gameId}`, move);
  }

  resign(gameId: string): boolean {
    return this.publish(`/app/chess/resign/${gameId}`, {});
  }

  offerDraw(gameId: string): boolean {
    return this.publish(`/app/chess/draw-offer/${gameId}`, {});
  }

  respondToDraw(gameId: string, accepted: boolean): boolean {
    return this.publish(`/app/chess/draw-response/${gameId}`, { accepted });
  }

  sendChat(message: { content: string }) {
    if (this.stompClient?.connected) {
      this.stompClient.publish({
        destination: '/app/chat.send',
        body: JSON.stringify(message)
      });
    }
  }

  private publish(destination: string, body: unknown): boolean {
    if (!this.stompClient?.connected) {
      return false;
    }
    this.stompClient.publish({ destination, body: JSON.stringify(body) });
    return true;
  }

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
