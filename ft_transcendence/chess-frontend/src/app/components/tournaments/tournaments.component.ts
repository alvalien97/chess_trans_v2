// HttpClient realiza llamadas al backend; HttpHeaders permite enviar el token de sesión.
import { HttpClient, HttpHeaders } from '@angular/common/http';
// Component crea la pantalla; EventEmitter comunica la selección de una partida al componente padre.
import { Component, EventEmitter, OnDestroy, OnInit, Output, inject, signal } from '@angular/core';
// Permite enlazar los campos de los formularios con propiedades de esta clase.
import { FormsModule } from '@angular/forms';
// Subscription sirve para detener una tarea periódica; interval genera avisos cada cierto tiempo.
import { Subscription, interval } from 'rxjs';
// startWith hace una consulta inmediata y switchMap inicia la petición para cada aviso.
import { startWith, switchMap } from 'rxjs/operators';
// Servicios compartidos para la sesión y la partida.
import { AuthService } from '../../services/auth';
import { MatchAssignment } from '../board/board.component';

// Estructura de una partida que forma parte del cuadro de un torneo.
interface TournamentMatch {
  id: string; // Identificador de la partida dentro del torneo.
  round: number; // Ronda del cuadro en la que se juega.
  number: number; // Número de partida dentro de esa ronda.
  whitePlayerId: number; // Identificador de quien juega con blancas.
  whitePlayer: string; // Nombre visible de quien juega con blancas.
  blackPlayerId: number; // Identificador de quien juega con negras.
  blackPlayer: string; // Nombre visible de quien juega con negras.
  winner: string | null; // Ganador, o null mientras no se haya decidido.
  gameId: string; // Identificador usado para cargar la partida de ajedrez.
  status: 'PLAYING' | 'REMATCH' | 'DRAW' | 'COMPLETED'; // Estado posible de la partida.
}

// Información que la pantalla necesita para representar un torneo.
interface Tournament {
  id: string; // Identificador del torneo.
  name: string; // Nombre elegido al crearlo.
  status: 'WAITING' | 'RUNNING' | 'COMPLETED'; // Fase en la que se encuentra.
  owner: string; // Usuario que organiza el torneo.
  maxPlayers: number; // Número de participantes necesarios.
  participants: string[]; // Nombres de las personas apuntadas.
  matches: TournamentMatch[]; // Partidas que componen el cuadro.
}

@Component({
  // Etiqueta con la que el resto de Angular puede insertar esta pantalla.
  selector: 'app-tournaments',
  // Permite utilizar formularios enlazados en la plantilla.
  imports: [FormsModule],
  // Plantilla HTML y hoja de estilos de esta pantalla.
  templateUrl: './tournaments.component.html',
  styleUrl: './tournaments.component.css'
})
export class TournamentsComponent implements OnInit, OnDestroy {
  // Dependencias de Angular necesarias para llamar al backend y leer la sesión.
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  // Guarda la tarea periódica para poder detenerla al cerrar la pantalla.
  private refreshSubscription?: Subscription;

  // Avisa al componente padre qué partida debe abrir el tablero.
  @Output() readonly playGame = new EventEmitter<MatchAssignment>();

  // Datos reactivos que la plantilla muestra y actualiza.
  readonly tournaments = signal<Tournament[]>([]);
  readonly errorMessage = signal('');
  // Copia el nombre conectado para comparar permisos en la pantalla.
  readonly currentUsername = this.auth.user()?.username ?? '';
  // Valores que el usuario puede editar en el formulario de creación.
  tournamentName = '';
  maxPlayers = 4;

  // Al abrir la pantalla, carga los torneos y vuelve a consultarlos cada tres segundos.
  ngOnInit() {
    this.refreshSubscription = interval(3000).pipe(
      startWith(0),
      switchMap(() => this.http.get<Tournament[]>('/api/tournaments', { headers: this.headers() }))
    ).subscribe({
      next: (tournaments) => this.tournaments.set(tournaments),
      error: (error) => this.errorMessage.set(error.error?.detail ?? 'No se pudieron cargar los torneos.')
    });
  }

