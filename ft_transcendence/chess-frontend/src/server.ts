// Motor de Angular que procesa peticiones y crea el HTML inicial en el servidor.
import {
  AngularNodeAppEngine,
  createNodeRequestHandler,
  isMainModule,
  writeResponseToNodeResponse,
} from '@angular/ssr/node';
// Express recibe las peticiones HTTP que llegan a este servidor de renderizado.
import express from 'express';
// join construye rutas de archivos válidas en el sistema operativo actual.
import { join } from 'node:path';

// Carpeta donde se compilaron los archivos que el navegador descarga.
const browserDistFolder = join(import.meta.dirname, '../browser');

// Crea el servidor web de Express.
const app = express();
// Adaptador que ejecuta Angular para generar una respuesta HTML desde el servidor.
const angularApp = new AngularNodeAppEngine();

// Los endpoints propios de Express podrían definirse aquí; la API de la aplicación
// está en el backend Spring y no en este servidor de renderizado.

// Entrega directamente imágenes, estilos y otros archivos estáticos compilados.
app.use(
  express.static(browserDistFolder, {
    maxAge: '1y', // Permite al navegador conservar estos archivos durante un año.
    index: false, // Deja que Angular gestione la página principal.
    redirect: false, // No redirige automáticamente si la ruta apunta a una carpeta.
  }),
);

// Para el resto de rutas, Angular prepara el HTML inicial que recibirá el navegador.
app.use((req, res, next) => {
  angularApp
    .handle(req)
    .then((response) =>
      // Si Angular generó una respuesta, se la devuelve al navegador; si no, sigue Express.
      {
        if (response) {
          writeResponseToNodeResponse(response, res);
        } else {
          next();
        }
      },
    )
    // Pasa los errores al manejador de errores de Express.
    .catch(next);
});

// Arranca el servidor cuando este archivo es el punto de entrada o está gestionado por PM2.
if (isMainModule(import.meta.url) || process.env['pm_id']) {
  // Usa el puerto de entorno PORT o el 4000 por defecto.
  const port = process.env['PORT'] || 4000;
  app.listen(port, (error) => {
    if (error) {
      // Un error al abrir el puerto impide arrancar el servidor.
      throw error;
    }

    // Indica en consola la dirección donde está escuchando.
    console.log(`Node Express server listening on http://localhost:${port}`);
  });
}

// Permite que Angular CLI u otra plataforma reutilice Express como manejador de peticiones.
export const reqHandler = createNodeRequestHandler(app);
