// Herramientas de Angular para crear el componente, intercambiar datos y guardar estado reactivo.
import {
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  QueryList,
  ViewChildren,
  inject,
  signal
} from '@angular/core';
// HttpClient consulta el backend y HttpHeaders añade autenticación a la petición.
import { HttpClient, HttpHeaders } from '@angular/common/http';
// Subscription permite cancelar consultas periódicas; interval crea esas consultas repetidas.
import { Subscription, interval } from 'rxjs';
// Operadores para lanzar una consulta inmediata, repetirla y detenerla según su resultado.
import { startWith, switchMap, takeWhile } from 'rxjs/operators';
// Servicios que gestionan el WebSocket de ajedrez y los datos de sesión.
import { ChessWebsocketService } from '../../services/chess-websocket';
import { AuthService } from '../../services/auth';

// Datos que otro componente necesita para abrir una partida concreta.
export interface MatchAssignment {
  gameId: string; // Identificador de la partida en el backend.
  color: number; // Color asignado al jugador.
  opponent: string; // Nombre visible del rival.
}

// Respuesta del backend al consultar la búsqueda de rival.
interface MatchmakingStatus {
  status: 'IDLE' | 'WAITING' | 'MATCHED'; // Estado de la búsqueda.
  gameId: string | null; // Partida asignada cuando se encuentra rival.
  color: number | null; // Color asignado en esa partida.
  opponent: string | null; // Nombre del rival, si ya se encontró.
}

// Parte del estado de partida que la interfaz necesita para dibujar el tablero.
interface SharedGameState {
  pieces?: any[]; // Piezas con sus posiciones recibidas del servidor.
  currentColor?: number; // Color al que le toca mover.
  status?: string; // Estado de la partida: en curso, jaque mate, tablas, etc.
  drawOfferByUserId?: number | null; // Id de quien ha ofrecido tablas.
  winnerId?: number | null; // Id del ganador, si ya se decidió.
  resultMessage?: string | null; // Texto opcional con el resultado.
}

@Component({
  // Etiqueta con la que se inserta este tablero en otra plantilla Angular.
  selector: 'app-board',
  // Plantilla y estilos propios del tablero.
  templateUrl: './board.component.html',
  styleUrls: ['./board.component.css']
})
export class BoardComponent implements OnInit, OnDestroy {
  // Servicios necesarios para intercambiar datos, conectar el juego y consultar la sesión.
  private chessWsService = inject(ChessWebsocketService);
  private http = inject(HttpClient);
  private auth = inject(AuthService);
  // Suscripción a la búsqueda periódica de rival; se cancela al terminar.
  private polling?: Subscription;
  // Referencia a todos los botones de casilla para mover el foco con el teclado.
  @ViewChildren('squareButton', { read: ElementRef })
  private squareButtons!: QueryList<ElementRef<HTMLButtonElement>>;

  // Estado reactivo que la plantilla usa para mostrar partida y emparejamiento.
  gameId = signal<string | null>(null);
  queueState = signal<'IDLE' | 'WAITING' | 'MATCHED' | 'FINISHED'>('IDLE');
  playerColor = signal<number | null>(null);
  opponent = signal('');
  pieces = signal<any[]>(this.createInitialPieces());
  
  // Crea la posición inicial de ajedrez para mostrar el tablero antes de recibir datos.
  private createInitialPieces() {
    // Aquí se reunirán las 32 piezas con su color, columna, fila y tipo.
    const pieces: any[] = [];
    // Orden habitual de las piezas de la primera fila de cada lado.
    const backRank = ['ROOK', 'KNIGHT', 'BISHOP', 'QUEEN', 'KING', 'BISHOP', 'KNIGHT', 'ROOK'];
    
    // Recorre las ocho columnas y coloca una pieza mayor y un peón por columna.
    for (let col = 0; col < 8; col++) {
      // Coloca las piezas negras en las dos filas superiores.
      pieces.push({ color: 1, col, row: 0, type: backRank[col] });
      pieces.push({ color: 1, col, row: 1, type: 'PAWN' });
      // Coloca los peones y las piezas blancas en las dos filas inferiores.
      pieces.push({ color: 0, col, row: 6, type: 'PAWN' });
      pieces.push({ color: 0, col, row: 7, type: backRank[col] });
    }
    
    // Devuelve todas las piezas para inicializar el signal del tablero.
    return pieces;
  }
  // Pieza seleccionada al pulsar una casilla.
  selectedPiece = signal<any>(null);
  // Color que debe realizar el siguiente movimiento.
  currentColor = signal(0);
  // Texto de error que se presenta al jugador.
  errorMessage = signal('');
  // Estado textual de la partida, como jaque o tablas.
  gameStatus = signal('');
  // Identificador de quien ha enviado una oferta de tablas, o null si no hay oferta.
  drawOfferByUserId = signal<number | null>(null);
  // Mensaje final que se muestra al terminar la partida.
  resultMessage = signal('');
  // Identificador de la persona conectada, usado para reconocer resultados propios.
  readonly currentUserId = this.auth.user()?.id ?? null;
  // Casilla que tiene el foco del teclado en este momento.
  focusedSquare = signal({ col: 0, row: 0 });

