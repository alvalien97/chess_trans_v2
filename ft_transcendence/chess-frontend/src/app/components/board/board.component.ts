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
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Subscription, interval } from 'rxjs';
import { startWith, switchMap, takeWhile } from 'rxjs/operators';
import { ChessWebsocketService } from '../../services/chess-websocket';
import { AuthService } from '../../services/auth';

export interface MatchAssignment {
  gameId: string;
  color: number;
  opponent: string;
}

interface MatchmakingStatus {
  status: 'IDLE' | 'WAITING' | 'MATCHED';
  gameId: string | null;
  color: number | null;
  opponent: string | null;
}

interface SharedGameState {
  pieces?: any[];
  currentColor?: number;
  status?: string;
  drawOfferByUserId?: number | null;
  winnerId?: number | null;
  resultMessage?: string | null;
}

@Component({
  selector: 'app-board',
  templateUrl: './board.component.html',
  styleUrls: ['./board.component.css']
})
export class BoardComponent implements OnInit, OnDestroy {
  private chessWsService = inject(ChessWebsocketService);
  private http = inject(HttpClient);
  private auth = inject(AuthService);
  private polling?: Subscription;
  @ViewChildren('squareButton', { read: ElementRef })
  private squareButtons!: QueryList<ElementRef<HTMLButtonElement>>;

  gameId = signal<string | null>(null);
  queueState = signal<'IDLE' | 'WAITING' | 'MATCHED' | 'FINISHED'>('IDLE');
  playerColor = signal<number | null>(null);
  opponent = signal('');
  pieces = signal<any[]>(this.createInitialPieces());
  
  private createInitialPieces() {
    const pieces: any[] = [];
    const backRank = ['ROOK', 'KNIGHT', 'BISHOP', 'QUEEN', 'KING', 'BISHOP', 'KNIGHT', 'ROOK'];
    
    for (let col = 0; col < 8; col++) {
      pieces.push({ color: 1, col, row: 0, type: backRank[col] });
      pieces.push({ color: 1, col, row: 1, type: 'PAWN' });
      pieces.push({ color: 0, col, row: 6, type: 'PAWN' });
      pieces.push({ color: 0, col, row: 7, type: backRank[col] });
    }
    
    return pieces;
  }
  selectedPiece = signal<any>(null);
  currentColor = signal(0);
  errorMessage = signal('');
  gameStatus = signal('');
  drawOfferByUserId = signal<number | null>(null);
  resultMessage = signal('');
  readonly currentUserId = this.auth.user()?.id ?? null;
  focusedSquare = signal({ col: 0, row: 0 });

  @Output() matchSelected = new EventEmitter<MatchAssignment>();
  @Output() closeGame = new EventEmitter<string>();

  @Input() queueEnabled = false;

  @Input() set match(assignment: MatchAssignment | null) {
    if (assignment && assignment.gameId !== this.gameId()) {
      this.activateMatch(assignment);
    }
  }

  boardRows = [0, 1, 2, 3, 4, 5, 6, 7];
  boardCols = [0, 1, 2, 3, 4, 5, 6, 7];

  ngOnInit(): void {
    this.gameStatus.set('');
  }

  findOpponent() {
    const headers = this.authHeaders();
    if (!headers) {
      this.errorMessage.set('Inicia sesión para buscar una partida.');
      return;
    }
    this.errorMessage.set('');
    this.http.post<MatchmakingStatus>('/api/matchmaking/queue', {}, { headers }).subscribe({
      next: (status) => {
        this.handleMatchmakingStatus(status);
        if (status.status === 'WAITING') {
          this.pollForMatch(headers);
        }
      },
      error: (error) => this.errorMessage.set(error.error?.detail ?? 'No se pudo iniciar la búsqueda.')
    });
  }

  closeFinishedGame() {
    const gameId = this.gameId();
    if (gameId && this.queueState() === 'FINISHED') {
      this.closeGame.emit(gameId);
    }
  }

