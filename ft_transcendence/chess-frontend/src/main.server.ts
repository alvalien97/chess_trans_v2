// Herramienta para arrancar Angular en el servidor durante el renderizado.
import { BootstrapContext, bootstrapApplication } from '@angular/platform-browser';
// Se renderiza la misma aplicación que se muestra en el navegador.
import { App } from './app/app';
// Incluye los proveedores comunes y los específicos del servidor.
import { config } from './app/app.config.server';

// Función que inicializa la aplicación con el contexto de la petición del servidor.
const bootstrap = (context: BootstrapContext) =>
    bootstrapApplication(App, config, context);

// Angular SSR busca esta función como punto de entrada para representar páginas.
export default bootstrap;
