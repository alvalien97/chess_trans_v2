import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideZonelessChangeDetection } from '@angular/core';
import { BoardComponent } from './board.component';

describe('BoardComponent', () => {
  let component: BoardComponent;
  let fixture: ComponentFixture<BoardComponent>;

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

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('labels squares with chess coordinates', () => {
    expect(component.getSquareLabel(0, 0)).toBe('a8, torre negras');
  });

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