  // Eventos que este componente envía al componente que contiene el tablero.
  @Output() matchSelected = new EventEmitter<MatchAssignment>();
  @Output() closeGame = new EventEmitter<string>();

  // Activa el modo de búsqueda delegada, donde el componente padre abrirá la partida.
  @Input() queueEnabled = false;

  // Recibe una partida desde el componente padre y la activa si es una nueva.
  @Input() set match(assignment: MatchAssignment | null) {
    if (assignment && assignment.gameId !== this.gameId()) {
      this.activateMatch(assignment);
    }
  }

  // Números de fila y columna que la plantilla recorre para dibujar las 64 casillas.
  boardRows = [0, 1, 2, 3, 4, 5, 6, 7];
  boardCols = [0, 1, 2, 3, 4, 5, 6, 7];

  // Invierte filas y columnas para que negras vean sus piezas en la parte inferior.
  get displayRows(): number[] {
    return this.getDisplayOrder(this.boardRows);
  }

  get displayCols(): number[] {
    return this.getDisplayOrder(this.boardCols);
  }

  get boardAriaLabel(): string {
    if (this.playerColor() === 1) {
      return 'Tablero de ajedrez desde la perspectiva de negras';
    }
    return 'Tablero de ajedrez desde la perspectiva de blancas';
  }

  private getDisplayOrder(coordinates: number[]): number[] {
    const displayOrder: number[] = [];

    if (this.playerColor() === 1) {
      for (let index = coordinates.length - 1; index >= 0; index--) {
        displayOrder.push(coordinates[index]);
      }
      return displayOrder;
    }

    for (let index = 0; index < coordinates.length; index++) {
      displayOrder.push(coordinates[index]);
    }
    return displayOrder;
  }

  // Traduce coordenadas del juego al índice de la casilla en el orden visible del DOM.
  private getSquareIndex(col: number, row: number): number {
    let displayRow = -1;
    let displayCol = -1;

    for (let index = 0; index < this.displayRows.length; index++) {
      if (this.displayRows[index] === row) {
        displayRow = index;
        break;
      }
    }

    for (let index = 0; index < this.displayCols.length; index++) {
      if (this.displayCols[index] === col) {
        displayCol = index;
        break;
      }
    }

    return displayRow * 8 + displayCol;
  }

  // Angular llama este método al mostrar el tablero.
  ngOnInit(): void {
    this.gameStatus.set('');
  }

  // Solicita entrar en la cola de matchmaking para buscar un rival.
  findOpponent() {
    // Sin token no se puede hacer una búsqueda asociada a una cuenta.
    const headers = this.authHeaders();
    if (!headers) {
      this.errorMessage.set('Inicia sesión para buscar una partida.');
      return;
    }
    // Limpia errores anteriores y envía la solicitud de búsqueda.
    this.errorMessage.set('');
    this.http.post<MatchmakingStatus>('/api/matchmaking/queue', {}, { headers }).subscribe({
      next: (status) => {
        // Actualiza la pantalla con el estado que devolvió el backend.
        this.handleMatchmakingStatus(status);
        // Si todavía no hay rival, consulta periódicamente hasta encontrarlo.
        if (status.status === 'WAITING') {
          this.pollForMatch(headers);
        }
      },
      error: (error) => this.errorMessage.set(error.error?.detail ?? 'No se pudo iniciar la búsqueda.')
    });
  }

  // Avisa al componente padre que puede quitar de pantalla una partida terminada.
  closeFinishedGame() {
    const gameId = this.gameId();
    if (gameId && this.queueState() === 'FINISHED') {
      this.closeGame.emit(gameId);
    }
  }

