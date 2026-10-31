import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ChessWebsocketService } from '../../services/chess-websocket';

@Component({
  selector: 'app-chat',
  imports: [FormsModule],
  templateUrl: './chat.component.html',
  styleUrls: ['./chat.component.css']
})
export class ChatComponent implements OnInit, OnDestroy {
  private wsService = inject(ChessWebsocketService);

  messages = signal<{ sender: string, content: string }[]>([]);
  messageInput = '';

  ngOnInit(): void {
    this.wsService.connectChat((message) => {
      this.messages.update((current) => [...current, message]);
    });
  }

  ngOnDestroy(): void {
    this.wsService.disconnect();
  }

  sendMessage() {
    const content = this.messageInput.trim();
    if (!content) {
      return;
    }
    this.wsService.sendChat({ content });
    this.messageInput = '';
  }
}
