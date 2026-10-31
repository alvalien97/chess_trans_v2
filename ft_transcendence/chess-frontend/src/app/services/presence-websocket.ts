import { Injectable, inject } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { AuthService } from './auth';

export interface PresenceUpdate {
  userId: number;
  online: boolean;
}

@Injectable({ providedIn: 'root' })
export class PresenceWebsocketService {
  private readonly auth = inject(AuthService);
  private client?: Client;
  private updateHandler?: (update: PresenceUpdate) => void;

  connect(onUpdate: (update: PresenceUpdate) => void) {
    if (typeof window === 'undefined') return;
    this.updateHandler = onUpdate;
    if (this.client) return;

    const authorization = this.auth.authorizationHeader;
    this.client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: authorization ? { Authorization: authorization } : {},
      reconnectDelay: 3000,
      debug: () => {},
      onConnect: () => {
        this.client?.subscribe('/topic/presence', (message: IMessage) => {
          this.updateHandler?.(JSON.parse(message.body) as PresenceUpdate);
        });
      },
      onStompError: () => {}
    });
    this.client.activate();
  }

  disconnect() {
    const client = this.client;
    this.client = undefined;
    this.updateHandler = undefined;
    void client?.deactivate();
  }
}