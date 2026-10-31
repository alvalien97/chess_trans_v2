import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, EventEmitter, OnDestroy, OnInit, Output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription, interval } from 'rxjs';
import { startWith, switchMap } from 'rxjs/operators';
import { AuthService } from '../../services/auth';
import { MatchAssignment } from '../board/board.component';

interface TournamentMatch {
  id: string;
  round: number;
  number: number;
  whitePlayerId: number;
  whitePlayer: string;
  blackPlayerId: number;
  blackPlayer: string;
  winner: string | null;
  gameId: string;
  status: 'PLAYING' | 'REMATCH' | 'DRAW' | 'COMPLETED';
}

interface Tournament {
  id: string;
  name: string;
  status: 'WAITING' | 'RUNNING' | 'COMPLETED';
  owner: string;
  maxPlayers: number;
  participants: string[];
  matches: TournamentMatch[];
}

@Component({
  selector: 'app-tournaments',
  imports: [FormsModule],
  templateUrl: './tournaments.component.html',
  styleUrl: './tournaments.component.css'
})
export class TournamentsComponent implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private refreshSubscription?: Subscription;

  @Output() readonly playGame = new EventEmitter<MatchAssignment>();

  readonly tournaments = signal<Tournament[]>([]);
  readonly errorMessage = signal('');
  readonly currentUsername = this.auth.user()?.username ?? '';
  tournamentName = '';
  maxPlayers = 4;

  ngOnInit() {
    this.refreshSubscription = interval(3000).pipe(
      startWith(0),
      switchMap(() => this.http.get<Tournament[]>('/api/tournaments', { headers: this.headers() }))
    ).subscribe({
      next: (tournaments) => this.tournaments.set(tournaments),
      error: (error) => this.errorMessage.set(error.error?.detail ?? 'No se pudieron cargar los torneos.')
    });
  }

  ngOnDestroy() {
    this.refreshSubscription?.unsubscribe();
  }

  createTournament() {
    this.request<Tournament>('post', '/api/tournaments', {
      name: this.tournamentName,
      maxPlayers: Number(this.maxPlayers)
    });
  }

  joinTournament(tournament: Tournament) {
    this.request<Tournament>('post', `/api/tournaments/${tournament.id}/join`, {});
  }

  startTournament(tournament: Tournament) {
    this.request<Tournament>('post', `/api/tournaments/${tournament.id}/start`, {});
  }

  deleteTournament(tournament: Tournament) {
    if (!window.confirm(`¿Eliminar el torneo "${tournament.name}"?`)) {
      return;
    }
    this.request<void>('delete', `/api/tournaments/${tournament.id}`, undefined);
  }

  playMatch(match: TournamentMatch) {
    const isWhite = match.whitePlayer === this.currentUsername;
    this.playGame.emit({
      gameId: match.gameId,
      color: isWhite ? 0 : 1,
      opponent: isWhite ? match.blackPlayer : match.whitePlayer
    });
  }

  resolveDraw(match: TournamentMatch) {
    this.request<Tournament>('post', `/api/tournaments/matches/${match.id}/resolve-draw`, {});
  }

  isParticipant(tournament: Tournament) {
    return tournament.participants.includes(this.currentUsername);
  }

  canJoin(tournament: Tournament) {
    return tournament.status === 'WAITING'
      && !this.isParticipant(tournament)
      && tournament.participants.length < tournament.maxPlayers;
  }

  canStart(tournament: Tournament) {
    return tournament.status === 'WAITING'
      && tournament.owner === this.currentUsername
      && tournament.participants.length === tournament.maxPlayers;
  }

  private request<T>(method: 'post' | 'delete', url: string, body?: unknown) {
    this.errorMessage.set('');
    this.http.request<T>(method, url, { body, headers: this.headers() }).subscribe({
      next: () => this.reload(),
      error: (error) => this.errorMessage.set(error.error?.detail ?? error.error?.error ?? 'No se pudo completar la acción.')
    });
  }

  private reload() {
    this.http.get<Tournament[]>('/api/tournaments', { headers: this.headers() }).subscribe({
      next: (tournaments) => this.tournaments.set(tournaments),
      error: (error) => this.errorMessage.set(error.error?.detail ?? 'No se pudieron cargar los torneos.')
    });
  }

  private headers() {
    const authorization = this.auth.authorizationHeader;
    return new HttpHeaders(authorization ? { Authorization: authorization } : {});
  }
}
