// Herramientas de Angular para crear componentes, observar estado y obtener servicios.
import { Component, effect, inject, signal } from '@angular/core';
// Permite conectar los campos de los formularios con propiedades de esta clase.
import { FormsModule } from '@angular/forms';
// Pantallas que se muestran dentro de la aplicación principal.
import { BoardComponent } from './components/board/board.component';
import { MatchAssignment } from './components/board/board.component';
import { ChatComponent } from './components/chat/chat.component';
import { TournamentsComponent } from './components/tournaments/tournaments.component';
import { LegalNoticeComponent } from './components/legal-notice/legal-notice.component';
import type { LegalPage } from './components/legal-notice/legal-notice.component';
// Servicio de autenticación y tipos de datos que devuelven sus operaciones.
import { AuthService, FriendView, PublicProfile } from './services/auth';
// Canal en tiempo real que avisa cuando una amistad se conecta o desconecta.
import { PresenceWebsocketService } from './services/presence-websocket';

// Secciones disponibles en la navegación principal.
type AppSection = 'home' | 'game' | 'tournament' | 'settings' | 'profile';

@Component({
  // Etiqueta HTML que Angular convierte en la aplicación completa.
  selector: 'app-root',
  // Componentes y módulos que puede usar la plantilla principal.
  imports: [FormsModule, BoardComponent, ChatComponent, TournamentsComponent, LegalNoticeComponent],
  // Archivo HTML que define lo que se ve en cada sección.
  templateUrl: './app.html'
})
export class App {
  // Servicios compartidos para la sesión y el estado de conexión de amistades.
  private auth = inject(AuthService);
  private presence = inject(PresenceWebsocketService);

  // Reutiliza el estado de usuario del servicio de autenticación.
  readonly user = this.auth.user;
  // Etiquetas e identificadores usados para construir la navegación.
  readonly navigation: { id: AppSection; label: string; number: string }[] = [
    { id: 'home', label: 'Home', number: '01' },
    { id: 'game', label: 'Game', number: '02' },
    { id: 'tournament', label: 'Tournament', number: '03' },
    { id: 'settings', label: 'Settings', number: '04' }
  ];
  // Sección que está mostrando la aplicación en este momento.
  activeSection = signal<AppSection>('home');
  // Documento legal abierto, o null cuando la pantalla legal está cerrada.
  legalPage = signal<LegalPage | null>(null);
  // Indica si el formulario de acceso está en modo login o registro.
  mode = signal<'login' | 'register'>('login');
  // Campos que se escriben en el formulario de acceso o registro.
  username = '';
  email = '';
  password = '';
  // Mensaje que se muestra si falla el acceso o registro.
  errorMessage = signal('');
  // Partidas abiertas por el jugador, identificadas para poder mostrarlas/cerrarlas.
  activeMatches = signal<MatchAssignment[]>([]);
  // Campos editables de la pantalla de configuración de la cuenta.
  settingsUsername = this.auth.user()?.username ?? '';
  settingsCurrentPassword = '';
  settingsNewPassword = '';
  settingsAvatarUrl = '';
  deleteAccountPassword = '';
  // Mensajes de resultado de las operaciones de configuración.
  settingsMessage = signal('');
  settingsError = signal('');
  // Datos de la lista de amistades y del buscador.
  friends = signal<FriendView[]>([]);
  friendSearchQuery = '';
  friendSearchResults = signal<FriendView[]>([]);
  friendSearchMessage = signal('');
  friendSearchLoading = signal(false);
  friendsError = signal('');
  // Estado del perfil público que se está consultando.
  publicProfile = signal<PublicProfile | null>(null);
  profileLoading = signal(false);
  profileError = signal('');
  // Recuerda la pantalla anterior para volver a ella al cerrar un perfil.
  private profileReturnSection: AppSection = 'home';
  // Número que permite ignorar respuestas antiguas si se abre otro perfil enseguida.
  private profileRequestId = 0;

  constructor() {
    // effect vuelve a ejecutar este bloque si cambia el usuario conectado.
    effect(() => {
      const currentUser = this.user();
      if (currentUser) {
        // Rellena la configuración con los datos actuales de la cuenta.
        this.settingsUsername = currentUser.username;
        this.settingsAvatarUrl = currentUser.avatarUrl ?? '';
        // Carga las amistades y empieza a escuchar cambios de conexión.
        this.loadFriends();
        this.presence.connect((update) => {
          // Actualiza el estado online en la lista de amigos.
          this.friends.update((friends) => {
            const updatedFriends: FriendView[] = [];
            for (const friend of friends) {
              if (friend.id === update.userId) {
                updatedFriends.push({ ...friend, online: update.online });
              } else {
                updatedFriends.push(friend);
              }
            }
            return updatedFriends;
          });
          // Actualiza también los resultados visibles del buscador.
          this.friendSearchResults.update((results) => {
            const updatedResults: FriendView[] = [];
            for (const result of results) {
              if (result.id === update.userId) {
                updatedResults.push({ ...result, online: update.online });
              } else {
                updatedResults.push(result);
              }
            }
            return updatedResults;
          });
          // Si el perfil abierto es de esa persona, actualiza su presencia también.
          this.publicProfile.update((profile) => {
            if (profile && profile.id === update.userId) {
              return { ...profile, online: update.online };
            }
            return profile;
          });
        });
      } else {
        // Sin sesión no hay que recibir presencia ni conservar listas de la cuenta anterior.
        this.presence.disconnect();
        this.friends.set([]);
        this.friendSearchResults.set([]);
        this.publicProfile.set(null);
      }
    });
  }

