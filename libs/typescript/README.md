# afidgen

A TypeScript library for generating [afid ids](https://github.com/ajanuary/afid).

## Requirements

Node.js 20 or later (or any modern runtime with Web Crypto and `TextDecoder`:
Bun, Deno, browsers, edge runtimes).

## Installing

```sh
pnpm add afidgen
# or
npm install afidgen
```

## Usage

```ts
import {
  createLongGenerator,
  createShortGenerator,
  randomLong,
  randomShort,
} from "afidgen";

// Generate a short ID with the prefix "res".
const resourceId = randomShort("res");

// Generate a long ID with the prefix "evt".
const eventId = randomLong("evt");

// Build a generator. This is more efficient for generating multiple afids,
// because it only validates the prefix once when you create the generator
// — and reuses internal buffers across every call.
const gen = createLongGenerator("evt");
const eventIds = Array.from({ length: 1_000 }, gen);

// By default, generators use crypto.getRandomValues (cryptographically
// secure). Override it to supply your own source of randomness. The
// function must return exactly n random bytes.
const seeded = createShortGenerator("txi", {
  randomBytes: (n) => myRng.bytes(n),
});
const txId = seeded();
```

Invalid prefixes throw an `AfidError` with a structured `code`:

```ts
import { AfidError } from "afidgen";

try {
  randomShort("AB");
} catch (err) {
  if (err instanceof AfidError) {
    console.log(err.code); // "INVALID_PREFIX_LENGTH"
  }
}
```

## Performance

For bulk generation, build a generator once with `createShortGenerator`
or `createLongGenerator` and reuse it. The prefix is validated at
construction, so each call only pays for the randomness draw and the
encoding.

`randomShort` / `randomLong` are convenience helpers that build a fresh
generator on every call. Use them for ad-hoc IDs; reach for the factories
in tight loops.

Run benchmarks with `pnpm bench`.

## Concurrency

JavaScript runs single-threaded per realm, so a generator is implicitly
safe to share within one realm. Workers each have their own realm and
will build their own generators on first import.

## Developing

From `libs/typescript/`:

```sh
pnpm check          # lint, typecheck, tests
pnpm test           # tests only
pnpm format         # apply biome format
pnpm bench          # vitest bench
```

Initial setup: `pnpm install`.
