// Herramientas de Angular para construir servicios dentro de un entorno de prueba.
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideZonelessChangeDetection } from '@angular/core';
// Servicio de tiempo real que se va a verificar.
import { ChessWebsocketService } from './chess-websocket';

// Agrupa las pruebas del servicio de conexión con el backend.
describe('ChessWebsocketService', () => {
  // Referencia al servicio que crea Angular para estas pruebas.
  let service: ChessWebsocketService;

  // Prepara las dependencias necesarias antes de cada prueba.
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient()]
    });
    service = TestBed.inject(ChessWebsocketService);
  });

  // Confirma que Angular puede crear el servicio.
  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // Sin conexión, el servicio debe indicar que no pudo enviar el movimiento.
  it('does not claim a move was sent while disconnected', () => {
    expect(service.sendMove('game-1', { fromCol: 4, fromRow: 6, toCol: 4, toRow: 4 })).toBeFalse();
  });
});
