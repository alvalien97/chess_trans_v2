// Algunos paquetes esperan que exista global, como en Node.js; aquí se asigna globalThis
// para proporcionar ese nombre también en navegadores y otros entornos JavaScript.
type GlobalThisWithGlobal = typeof globalThis & { global?: typeof globalThis };

(globalThis as GlobalThisWithGlobal).global = globalThis;
