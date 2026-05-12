# afidgen

A Python library for generating [afid ids](https://github.com/ajanuary/afid).

## Requirements

Python 3.9 or later.

## Installing
```sh
pip install afidgen
```

## Usage

```python
import afidgen

# Generate a short ID with the prefix "res".
resource_id = afidgen.random_short("res")

# Generate a long ID with the prefix "evt".
event_id = afidgen.random_long("evt")

# Create a generator. This is more efficient for generating multiple afids,
# because it only needs to validate the prefix once when you create the
# generator, instead of every time you generate an ID.
gen = afidgen.Generator.long("evt")
event_ids = [gen() for _ in range(1_000)]

# Generator is also iterable.
import itertools
more_event_ids = list(itertools.islice(gen, 1_000))

# By default, generators use secrets.token_bytes (cryptographically secure).
# You can override it when creating a generator to provide your own source of
# randomness — for example, random.randbytes if you need reproducibility or
# don't need security. The function must take a number of bytes and return
# that many random bytes.
import random
gen = afidgen.Generator.long("txi", randbytes=random.randbytes)
tx_id = gen()
```

## Performance

For bulk generation, build a generator once with `Generator.short` or
`Generator.long` and reuse it. The prefix is validated at construction,
so each call only pays for the randomness draw and the encoding.

`random_short` / `random_long` are convenience helpers that build a fresh
generator on every call. Use them for ad-hoc IDs; reach for the factories
in tight loops.

Run benchmarks with `uv run pytest bench/`.

## Concurrency

The default randomness source (`secrets.token_bytes`) is thread-safe, so
a single `Generator` can be called from multiple threads. If you pass a
custom `randbytes` callable, concurrent safety follows that callable.

## Developing

From `libs/python/`:

```sh
uv run pytest                  # tests
uv run ruff format --check .   # format check
uv run ruff format .           # apply formatting
uv run ruff check .            # lint
uv run ty check                # typecheck
uv run pytest bench/           # benchmarks
```

Initial setup: `uv sync`.
