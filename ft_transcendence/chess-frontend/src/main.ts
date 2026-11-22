// Arranca Angular en el navegador usando el renderizado estándar del elemento raíz.
import { bootstrapApplication } from '@angular/platform-browser';
// Configuración global: servicios, rutas y otras dependencias de la aplicación.
import { appConfig } from './app/app.config';
// Componente principal que contiene la estructura general de la web.
import { App } from './app/app';

// Crea la aplicación; si el arranque falla, muestra el error en la consola.
bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