  resign() {
    const gameId = this.gameId();
    if (!gameId || !window.confirm('¿Rendirte? Tu rival ganará esta partida.')) return;
    if (!this.chessWsService.resign(gameId)) {
      this.errorMessage.set('Sin conexión con la partida. No se pudo enviar la rendición.');
    }
  }

  offerDraw() {
    const gameId = this.gameId();
    if (gameId && !this.chessWsService.offerDraw(gameId)) {
      this.errorMessage.set('Sin conexión con la partida. No se pudo enviar la oferta.');
    }
  }

  respondToDraw(accepted: boolean) {
    const gameId = this.gameId();
    if (gameId && !this.chessWsService.respondToDraw(gameId, accepted)) {
      this.errorMessage.set('Sin conexión con la partida. No se pudo enviar la respuesta.');
    }
  }

  cancelSearch() {
    this.polling?.unsubscribe();
    this.queueState.set('IDLE');
    const headers = this.authHeaders();
    if (headers) {
      this.http.delete('/api/matchmaking/queue', { headers }).subscribe();
    }
  }

  onSquareClick(col: number, row: number) {
    this.focusedSquare.set({ col, row });
    if (this.queueState() !== 'MATCHED' || this.currentColor() !== this.playerColor()) {
      return;
    }

    const clickedPiece = this.getPieceAt(col, row);
    const selected = this.selectedPiece();

    if (clickedPiece?.color === this.playerColor()) {
      this.selectedPiece.set(clickedPiece);
      return;
    }

    if (selected) {
      const gameId = this.gameId();
      if (!gameId) {
        return;
      }
      const sent = this.chessWsService.sendMove(gameId, {
        fromCol: selected.col,
        fromRow: selected.row,
        toCol: col,
        toRow: row
      });
      if (!sent) {
        this.errorMessage.set('Sin conexión con la partida. El movimiento no se envió; inténtalo de nuevo.');
      }
    } else if (clickedPiece) {
      this.selectedPiece.set(clickedPiece);
    }
  }

  getPieceAt(col: number, row: number) {
    return this.pieces().find(p => p.col === col && p.row === row);
  }

  getPieceSymbol(piece: any): string {
    const white: Record<string, string> = {
      KING: '♔', QUEEN: '♕', ROOK: '♖', BISHOP: '♗', KNIGHT: '♘', PAWN: '♙'
    };
    const black: Record<string, string> = {
      KING: '♚', QUEEN: '♛', ROOK: '♜', BISHOP: '♝', KNIGHT: '♞', PAWN: '♟'
    };
    const table = piece.color === 0 ? white : black;
    return table[piece.type] ?? '?';
  }

  isLightSquare(col: number, row: number): boolean {
    return (col + row) % 2 === 0;
  }

  getSquareLabel(col: number, row: number): string {
    const coordinate = `${String.fromCharCode(97 + col)}${8 - row}`;
    const piece = this.getPieceAt(col, row);
    if (!piece) {
      return `${coordinate}, vacía`;
    }

    const pieceNames: Record<string, string> = {
      KING: 'rey', QUEEN: 'reina', ROOK: 'torre', BISHOP: 'alfil', KNIGHT: 'caballo', PAWN: 'peón'
    };
    const color = piece.color === 0 ? 'blancas' : 'negras';
    const selected = this.selectedPiece()?.col === col && this.selectedPiece()?.row === row;
    return `${coordinate}, ${pieceNames[piece.type] ?? 'pieza'} ${color}${selected ? ', seleccionada' : ''}`;
  }

  onSquareKeydown(event: KeyboardEvent, col: number, row: number) {
    const target = {
      ArrowUp: { col, row: row - 1 },
      ArrowDown: { col, row: row + 1 },
      ArrowLeft: { col: col - 1, row },
      ArrowRight: { col: col + 1, row }
    }[event.key];

    if (!target || target.col < 0 || target.col > 7 || target.row < 0 || target.row > 7) {
      return;
    }

    event.preventDefault();
    this.focusedSquare.set(target);
    this.squareButtons.get(target.row * 8 + target.col)?.nativeElement.focus();
  }

