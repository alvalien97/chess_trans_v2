// ApplicationConfig reúne los servicios que Angular debe preparar al iniciar.
import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection } from '@angular/core';
// Registra el cliente HTTP y el sistema de navegación de Angular.
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';

// Rutas de la aplicación; se pueden ampliar para asociar URLs con pantallas.
import { routes } from './app.routes';

// Proveedores globales disponibles para todos los componentes.
export const appConfig: ApplicationConfig = {
  providers: [
    // Envía los errores globales del navegador a los mecanismos de Angular.
    provideBrowserGlobalErrorListeners(),
    // Actualiza la interfaz sin depender de Zone.js.
    provideZonelessChangeDetection(),
    // Permite que los servicios hagan peticiones HTTP.
    provideHttpClient(),
    // Activa la navegación configurada en el archivo de rutas.
    provideRouter(routes)
  ]
};
