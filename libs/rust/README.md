# afidgen

A Rust crate for generating [afid ids](https://github.com/ajanuary/afid).

## Requirements

Rust 2024 edition (1.85 or later).

## Installing

```sh
cargo add afidgen
```

## Usage

```rust
use afidgen::{Generator, random_short, random_long, SHORT_LEN};

// Generate a short ID with the prefix "res".
let resource_id = random_short("res")?;

// Generate a long ID with the prefix "evt".
let event_id = random_long("evt")?;

// Build a generator. More efficient for bulk generation — the prefix is
// validated once at construction, not on every ID.
let mut gen = Generator::long("evt")?;
let event_ids: Vec<String> = (&mut gen).take(1_000).collect();

// Hot loop? Use generate_into to skip the String allocation: it writes
// directly into a caller-supplied buffer and returns a `&str` view.
let mut buf = [0u8; SHORT_LEN];
let mut gen = Generator::short("res")?;
for _ in 0..1_000 {
    let id: &str = gen.generate_into(&mut buf);
    // ... use ID before the next call
}

// By default, generators use a thread-local cryptographic RNG (ChaCha-based,
// batched, fast). Pass your own RNG with short_with_rng / long_with_rng —
// any `rand::Rng` implementation works.
use rand::SeedableRng;
let rng = rand::rngs::StdRng::seed_from_u64(42);
let mut gen = Generator::long_with_rng("txi", rng)?;
let tx_id = gen.generate();
# Ok::<(), afidgen::Error>(())
```

## Performance

For bulk generation, build a generator once with `Generator::short` or
`Generator::long` and reuse it. The prefix is validated at construction,
so each call only pays for the randomness draw and the encoding.

For tight hot loops, `generate_into` writes the ID directly into a
caller-supplied buffer, avoiding the per-ID `String` allocation that
`generate` performs.

`random_short` / `random_long` are convenience helpers that build a fresh
generator on every call. Use them for ad-hoc IDs; reach for the factories
in tight loops.

Run benchmarks with `cargo bench`.

## Concurrency

`Generator` methods take `&mut self`, so a single instance cannot be
shared across threads by reference. Build one generator per thread, or
guard a shared one with a mutex.

## Developing

From `libs/rust/`:

```sh
cargo test           # tests
cargo fmt --check    # format check
cargo fmt            # apply formatting
cargo clippy         # lint
cargo bench          # benchmarks
```