  // Envía el formulario de acceso o registro según el modo seleccionado.
  submit() {
    // Limpia un error anterior antes de intentar la nueva petición.
    this.errorMessage.set('');
    let request$: ReturnType<AuthService['login']>;
    if (this.mode() === 'register') {
      request$ = this.auth.register({ username: this.username, email: this.email, password: this.password });
    } else {
      request$ = this.auth.login({ email: this.email, password: this.password });
    }

    // subscribe inicia la petición y define qué hacer con éxito o con error.
    request$.subscribe({
      next: (response) => {
        // Actualiza el nombre que se muestra en la configuración.
        this.settingsUsername = response.user.username;
      },
      error: (err) => this.errorMessage.set(err.error?.error ?? 'No se pudo autenticar.')
    });
  }

  // Cierra la sesión y vuelve a una pantalla limpia de inicio.
  logout() {
    // Evita dejar la aplicación en una sección privada o conservar partidas visibles.
    this.activeSection.set('home');
    this.activeMatches.set([]);
    this.auth.logout().subscribe({ error: () => {} });
  }

  // Cambia la sección principal que se muestra.
  navigateTo(section: AppSection) {
    this.activeSection.set(section);
  }

  // Abre el documento legal seleccionado.
  openLegal(page: LegalPage) {
    this.legalPage.set(page);
  }

  // Cierra el documento legal y devuelve el foco al control que lo abrió.
  closeLegal() {
    const page = this.legalPage();
    this.legalPage.set(null);
    if (page && typeof window !== 'undefined') {
      requestAnimationFrame(() => document.querySelector<HTMLElement>(`[data-legal="${page}"]`)?.focus());
    }
  }

  // Busca el título de la sección activa para mostrarlo en la cabecera.
  sectionTitle() {
    const activeItem = this.navigation.find((item) => item.id === this.activeSection());
    if (activeItem) {
      return activeItem.label;
    }
    if (this.activeSection() === 'profile') {
      return 'Perfil de jugador';
    }
    return 'Home';
  }

  get passwordAutocomplete(): string {
    if (this.mode() === 'register') {
      return 'new-password';
    }
    return 'current-password';
  }

  currentSectionAttribute(section: AppSection): string | null {
    if (this.activeSection() === section) {
      return 'page';
    }
    return null;
  }

  presenceLabel(isOnline: boolean): string {
    if (isOnline) {
      return 'Conectado';
    }
    return 'Desconectado';
  }

  profilePresenceLabel(isOnline: boolean): string {
    if (isOnline) {
      return 'Conectado ahora';
    }
    return 'Desconectado';
  }

  friendSearchButtonLabel(): string {
    if (this.friendSearchLoading()) {
      return 'Buscando...';
    }
    return 'Buscar';
  }

  toggleAuthMode() {
    if (this.mode() === 'register') {
      this.mode.set('login');
    } else {
      this.mode.set('register');
    }
  }

  // Abre un perfil público y solicita sus datos al backend.
  openProfile(userId: number) {
    // Recuerda desde qué sección se abrió el perfil.
    if (this.activeSection() === 'profile') {
      this.profileReturnSection = 'home';
    } else {
      this.profileReturnSection = this.activeSection();
    }
    // Limpia los datos anteriores y muestra el estado de carga.
    this.publicProfile.set(null);
    this.profileError.set('');
    this.profileLoading.set(true);
    this.activeSection.set('profile');
    // Identifica esta petición para descartar respuestas de perfiles abiertos antes.
    const requestId = ++this.profileRequestId;
    this.auth.getPublicProfile(userId).subscribe({
      next: (profile) => {
        // Si el usuario abrió otro perfil, no se debe mostrar esta respuesta antigua.
        if (requestId !== this.profileRequestId) return;
        this.publicProfile.set(profile);
        this.profileLoading.set(false);
      },
      error: (error) => {
        // También se ignoran errores de una consulta que ya dejó de ser la actual.
        if (requestId !== this.profileRequestId) return;
        this.profileError.set(error.error?.error ?? 'No se pudo cargar el perfil.');
        this.profileLoading.set(false);
      }
    });
  }

  // Cierra el perfil y vuelve a la sección desde la que se había abierto.
  closeProfile() {
    this.profileRequestId++;
    if (this.profileReturnSection === 'profile') {
      this.activeSection.set('home');
    } else {
      this.activeSection.set(this.profileReturnSection);
    }
  }

