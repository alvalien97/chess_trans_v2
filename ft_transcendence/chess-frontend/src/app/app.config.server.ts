// Combina proveedores de Angular para ejecutar la aplicación también en el servidor.
import { mergeApplicationConfig, ApplicationConfig } from '@angular/core';
// Activa el renderizado del servidor y aplica sus reglas de rutas.
import { provideServerRendering, withRoutes } from '@angular/ssr';
// Se reutiliza la configuración común de la versión de navegador.
import { appConfig } from './app.config';
// Reglas que determinan cómo preparar las páginas en el servidor.
import { serverRoutes } from './app.routes.server';

// Proveedores exclusivos del proceso de renderizado de servidor.
const serverConfig: ApplicationConfig = {
  providers: [
    provideServerRendering(withRoutes(serverRoutes))
  ]
};

// Une la configuración general con las funciones específicas del servidor.
export const config = mergeApplicationConfig(appConfig, serverConfig);