  // Confirma la rendición y la envía al backend por el canal en tiempo real.
  resign() {
    const gameId = this.gameId();
    if (!gameId || !window.confirm('¿Rendirte? Tu rival ganará esta partida.')) return;
    if (!this.chessWsService.resign(gameId)) {
      this.errorMessage.set('Sin conexión con la partida. No se pudo enviar la rendición.');
    }
  }

  // Envía al rival una oferta de tablas.
  offerDraw() {
    const gameId = this.gameId();
    if (gameId && !this.chessWsService.offerDraw(gameId)) {
      this.errorMessage.set('Sin conexión con la partida. No se pudo enviar la oferta.');
    }
  }

  // Responde a la oferta de tablas con aceptación o rechazo.
  respondToDraw(accepted: boolean) {
    const gameId = this.gameId();
    if (gameId && !this.chessWsService.respondToDraw(gameId, accepted)) {
      this.errorMessage.set('Sin conexión con la partida. No se pudo enviar la respuesta.');
    }
  }

  // Detiene la consulta de rival y solicita salir de la cola al backend.
  cancelSearch() {
    this.polling?.unsubscribe();
    this.queueState.set('IDLE');
    const headers = this.authHeaders();
    if (headers) {
      this.http.delete('/api/matchmaking/queue', { headers }).subscribe();
    }
  }

  // Gestiona la selección de casillas y envía un movimiento cuando corresponde.
  onSquareClick(col: number, row: number) {
    // Mantiene actualizado el foco para que el teclado siga al ratón.
    this.focusedSquare.set({ col, row });
    // Solo se puede mover durante una partida y cuando es el turno del jugador.
    if (this.queueState() !== 'MATCHED' || this.currentColor() !== this.playerColor()) {
      return;
    }

    // Averigua qué pieza ocupa la casilla pulsada y qué pieza estaba seleccionada.
    const clickedPiece = this.getPieceAt(col, row);
    const selected = this.selectedPiece();

    if (clickedPiece?.color === this.playerColor()) {
      // Pulsar una pieza propia la selecciona como origen del siguiente movimiento.
      this.selectedPiece.set(clickedPiece);
      return;
    }

    if (selected) {
      // Si ya había una pieza seleccionada, la casilla pulsada se convierte en destino.
      const gameId = this.gameId();
      if (!gameId) {
        return;
      }
      // Envía origen y destino al backend; el backend valida las reglas del ajedrez.
      const sent = this.chessWsService.sendMove(gameId, {
        fromCol: selected.col,
        fromRow: selected.row,
        toCol: col,
        toRow: row
      });
      if (!sent) {
        // Informa si no se pudo enviar el movimiento por falta de conexión.
        this.errorMessage.set('Sin conexión con la partida. El movimiento no se envió; inténtalo de nuevo.');
      }
    } else if (clickedPiece) {
      // Permite seleccionar una pieza si todavía no había origen seleccionado.
      this.selectedPiece.set(clickedPiece);
    }
  }

  // Busca la pieza situada en la columna y fila recibidas.
  getPieceAt(col: number, row: number) {
    return this.pieces().find(p => p.col === col && p.row === row);
  }

  // Convierte el tipo y color interno de una pieza en su símbolo de ajedrez.
  getPieceSymbol(piece: any): string {
    // Símbolos Unicode para las piezas blancas.
    const white: Record<string, string> = {
      KING: '♔', QUEEN: '♕', ROOK: '♖', BISHOP: '♗', KNIGHT: '♘', PAWN: '♙'
    };
    // Símbolos Unicode para las piezas negras.
    const black: Record<string, string> = {
      KING: '♚', QUEEN: '♛', ROOK: '♜', BISHOP: '♝', KNIGHT: '♞', PAWN: '♟'
    };
    // Elige la tabla correcta según el código de color del modelo.
    let table = white;
    if (piece.color !== 0) {
      table = black;
    }
    // Devuelve el símbolo correspondiente o ? si llega un tipo desconocido.
    return table[piece.type] ?? '?';
  }

  // En un tablero de ajedrez los colores de las casillas alternan según fila + columna.
  isLightSquare(col: number, row: number): boolean {
    return (col + row) % 2 === 0;
  }