  ngOnDestroy(): void {
    this.polling?.unsubscribe();
    if (this.queueState() === 'WAITING') {
      const headers = this.authHeaders();
      if (headers) {
        this.http.delete('/api/matchmaking/queue', { headers }).subscribe();
      }
    }
    const gameId = this.gameId();
    if (gameId) {
      this.chessWsService.leaveGame(gameId);
    }
  }

  private applyState(gameState: SharedGameState) {
    this.pieces.set(gameState.pieces ?? []);
    this.currentColor.set(gameState.currentColor ?? 0);
    this.gameStatus.set(gameState.status ?? 'PLAYING');
    this.queueState.set(['CHECKMATE', 'DRAW', 'STALEMATE', 'RESIGNED'].includes(gameState.status ?? 'PLAYING')
      ? 'FINISHED'
      : 'MATCHED');
    this.drawOfferByUserId.set(gameState.drawOfferByUserId ?? null);
    if (gameState.winnerId && gameState.status === 'CHECKMATE') {
      this.resultMessage.set(gameState.winnerId === this.currentUserId
        ? 'Jaque mate. Has ganado.'
        : 'Jaque mate. Ha ganado tu rival.');
    } else if (gameState.status === 'RESIGNED' && gameState.winnerId) {
      this.resultMessage.set(gameState.winnerId === this.currentUserId
        ? 'Tu rival se ha rendido. Has ganado.'
        : 'Te has rendido. Ha ganado tu rival.');
    } else {
      this.resultMessage.set(gameState.resultMessage ?? '');
    }
    this.errorMessage.set('');
    this.selectedPiece.set(null);
    const focused = this.focusedSquare();
    if (!this.getPieceAt(focused.col, focused.row)) {
      this.focusedSquare.set({ col: 4, row: 6 });
    }
  }

  private pollForMatch(headers: HttpHeaders) {
    this.polling?.unsubscribe();
    this.polling = interval(1500).pipe(
      startWith(0),
      switchMap(() => this.http.get<MatchmakingStatus>('/api/matchmaking/status', { headers })),
      takeWhile((status) => status.status === 'WAITING', true)
    ).subscribe({
      next: (status) => this.handleMatchmakingStatus(status),
      error: (error) => {
        this.queueState.set('IDLE');
        this.errorMessage.set(error.error?.detail ?? 'Se perdió la conexión con el emparejamiento.');
      }
    });
  }

  private handleMatchmakingStatus(status: MatchmakingStatus) {
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
    this.queueState.set(status.status);
    if (status.status === 'MATCHED' && status.gameId && status.color !== null) {
      this.activateMatch({ gameId: status.gameId, color: status.color, opponent: status.opponent ?? 'Rival' });
    }
  }

  private activateMatch(assignment: MatchAssignment) {
    this.polling?.unsubscribe();
    if (this.queueEnabled) {
      this.queueState.set('IDLE');
      this.matchSelected.emit(assignment);
      return;
    }
    this.gameId.set(assignment.gameId);
    this.playerColor.set(assignment.color);
    this.opponent.set(assignment.opponent);
    this.queueState.set('MATCHED');
    this.selectedPiece.set(null);
    const headers = this.authHeaders();
    if (!headers) {
      this.errorMessage.set('La sesión ha caducado. Inicia sesión de nuevo.');
      return;
    }
    this.http.get<any>(`/api/games/${assignment.gameId}`, { headers }).subscribe({
      next: (gameState) => this.applyState(gameState),
      error: (error) => this.errorMessage.set(error.error?.detail ?? 'No se pudo cargar la partida.')
    });
    this.chessWsService.joinGame(
      assignment.gameId,
      (gameState) => this.applyState(gameState),
      (message) => this.errorMessage.set(message)
    );
    this.matchSelected.emit(assignment);
  }

  private authHeaders(): HttpHeaders | null {
    const authorization = this.auth.authorizationHeader;
    return authorization ? new HttpHeaders({ Authorization: authorization }) : null;
  }
}