  // Detiene la actualización periódica al abandonar la pantalla.
  ngOnDestroy() {
    this.refreshSubscription?.unsubscribe();
  }

  // Envía al backend el nombre y la cantidad de jugadores elegidos.
  createTournament() {
    this.request<Tournament>('post', '/api/tournaments', {
      name: this.tournamentName,
      maxPlayers: Number(this.maxPlayers)
    });
  }

  // Solicita apuntarse al torneo seleccionado.
  joinTournament(tournament: Tournament) {
    this.request<Tournament>('post', `/api/tournaments/${tournament.id}/join`, {});
  }

  // Pide al backend iniciar el torneo y generar las partidas.
  startTournament(tournament: Tournament) {
    this.request<Tournament>('post', `/api/tournaments/${tournament.id}/start`, {});
  }

  // Pregunta antes de solicitar al backend que elimine el torneo.
  deleteTournament(tournament: Tournament) {
    if (!window.confirm(`¿Eliminar el torneo "${tournament.name}"?`)) {
      return;
    }
    this.request<void>('delete', `/api/tournaments/${tournament.id}`, undefined);
  }

  // Avisa al componente padre de qué partida abrir y qué color asignar al jugador.
  playMatch(match: TournamentMatch) {
    const isWhite = match.whitePlayer === this.currentUsername;
    let color: number;
    let opponent: string;
    if (isWhite) {
      color = 0;
      opponent = match.blackPlayer;
    } else {
      color = 1;
      opponent = match.whitePlayer;
    }
    this.playGame.emit({
      gameId: match.gameId,
      color,
      opponent
    });
  }

  // Solicita decidir por sorteo el ganador de una partida que terminó en tablas.
  resolveDraw(match: TournamentMatch) {
    this.request<Tournament>('post', `/api/tournaments/matches/${match.id}/resolve-draw`, {});
  }

  // Comprueba si el usuario actual ya está apuntado al torneo.
  isParticipant(tournament: Tournament) {
    return tournament.participants.includes(this.currentUsername);
  }

  // Solo permite apuntarse si el torneo espera jugadores y todavía hay plazas.
  canJoin(tournament: Tournament) {
    return tournament.status === 'WAITING'
      && !this.isParticipant(tournament)
      && tournament.participants.length < tournament.maxPlayers;
  }

  // Solo el organizador puede iniciar el torneo cuando se han llenado las plazas.
  canStart(tournament: Tournament) {
    return tournament.status === 'WAITING'
      && tournament.owner === this.currentUsername
      && tournament.participants.length === tournament.maxPlayers;
  }

  // Ejecuta una acción de torneo y, si funciona, vuelve a cargar la lista.
  private request<T>(method: 'post' | 'delete', url: string, body?: unknown) {
    this.errorMessage.set('');
    this.http.request<T>(method, url, { body, headers: this.headers() }).subscribe({
      next: () => this.reload(),
      error: (error) => this.errorMessage.set(error.error?.detail ?? error.error?.error ?? 'No se pudo completar la acción.')
    });
  }

  // Actualiza la lista después de crear, modificar o eliminar un torneo.
  private reload() {
    this.http.get<Tournament[]>('/api/tournaments', { headers: this.headers() }).subscribe({
      next: (tournaments) => this.tournaments.set(tournaments),
      error: (error) => this.errorMessage.set(error.error?.detail ?? 'No se pudieron cargar los torneos.')
    });
  }

  // Añade a las peticiones el token que identifica al usuario conectado.
  private headers() {
    const authorization = this.auth.authorizationHeader;
    const headers: Record<string, string> = {};
    if (authorization) {
      headers['Authorization'] = authorization;
    }
    return new HttpHeaders(headers);
  }
}