  // Convierte la fecha ISO recibida del backend a una fecha legible en español.
  formatJoinDate(joinedAt: string) {
    return new Intl.DateTimeFormat('es-ES', { dateStyle: 'long' }).format(new Date(joinedAt));
  }

  // Añade la partida seleccionada a la lista y muestra la sección del tablero.
  selectMatch(match: MatchAssignment) {
    // Evita abrir dos veces la misma partida.
    this.activeMatches.update((matches) => {
      for (const activeMatch of matches) {
        if (activeMatch.gameId === match.gameId) {
          return matches;
        }
      }
      return [...matches, match];
    });
    this.activeSection.set('game');
  }

  // Elimina de la interfaz la partida cerrada.
  closeMatch(gameId: string) {
    this.activeMatches.update((matches) => matches.filter((match) => match.gameId !== gameId));
  }

  // Guarda cambios de nombre, contraseña o avatar en la cuenta.
  saveAccountChanges() {
    // Limpia avisos de una operación anterior.
    this.settingsMessage.set('');
    this.settingsError.set('');
    const currentUser = this.user();
    // Solo una sesión iniciada puede cambiar sus datos.
    if (!currentUser) return;

    // La contraseña actual se envía para que el backend confirme la identidad.
    const payload: { currentPassword: string; username?: string; newPassword?: string; avatarUrl?: string | null } = {
      currentPassword: this.settingsCurrentPassword
    };
    const username = this.settingsUsername.trim();
    // Añade al payload únicamente los campos que realmente han cambiado.
    if (username !== currentUser.username) payload.username = username;
    if (this.settingsNewPassword) payload.newPassword = this.settingsNewPassword;
    if (this.settingsAvatarUrl !== (currentUser.avatarUrl ?? '')) payload.avatarUrl = this.settingsAvatarUrl || null;
    if (!payload.username && !payload.newPassword && !('avatarUrl' in payload)) {
      this.settingsError.set('No hay cambios que guardar.');
      return;
    }

    // Envía los cambios y actualiza los campos locales según la respuesta.
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

  // Lee el archivo de imagen seleccionado y lo prepara para mostrarlo/guardarlo como texto.
  onAvatarSelected(event: Event) {
    // Obtiene el campo de archivo y el primer archivo seleccionado.
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.settingsError.set('');
    if (file.size > 3_700_000) {
      // Rechaza imágenes demasiado grandes antes de leerlas.
      this.settingsError.set('La imagen no puede superar 3,7 MB.');
      input.value = '';
      return;
    }

    // FileReader transforma el archivo en una URL de datos que se puede usar como imagen.
    const reader = new FileReader();
    reader.onload = () => {
      this.settingsAvatarUrl = String(reader.result ?? '');
    };
    reader.onerror = () => this.settingsError.set('No se pudo leer la imagen seleccionada.');
    reader.readAsDataURL(file);
  }

  // Pide al backend las amistades de la persona conectada.
  loadFriends() {
    this.auth.getFriends().subscribe({
      next: (friends) => this.friends.set(friends),
      error: () => this.friends.set([])
    });
  }

  // Busca usuarios por nombre y actualiza los resultados o el mensaje correspondiente.
  searchUsers() {
    this.friendsError.set('');
    this.friendSearchMessage.set('');
    const query = this.friendSearchQuery.trim();
    // Exige dos caracteres para evitar consultas demasiado cortas.
    if (query.length < 2) {
      this.friendSearchResults.set([]);
      this.friendSearchMessage.set('Escribe al menos 2 caracteres para buscar.');
      return;
    }

    this.friendSearchLoading.set(true);
    // Marca la búsqueda como activa y la desactiva al recibir éxito o error.
    this.auth.searchUsers(query).subscribe({
      next: (results) => {
        this.friendSearchResults.set(results);
        if (results.length > 0) {
          this.friendSearchMessage.set('');
        } else {
          this.friendSearchMessage.set('No se han encontrado usuarios con ese nombre.');
        }
        this.friendSearchLoading.set(false);
      },
      error: (error) => {
        this.friendSearchResults.set([]);
        this.friendsError.set(error.error?.error ?? 'No se pudo buscar usuarios.');
        this.friendSearchLoading.set(false);
      }
    });
  }

  // Añade una persona como amistad y refresca la lista.
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

  // Elimina una amistad de la lista y del backend.
  removeFriend(friendId: number) {
    this.friendsError.set('');
    this.auth.removeFriend(friendId).subscribe({
      next: () => this.friends.update((friends) => friends.filter((friend) => friend.id !== friendId)),
      error: (error) => this.friendsError.set(error.error?.error ?? 'No se pudo eliminar el amigo.')
    });
  }

  // Pide confirmar la eliminación y luego solicita borrar la cuenta.
  deleteAccount() {
    this.settingsMessage.set('');
    this.settingsError.set('');
    if (!window.confirm('¿Eliminar tu cuenta? Se borrarán tu avatar y amistades; el historial de partidas se conservará anonimizado.')) {
      return;
    }
    // Si el backend confirma el borrado, cierra la sesión y limpia las partidas abiertas.
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
