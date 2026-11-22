// Tipos que describen el modo de renderizado de cada ruta del servidor.
import { RenderMode, ServerRoute } from '@angular/ssr';

// Regla comodín que se aplica a cualquier ruta de la aplicación.
export const serverRoutes: ServerRoute[] = [
  {
    path: '**',
    // Genera por adelantado el HTML de la página durante la compilación.
    renderMode: RenderMode.Prerender
  }
];
