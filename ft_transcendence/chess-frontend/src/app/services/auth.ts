import { Injectable, signal } from '@angular/core';//importo signal para reacciones en angular
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { finalize, tap } from 'rxjs/operators';//no hace cambios pero ejecuta efectos secundarios

export interface AuthUser {
  id: number;
  username: string;
  email: string;
  avatarUrl?: string | null;
}

export interface FriendView {
  id: number;
  username: string;
  avatarUrl?: string | null;
  online: boolean;
}

export interface PublicProfile {
  id: number;
  username: string;
  avatarUrl?: string | null;
  joinedAt: string;
  online: boolean;
}

interface AuthResponse {
  token: string;//token del backend
  user: AuthUser;
}

@Injectable({
  providedIn: 'root'//servicio inyectable ready to en toda la app
})
export class AuthService {
  readonly user = signal<AuthUser | null>(this.readUser());

  constructor(private http: HttpClient) {}//le meto el cliente http por el constructor

  get authorizationHeader(): string | null {//el getter para confirmar al user que usa su token del backend
    if (typeof sessionStorage === 'undefined') {
      return null;
    }
    const token = sessionStorage.getItem('token');
    return token ? `Bearer ${token}` : null;
  }

  register(payload: { username: string; email: string; password: string }) {//registro un nuevo user con sus datos
    return this.http.post<AuthResponse>('/api/auth/register', payload).pipe(
      tap((response) => this.store(response))
    );
  }

  login(payload: { email: string; password: string }) {//permite iniciar sesion con email y contraseña
    return this.http.post<AuthResponse>('/api/auth/login', payload).pipe(//realiza el metodo post para la peticion mediante un pie
      tap((response) => this.store(response))
    );
  }

  updateAccount(payload: { currentPassword: string; username?: string; newPassword?: string; avatarUrl?: string | null }) {
    return this.http.put<AuthUser>('/api/auth/account', payload, {
      headers: this.authHeaders()
    }).pipe(tap((user) => {
      sessionStorage.setItem('user', JSON.stringify(user));
      this.user.set(user);
    }));
  }

  getFriends() {
    return this.http.get<FriendView[]>('/api/auth/friends', {
      headers: this.authHeaders()
    });
  }

  searchUsers(query: string) {
    return this.http.get<FriendView[]>('/api/auth/friends/search', {
      headers: this.authHeaders(),
      params: { query }
    });
  }

  getPublicProfile(userId: number) {
    return this.http.get<PublicProfile>(`/api/auth/users/${userId}`, {
      headers: this.authHeaders()
    });
  }

  addFriend(username: string) {
    return this.http.post<FriendView>('/api/auth/friends', { username }, {
      headers: this.authHeaders()
    });
  }

  removeFriend(friendId: number) {
    return this.http.delete<void>(`/api/auth/friends/${friendId}`, {
      headers: this.authHeaders()
    });
  }

  deleteAccount(currentPassword: string) {
    return this.http.delete<void>('/api/auth/account', {
      headers: this.authHeaders(),
      body: { currentPassword }
    });
  }

  logout() {
    return this.http.post<void>('/api/auth/logout', {}, {
      headers: this.authHeaders()
    }).pipe(finalize(() => this.clearLocalSession()));
  }

  clearLocalSession() {
    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.removeItem('token');
      sessionStorage.removeItem('user');
    }
    this.user.set(null);
  }

  private store(response: AuthResponse) {//almacenar datos de sesion por si acaso cieroo sin querer
    sessionStorage.setItem('token', response.token);
    sessionStorage.setItem('user', JSON.stringify(response.user));
    this.user.set(response.user);
  }

  private readUser(): AuthUser | null {//comprobacion por si user vuelve a conectarse
    if (typeof sessionStorage === 'undefined') {//me aseguro que el codigo se ejeuta en el navegador
      return null;
    }
    const raw = sessionStorage.getItem('user');//si existe sesion lanza el metodo, sino null
    return raw ? JSON.parse(raw) : null;
  }

  private authHeaders() {
    const authorization = this.authorizationHeader;
    return new HttpHeaders(authorization ? { Authorization: authorization } : {});
  }
}
