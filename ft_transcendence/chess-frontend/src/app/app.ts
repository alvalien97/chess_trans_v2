import { Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';//importo modulo de formularios
import { BoardComponent } from './components/board/board.component';
import { MatchAssignment } from './components/board/board.component';
import { ChatComponent } from './components/chat/chat.component';
import { TournamentsComponent } from './components/tournaments/tournaments.component';
import { LegalNoticeComponent } from './components/legal-notice/legal-notice.component';
import type { LegalPage } from './components/legal-notice/legal-notice.component';
import { AuthService, FriendView, PublicProfile } from './services/auth';//modulo para gestionar usuarios y autenticaciones
import { PresenceWebsocketService } from './services/presence-websocket';

type AppSection = 'home' | 'game' | 'tournament' | 'settings' | 'profile';

@Component({
  selector: 'app-root',//define la etiqueta de app asociada
  imports: [FormsModule, BoardComponent, ChatComponent, TournamentsComponent, LegalNoticeComponent],
  templateUrl: './app.html'
})
export class App {
  private auth = inject(AuthService);
  private presence = inject(PresenceWebsocketService);

  readonly user = this.auth.user;
  readonly navigation: { id: AppSection; label: string; number: string }[] = [
    { id: 'home', label: 'Home', number: '01' },
    { id: 'game', label: 'Game', number: '02' },
    { id: 'tournament', label: 'Tournament', number: '03' },
    { id: 'settings', label: 'Settings', number: '04' }
  ];
  activeSection = signal<AppSection>('home');
  legalPage = signal<LegalPage | null>(null);
  mode = signal<'login' | 'register'>('login');
  username = '';
  email = '';
  password = '';
  errorMessage = signal('');
  activeMatches = signal<MatchAssignment[]>([]);
  settingsUsername = this.auth.user()?.username ?? '';
  settingsCurrentPassword = '';
  settingsNewPassword = '';
  settingsAvatarUrl = '';
  deleteAccountPassword = '';
  settingsMessage = signal('');
  settingsError = signal('');
  friends = signal<FriendView[]>([]);
  friendSearchQuery = '';
  friendSearchResults = signal<FriendView[]>([]);
  friendSearchMessage = signal('');
  friendSearchLoading = signal(false);
  friendsError = signal('');
  publicProfile = signal<PublicProfile | null>(null);
  profileLoading = signal(false);
  profileError = signal('');
  private profileReturnSection: AppSection = 'home';
  private profileRequestId = 0;

  constructor() {
    effect(() => {
      const currentUser = this.user();
      if (currentUser) {
        this.settingsUsername = currentUser.username;
        this.settingsAvatarUrl = currentUser.avatarUrl ?? '';
        this.loadFriends();
        this.presence.connect((update) => {
          this.friends.update((friends) => friends.map((friend) => friend.id === update.userId
            ? { ...friend, online: update.online }
            : friend));
          this.friendSearchResults.update((results) => results.map((result) => result.id === update.userId
            ? { ...result, online: update.online }
            : result));
          this.publicProfile.update((profile) => profile?.id === update.userId
            ? { ...profile, online: update.online }
            : profile);
        });
      } else {
        this.presence.disconnect();
        this.friends.set([]);
        this.friendSearchResults.set([]);
        this.publicProfile.set(null);
      }
    });
  }

  submit() {
    this.errorMessage.set('');
    const request$ = this.mode() === 'register'
      ? this.auth.register({ username: this.username, email: this.email, password: this.password })
      : this.auth.login({ email: this.email, password: this.password });//si estas en el registro compruebo pasandole los datos

    request$.subscribe({//se suscribe y comprueba que no haya errores
      next: (response) => {
        this.settingsUsername = response.user.username;
      },
      error: (err) => this.errorMessage.set(err.error?.error ?? 'No se pudo autenticar.')
    });
  }

  logout() {//metodo para cerrar sesion
    this.activeSection.set('home');
    this.activeMatches.set([]);
    this.auth.logout().subscribe({ error: () => {} });
  }

  navigateTo(section: AppSection) {
    this.activeSection.set(section);
  }

  openLegal(page: LegalPage) {
    this.legalPage.set(page);
  }

  closeLegal() {
    const page = this.legalPage();
    this.legalPage.set(null);
    if (page && typeof window !== 'undefined') {
      requestAnimationFrame(() => document.querySelector<HTMLElement>(`[data-legal="${page}"]`)?.focus());
    }
  }

  sectionTitle() {
    return this.navigation.find((item) => item.id === this.activeSection())?.label
      ?? (this.activeSection() === 'profile' ? 'Perfil de jugador' : 'Home');
  }

  openProfile(userId: number) {
    this.profileReturnSection = this.activeSection() === 'profile' ? 'home' : this.activeSection();
    this.publicProfile.set(null);
    this.profileError.set('');
    this.profileLoading.set(true);
    this.activeSection.set('profile');
    const requestId = ++this.profileRequestId;
    this.auth.getPublicProfile(userId).subscribe({
      next: (profile) => {
        if (requestId !== this.profileRequestId) return;
        this.publicProfile.set(profile);
        this.profileLoading.set(false);
      },
      error: (error) => {
        if (requestId !== this.profileRequestId) return;
        this.profileError.set(error.error?.error ?? 'No se pudo cargar el perfil.');
        this.profileLoading.set(false);
      }
    });
  }

  closeProfile() {
    this.profileRequestId++;
    this.activeSection.set(this.profileReturnSection === 'profile' ? 'home' : this.profileReturnSection);
  }

  formatJoinDate(joinedAt: string) {
    return new Intl.DateTimeFormat('es-ES', { dateStyle: 'long' }).format(new Date(joinedAt));
  }

  selectMatch(match: MatchAssignment) {//metodo para seleccionar match
    this.activeMatches.update((matches) => matches.some((item) => item.gameId === match.gameId)
      ? matches
      : [...matches, match]);
    this.activeSection.set('game');
  }

  closeMatch(gameId: string) {
    this.activeMatches.update((matches) => matches.filter((match) => match.gameId !== gameId));
  }

  saveAccountChanges() {
    this.settingsMessage.set('');
    this.settingsError.set('');
    const currentUser = this.user();
    if (!currentUser) return;

    const payload: { currentPassword: string; username?: string; newPassword?: string; avatarUrl?: string | null } = {
      currentPassword: this.settingsCurrentPassword
    };
    const username = this.settingsUsername.trim();
    if (username !== currentUser.username) payload.username = username;
    if (this.settingsNewPassword) payload.newPassword = this.settingsNewPassword;
    if (this.settingsAvatarUrl !== (currentUser.avatarUrl ?? '')) payload.avatarUrl = this.settingsAvatarUrl || null;
    if (!payload.username && !payload.newPassword && !('avatarUrl' in payload)) {
      this.settingsError.set('No hay cambios que guardar.');
      return;
    }

    this.auth.updateAccount(payload).subscribe({
      next: (updatedUser) => {
        this.settingsUsername = updatedUser.username;
        this.settingsCurrentPassword = '';
        this.settingsNewPassword = '';
        this.settingsAvatarUrl = updatedUser.avatarUrl ?? '';
        this.settingsMessage.set('Cambios guardados.');
      },
      error: (error) => this.settingsError.set(error.error?.error ?? 'No se pudieron guardar los cambios.')
    });
  }

  onAvatarSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.settingsError.set('');
    if (file.size > 3_700_000) {
      this.settingsError.set('La imagen no puede superar 3,7 MB.');
      input.value = '';
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      this.settingsAvatarUrl = String(reader.result ?? '');
    };
    reader.onerror = () => this.settingsError.set('No se pudo leer la imagen seleccionada.');
    reader.readAsDataURL(file);
  }

  loadFriends() {
    this.auth.getFriends().subscribe({
      next: (friends) => this.friends.set(friends),
      error: () => this.friends.set([])
    });
  }

  searchUsers() {
    this.friendsError.set('');
    this.friendSearchMessage.set('');
    const query = this.friendSearchQuery.trim();
    if (query.length < 2) {
      this.friendSearchResults.set([]);
      this.friendSearchMessage.set('Escribe al menos 2 caracteres para buscar.');
      return;
    }

    this.friendSearchLoading.set(true);
    this.auth.searchUsers(query).subscribe({
      next: (results) => {
        this.friendSearchResults.set(results);
        this.friendSearchMessage.set(results.length ? '' : 'No se han encontrado usuarios con ese nombre.');
        this.friendSearchLoading.set(false);
      },
      error: (error) => {
        this.friendSearchResults.set([]);
        this.friendsError.set(error.error?.error ?? 'No se pudo buscar usuarios.');
        this.friendSearchLoading.set(false);
      }
    });
  }

  addFriend(username: string) {
    this.friendsError.set('');
    this.auth.addFriend(username).subscribe({
      next: () => {
        this.friendSearchResults.update((results) => results.filter((friend) => friend.username !== username));
        this.friendSearchMessage.set(`${username} añadido a tus amigos.`);
        this.loadFriends();
      },
      error: (error) => this.friendsError.set(error.error?.error ?? 'No se pudo añadir el amigo.')
    });
  }

  removeFriend(friendId: number) {
    this.friendsError.set('');
    this.auth.removeFriend(friendId).subscribe({
      next: () => this.friends.update((friends) => friends.filter((friend) => friend.id !== friendId)),
      error: (error) => this.friendsError.set(error.error?.error ?? 'No se pudo eliminar el amigo.')
    });
  }

  deleteAccount() {
    this.settingsMessage.set('');
    this.settingsError.set('');
    if (!window.confirm('¿Eliminar tu cuenta? Se borrarán tu avatar y amistades; el historial de partidas se conservará anonimizado.')) {
      return;
    }
    this.auth.deleteAccount(this.deleteAccountPassword).subscribe({
      next: () => {
        this.activeSection.set('home');
        this.activeMatches.set([]);
        this.auth.clearLocalSession();
      },
      error: (error) => this.settingsError.set(error.error?.error ?? 'No se pudo eliminar la cuenta.')
    });
  }
}
//Conecto tablero chat y torneos