  // Crea una descripción accesible que un lector de pantalla puede anunciar.
  getSquareLabel(col: number, row: number): string {
    // Convierte la columna 0-7 a letra a-h y la fila 0-7 a número 8-1.
    const coordinate = `${String.fromCharCode(97 + col)}${8 - row}`;
    const piece = this.getPieceAt(col, row);
    if (!piece) {
      return `${coordinate}, vacía`;
    }

    // Traduce los códigos internos de pieza a nombres comprensibles.
    const pieceNames: Record<string, string> = {
      KING: 'rey', QUEEN: 'reina', ROOK: 'torre', BISHOP: 'alfil', KNIGHT: 'caballo', PAWN: 'peón'
    };
    // Prepara el color y comprueba si esta es la pieza seleccionada.
    let color = 'negras';
    if (piece.color === 0) {
      color = 'blancas';
    }
    let selected = '';
    if (this.selectedPiece()?.col === col && this.selectedPiece()?.row === row) {
      selected = ', seleccionada';
    }
    return `${coordinate}, ${pieceNames[piece.type] ?? 'pieza'} ${color}${selected}`;
  }

  squareTabIndex(col: number, row: number): number {
    if (this.focusedSquare().col === col && this.focusedSquare().row === row) {
      return 0;
    }
    return -1;
  }

  // Permite recorrer el tablero con las teclas de flecha.
  onSquareKeydown(event: KeyboardEvent, col: number, row: number) {
    const isRotated = this.playerColor() === 1;
    const target = { col, row };

    if (event.key === 'ArrowUp') {
      if (isRotated) {
        target.row++;
      } else {
        target.row--;
      }
    } else if (event.key === 'ArrowDown') {
      if (isRotated) {
        target.row--;
      } else {
        target.row++;
      }
    } else if (event.key === 'ArrowLeft') {
      if (isRotated) {
        target.col++;
      } else {
        target.col--;
      }
    } else if (event.key === 'ArrowRight') {
      if (isRotated) {
        target.col--;
      } else {
        target.col++;
      }
    } else {
      return;
    }

    // Ignora otras teclas y movimientos que saldrían del tablero.
    if (!target || target.col < 0 || target.col > 7 || target.row < 0 || target.row > 7) {
      return;
    }

    // Evita el desplazamiento de la página y mueve el foco al botón de la casilla vecina.
    event.preventDefault();
    this.focusedSquare.set(target);
    this.squareButtons.get(this.getSquareIndex(target.col, target.row))?.nativeElement.focus();
  }

  // Angular llama este método al retirar el tablero de la pantalla.
  ngOnDestroy(): void {
    // Detiene el sondeo para evitar peticiones cuando la pantalla ya no existe.
    this.polling?.unsubscribe();
    // Si seguía buscando rival, intenta sacarlo de la cola del backend.
    if (this.queueState() === 'WAITING') {
      const headers = this.authHeaders();
      if (headers) {
        this.http.delete('/api/matchmaking/queue', { headers }).subscribe();
      }
    }
    // Si había una partida activa, deja de escuchar sus actualizaciones.
    const gameId = this.gameId();
    if (gameId) {
      this.chessWsService.leaveGame(gameId);
    }
  }

  // Copia el estado recibido del backend a los signals que utiliza la plantilla.
  private applyState(gameState: SharedGameState) {
    // Actualiza las piezas, el turno y el estado, usando valores seguros si falta algún campo.
    this.pieces.set(gameState.pieces ?? []);
    this.currentColor.set(gameState.currentColor ?? 0);
    this.gameStatus.set(gameState.status ?? 'PLAYING');
    const finishedStatuses = ['CHECKMATE', 'DRAW', 'STALEMATE', 'RESIGNED'];
    if (finishedStatuses.includes(gameState.status ?? 'PLAYING')) {
      this.queueState.set('FINISHED');
    } else {
      this.queueState.set('MATCHED');
    }
    this.drawOfferByUserId.set(gameState.drawOfferByUserId ?? null);
    // Construye un texto de resultado sencillo para jaque mate y rendición.
    if (gameState.winnerId && gameState.status === 'CHECKMATE') {
      if (gameState.winnerId === this.currentUserId) {
        this.resultMessage.set('Jaque mate. Has ganado.');
      } else {
        this.resultMessage.set('Jaque mate. Ha ganado tu rival.');
      }
    } else if (gameState.status === 'RESIGNED' && gameState.winnerId) {
      if (gameState.winnerId === this.currentUserId) {
        this.resultMessage.set('Tu rival se ha rendido. Has ganado.');
      } else {
        this.resultMessage.set('Te has rendido. Ha ganado tu rival.');
      }
    } else {
      // Para otros finales utiliza el mensaje que haya preparado el backend.
      this.resultMessage.set(gameState.resultMessage ?? '');
    }
    // Una actualización nueva elimina el error y la selección anterior.
    this.errorMessage.set('');
    this.selectedPiece.set(null);
    const focused = this.focusedSquare();
    // Si la casilla enfocada ya no contiene pieza, vuelve a una casilla inicial válida.
    if (!this.getPieceAt(focused.col, focused.row)) {
      let focusRow = 6;
      if (this.playerColor() === 1) {
        focusRow = 1;
      }
      this.focusedSquare.set({ col: 4, row: focusRow });
    }
  }

