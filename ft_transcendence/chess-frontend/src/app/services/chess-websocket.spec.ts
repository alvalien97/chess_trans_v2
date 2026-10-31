import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideZonelessChangeDetection } from '@angular/core';
import { ChessWebsocketService } from './chess-websocket';

describe('ChessWebsocketService', () => {
  let service: ChessWebsocketService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient()]
    });
    service = TestBed.inject(ChessWebsocketService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('does not claim a move was sent while disconnected', () => {
    expect(service.sendMove('game-1', { fromCol: 4, fromRow: 6, toCol: 4, toRow: 4 })).toBeFalse();
  });
});
