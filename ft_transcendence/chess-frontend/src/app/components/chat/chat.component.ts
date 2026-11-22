// Component define una pieza de interfaz; OnInit y OnDestroy marcan cuándo se crea y se elimina.
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
// FormsModule permite enlazar el campo de texto con una propiedad del componente.
import { FormsModule } from '@angular/forms';
// Servicio que intercambia mensajes en tiempo real con el backend.
import { ChessWebsocketService } from '../../services/chess-websocket';

@Component({
  // Nombre de la etiqueta Angular que representa este componente.
  selector: 'app-chat',
  // Módulos y componentes que usa esta plantilla.
  imports: [FormsModule],
  // Archivos separados para el contenido HTML y los estilos.
  templateUrl: './chat.component.html',
  styleUrls: ['./chat.component.css']
})
export class ChatComponent implements OnInit, OnDestroy {
  // Reutiliza el servicio compartido que mantiene la conexión en tiempo real.
  private wsService = inject(ChessWebsocketService);

  // Lista reactiva: al añadir mensajes, Angular actualiza la pantalla.
  messages = signal<{ sender: string, content: string }[]>([]);
  // Texto que el usuario está escribiendo en el formulario.
  messageInput = '';

  // Angular llama este método al mostrar el componente.
  ngOnInit(): void {
    // Se suscribe al chat y añade cada mensaje recibido a la lista visible.
    this.wsService.connectChat((message) => {
      this.messages.update((current) => [...current, message]);
    });
  }

  // Angular llama este método al quitar el componente de la pantalla.
  ngOnDestroy(): void {
    // Libera la conexión y suscripciones para no dejar procesos activos.
    this.wsService.disconnect();
  }

  // Envía el texto escrito y vacía el campo si tiene contenido.
  sendMessage() {
    // Elimina espacios al principio/final para detectar mensajes vacíos.
    const content = this.messageInput.trim();
    if (!content) {
      return;
    }
    // Envía el mensaje al backend mediante WebSocket.
    this.wsService.sendChat({ content });
    // Deja el campo listo para escribir el siguiente mensaje.
    this.messageInput = '';
  }
}
