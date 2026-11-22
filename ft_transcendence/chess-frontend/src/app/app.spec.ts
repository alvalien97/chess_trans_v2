// Configuración del mismo modo reactivo que usa la aplicación.
import { provideZonelessChangeDetection } from '@angular/core';
// Permite que los servicios HTTP funcionen también durante las pruebas.
import { provideHttpClient } from '@angular/common/http';
// Herramientas de Angular para crear componentes de prueba y respuestas HTTP simuladas.
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
// Componente y servicios que se necesitan para probar la aplicación principal.
import { App } from './app';
import { AuthService } from './services/auth';
import { PresenceWebsocketService } from './services/presence-websocket';

// Agrupa las pruebas relacionadas con el componente principal.
describe('App', () => {
  // Prepara un entorno aislado antes de cada prueba.
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideZonelessChangeDetection(), provideHttpClient()]
    }).compileComponents();
  });

  // Comprueba que Angular puede construir la aplicación.
  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  // Comprueba que se pueden mantener varias partidas abiertas sin duplicados.
  it('keeps multiple games open without duplicating the same game', () => {
    const app = TestBed.createComponent(App).componentInstance;
    app.selectMatch({ gameId: 'game-1', color: 0, opponent: 'player-a' });
    app.selectMatch({ gameId: 'game-2', color: 1, opponent: 'player-b' });
    app.selectMatch({ gameId: 'game-1', color: 0, opponent: 'player-a' });

    expect(app.activeMatches().map((match) => match.gameId)).toEqual(['game-1', 'game-2']);
    expect(app.sectionTitle()).toBe('Game');
  });

  // Comprueba que los documentos legales se abren y se puede cambiar de uno a otro.
  it('opens the privacy and terms pages from the logged-out screen', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const privacyLink = fixture.nativeElement.querySelector('[data-legal="privacy"]') as HTMLButtonElement;
    privacyLink.click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Esta política explica qué información utiliza Transcendence Chess');

    const termsTab = fixture.nativeElement.querySelector('.legal-nav button[aria-current="page"] + button') as HTMLButtonElement;
    termsTab.click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Términos de servicio');
    expect(fixture.nativeElement.textContent).toContain('No hay premios ni valor monetario');
  });

  // Comprueba que un perfil público se muestra cuando el usuario tiene sesión iniciada.
  it('opens a public profile from the authenticated app', () => {
    const fixture = TestBed.createComponent(App);
    const auth = TestBed.inject(AuthService);
    // Simula respuestas del backend para que la prueba no necesite una conexión real.
    spyOn(auth, 'getFriends').and.returnValue(of([]));
    spyOn(auth, 'getPublicProfile').and.returnValue(of({
      id: 42,
      username: 'opponent',
      avatarUrl: null,
      joinedAt: '2026-01-01T00:00:00Z',
      online: true
    }));
    // Evita abrir un WebSocket real durante la prueba.
    spyOn(TestBed.inject(PresenceWebsocketService), 'connect').and.stub();
    auth.user.set({ id: 7, username: 'player', email: 'player@example.com' });
    fixture.detectChanges();

    fixture.componentInstance.openProfile(42);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('opponent');
    expect(fixture.nativeElement.textContent).toContain('Conectado ahora');
    expect(fixture.nativeElement.textContent).toContain('Miembro desde');
  });
});