  // Consulta cada 1,5 segundos si el servidor ya encontró un rival.
  private pollForMatch(headers: HttpHeaders) {
    // Sustituye una consulta anterior para que nunca haya dos sondeos activos.
    this.polling?.unsubscribe();
    this.polling = interval(1500).pipe(
      // Ejecuta la primera consulta inmediatamente, sin esperar al primer intervalo.
      startWith(0),
      // Cada intervalo realiza una petición HTTP del estado actual.
      switchMap(() => this.http.get<MatchmakingStatus>('/api/matchmaking/status', { headers })),
      // Sigue consultando mientras espera; incluye la primera respuesta que ya no sea WAITING.
      takeWhile((status) => status.status === 'WAITING', true)
    ).subscribe({
      next: (status) => this.handleMatchmakingStatus(status),
      error: (error) => {
        // Si falla la comunicación, vuelve a permitir iniciar otra búsqueda.
        this.queueState.set('IDLE');
        this.errorMessage.set(error.error?.detail ?? 'Se perdió la conexión con el emparejamiento.');
      }
    });
  }

  // Decide si el resultado inicia una partida aquí o se comunica al componente padre.
  private handleMatchmakingStatus(status: MatchmakingStatus) {
    // En modo cola, el componente padre selecciona la partida cuando aparece un rival.
    if (this.queueEnabled && status.status === 'MATCHED' && status.gameId && status.color !== null) {
      this.polling?.unsubscribe();
      this.queueState.set('IDLE');
      this.matchSelected.emit({
        gameId: status.gameId,
        color: status.color,
        opponent: status.opponent ?? 'Rival'
      });
      return;
    }
    // Muestra el estado recibido; si ya hay rival, activa el tablero con sus datos.
    this.queueState.set(status.status);
    if (status.status === 'MATCHED' && status.gameId && status.color !== null) {
      this.activateMatch({ gameId: status.gameId, color: status.color, opponent: status.opponent ?? 'Rival' });
    }
  }

  // Prepara el tablero y abre las conexiones de la partida asignada.
  private activateMatch(assignment: MatchAssignment) {
    // Ya no hace falta seguir consultando por un rival.
    this.polling?.unsubscribe();
    // En modo cola entrega la partida al padre en vez de abrirla directamente.
    if (this.queueEnabled) {
      this.queueState.set('IDLE');
      this.matchSelected.emit(assignment);
      return;
    }
    // Guarda los datos necesarios para mostrar la partida y el lado del jugador.
    this.gameId.set(assignment.gameId);
    this.playerColor.set(assignment.color);
    let focusRow = 6;
    if (assignment.color === 1) {
      focusRow = 1;
    }
    this.focusedSquare.set({ col: 4, row: focusRow });
    this.opponent.set(assignment.opponent);
    this.queueState.set('MATCHED');
    this.selectedPiece.set(null);
    // Sin token no se pueden consultar los datos privados de la partida.
    const headers = this.authHeaders();
    if (!headers) {
      this.errorMessage.set('La sesión ha caducado. Inicia sesión de nuevo.');
      return;
    }
    // Obtiene el estado inicial del tablero mediante HTTP.
    this.http.get<any>(`/api/games/${assignment.gameId}`, { headers }).subscribe({
      next: (gameState) => this.applyState(gameState),
      error: (error) => this.errorMessage.set(error.error?.detail ?? 'No se pudo cargar la partida.')
    });
    // Luego escucha cambios en tiempo real para sincronizar ambos jugadores.
    this.chessWsService.joinGame(
      assignment.gameId,
      (gameState) => this.applyState(gameState),
      (message) => this.errorMessage.set(message)
    );
    // Informa al componente padre de qué partida se activó.
    this.matchSelected.emit(assignment);
  }

  // Crea una cabecera Authorization a partir del token de sesión, si existe.
  private authHeaders(): HttpHeaders | null {
    const authorization = this.auth.authorizationHeader;
    if (authorization) {
      return new HttpHeaders({ Authorization: authorization });
    }
    return null;
  }
}
