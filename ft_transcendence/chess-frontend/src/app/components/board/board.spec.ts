// Herramientas de Angular para probar componentes y simular sus dependencias.
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideZonelessChangeDetection } from '@angular/core';
// Componente del tablero que se va a probar.
import { BoardComponent } from './board.component';

// Agrupa pruebas sobre el comportamiento del tablero de ajedrez.
describe('BoardComponent', () => {
  // Referencia al componente y al contenedor de prueba de Angular.
  let component: BoardComponent;
  let fixture: ComponentFixture<BoardComponent>;

  // Crea un tablero nuevo antes de cada prueba.
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BoardComponent],
      providers: [provideHttpClient(), provideZonelessChangeDetection()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BoardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // Comprueba que el tablero se puede crear correctamente.
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // Comprueba que una casilla se describe usando coordenadas y pieza comprensibles.
  it('labels squares with chess coordinates', () => {
    expect(component.getSquareLabel(0, 0)).toBe('a8, torre negras');
  });

  it('rotates the visible rows and columns for the black player', () => {
    component.playerColor.set(1);
    component.queueState.set('MATCHED');
    fixture.detectChanges();

    const squares = fixture.nativeElement.querySelectorAll('.square') as NodeListOf<HTMLButtonElement>;
    expect(squares[0].getAttribute('aria-label')).toBe('h1, torre blancas');
    expect(squares[7].getAttribute('aria-label')).toBe('a1, torre blancas');
    expect(squares[56].getAttribute('aria-label')).toBe('h8, torre negras');
  });

  // Comprueba que las flechas mueven el foco por las casillas del tablero.
  it('moves keyboard focus between squares with arrow keys', () => {
    component.queueState.set('MATCHED');
    fixture.detectChanges();
    const squares = fixture.nativeElement.querySelectorAll('.square') as NodeListOf<HTMLButtonElement>;
    const event = new KeyboardEvent('keydown', { key: 'ArrowRight', cancelable: true });

    component.onSquareKeydown(event, 0, 0);
    fixture.detectChanges();

    expect(event.defaultPrevented).toBeTrue();
    expect(component.focusedSquare()).toEqual({ col: 1, row: 0 });
    expect(squares[1].tabIndex).toBe(0);
    expect(document.activeElement).toBe(squares[1]);
  });

  it('moves keyboard focus in the visible direction when the board is rotated', () => {
    component.playerColor.set(1);
    component.queueState.set('MATCHED');
    fixture.detectChanges();
    const squares = fixture.nativeElement.querySelectorAll('.square') as NodeListOf<HTMLButtonElement>;
    const event = new KeyboardEvent('keydown', { key: 'ArrowRight', cancelable: true });

    component.onSquareKeydown(event, 7, 7);
    fixture.detectChanges();

    expect(event.defaultPrevented).toBeTrue();
    expect(component.focusedSquare()).toEqual({ col: 6, row: 7 });
    expect(squares[1].tabIndex).toBe(0);
    expect(document.activeElement).toBe(squares[1]);
  });

  // Comprueba que durante una partida aparecen las acciones de tablas y rendición.
  it('shows draw and resignation controls during a match', () => {
    component.gameId.set('game-1');
    component.playerColor.set(0);
    component.queueState.set('MATCHED');
    fixture.detectChanges();

    const buttons = Array.from(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>)
      .map((button) => button.textContent?.trim());
    expect(buttons).toContain('Ofrecer tablas');
    expect(buttons).toContain('Rendirse');

    component.drawOfferByUserId.set(22);
    fixture.detectChanges();
    const responseButtons = Array.from(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>)
      .map((button) => button.textContent?.trim());
    expect(responseButtons).toContain('Aceptar tablas');
    expect(responseButtons).toContain('Rechazar');
  });
});
