type GlobalThisWithGlobal = typeof globalThis & { global?: typeof globalThis };

(globalThis as GlobalThisWithGlobal).global = globalThis;
