// Injectable registra un servicio en Angular; signal guarda estado que la interfaz observa.
import { Injectable, signal } from '@angular/core';
// HttpClient envía peticiones al backend y HttpHeaders añade cabeceras a esas peticiones.
import { HttpClient, HttpHeaders } from '@angular/common/http';
// tap permite reaccionar a una respuesta; finalize ejecuta una acción al terminar la petición.
import { finalize, tap } from 'rxjs/operators';

// Campos de la cuenta que la aplicación necesita mostrar o utilizar.
export interface AuthUser {
  id: number; // Identificador único de la cuenta.
  username: string; // Nombre visible del usuario.
  email: string; // Correo asociado a la cuenta.
  avatarUrl?: string | null; // Imagen opcional; puede faltar o no estar configurada.
}

// Información breve de una amistad, incluida su disponibilidad.
export interface FriendView {
  id: number; // Identificador del amigo.
  username: string; // Nombre que se muestra en pantalla.
  avatarUrl?: string | null; // Dirección de la imagen de perfil, si existe.
  online: boolean; // Indica si el amigo está conectado.
}

// Datos que se pueden mostrar al visitar el perfil público de otra persona.
export interface PublicProfile {
  id: number; // Identificador de la cuenta.
  username: string; // Nombre público del jugador.
  avatarUrl?: string | null; // Imagen de perfil opcional.
  joinedAt: string; // Fecha de registro recibida como texto desde el backend.
  online: boolean; // Estado de conexión actual.
}

// Estructura de la respuesta del backend después de registrarse o iniciar sesión.
interface AuthResponse {
  token: string; // Credencial que la aplicación enviará en futuras peticiones.
  user: AuthUser;
}

@Injectable({
  providedIn: 'root' // Angular comparte una única instancia de este servicio.
})
export class AuthService {
  // Estado reactivo: sus cambios se reflejan automáticamente en la interfaz.
  readonly user = signal<AuthUser | null>(this.readUser());

  // Angular proporciona HttpClient para que el servicio pueda hablar con el backend.
  constructor(private http: HttpClient) {}

  // Prepara el valor de autenticación; devuelve null cuando no hay token.
  get authorizationHeader(): string | null {
    // sessionStorage existe en el navegador, pero no durante el renderizado en servidor.
    if (typeof sessionStorage === 'undefined') {
      return null;
    }
    // Recupera la credencial que se guardó al iniciar sesión.
    const token = sessionStorage.getItem('token');
    if (token) {
      return `Bearer ${token}`;
    }
    return null;
  }

  // Envía los datos de registro y guarda la sesión cuando el backend responde correctamente.
  register(payload: { username: string; email: string; password: string }) {
    return this.http.post<AuthResponse>('/api/auth/register', payload).pipe(
      tap((response) => this.store(response))
    );
  }

  // Comprueba las credenciales en el backend y guarda la sesión si son válidas.
  login(payload: { email: string; password: string }) {
    return this.http.post<AuthResponse>('/api/auth/login', payload).pipe(
      tap((response) => this.store(response))
    );
  }

  // Actualiza la cuenta; los campos opcionales solo se envían si van a cambiar.
  updateAccount(payload: { currentPassword: string; username?: string; newPassword?: string; avatarUrl?: string | null }) {
    return this.http.put<AuthUser>('/api/auth/account', payload, {
      headers: this.authHeaders()
    }).pipe(tap((user) => {
      // Guarda los datos nuevos en el navegador y actualiza el estado de Angular.
      sessionStorage.setItem('user', JSON.stringify(user));
      this.user.set(user);
    }));
  }

  // Pide al backend la lista de amistades de la persona conectada.
  getFriends() {
    return this.http.get<FriendView[]>('/api/auth/friends', {
      headers: this.authHeaders()
    });
  }

  // Busca usuarios cuyo nombre coincida con el texto indicado.
  searchUsers(query: string) {
    return this.http.get<FriendView[]>('/api/auth/friends/search', {
      headers: this.authHeaders(),
      params: { query }
    });
  }

  // Recupera los datos públicos del usuario indicado por su identificador.
  getPublicProfile(userId: number) {
    return this.http.get<PublicProfile>(`/api/auth/users/${userId}`, {
      headers: this.authHeaders()
    });
  }

  // Solicita crear una amistad usando el nombre de usuario de la otra persona.
  addFriend(username: string) {
    return this.http.post<FriendView>('/api/auth/friends', { username }, {
      headers: this.authHeaders()
    });
  }

  // Solicita eliminar la amistad con el usuario indicado.
  removeFriend(friendId: number) {
    return this.http.delete<void>(`/api/auth/friends/${friendId}`, {
      headers: this.authHeaders()
    });
  }

  // Solicita borrar la cuenta; el backend comprueba de nuevo la contraseña.
  deleteAccount(currentPassword: string) {
    return this.http.delete<void>('/api/auth/account', {
      headers: this.authHeaders(),
      body: { currentPassword }
    });
  }

  // Avisa al backend del cierre de sesión y borra la sesión local al terminar.
  logout() {
    return this.http.post<void>('/api/auth/logout', {}, {
      headers: this.authHeaders()
    }).pipe(finalize(() => this.clearLocalSession()));
  }

  // Elimina las credenciales del navegador y actualiza la interfaz como desconectada.
  clearLocalSession() {
    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.removeItem('token');
      sessionStorage.removeItem('user');
    }
    this.user.set(null);
  }

  // Guarda en el navegador la respuesta correcta de un inicio de sesión o registro.
  private store(response: AuthResponse) {
    sessionStorage.setItem('token', response.token);
    sessionStorage.setItem('user', JSON.stringify(response.user));
    this.user.set(response.user);
  }

  // Recupera los datos de usuario al crear el servicio para restaurar la sesión de la pestaña.
  private readUser(): AuthUser | null {
    // En el servidor no existe el almacenamiento del navegador.
    if (typeof sessionStorage === 'undefined') {
      return null;
    }
    // JSON.parse convierte el texto almacenado en un objeto de usuario.
    const raw = sessionStorage.getItem('user');
    if (raw) {
      return JSON.parse(raw);
    }
    return null;
  }

  // Crea las cabeceras HTTP e incluye el token cuando hay una sesión activa.
  private authHeaders() {
    const authorization = this.authorizationHeader;
    const headers: Record<string, string> = {};
    if (authorization) {
      headers['Authorization'] = authorization;
    }
    return new HttpHeaders(headers);
  }
}
