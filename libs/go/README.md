# afidgen

A Go package for generating [afid ids](https://github.com/ajanuary/afid).

## Requirements

Go 1.25.0 or later.

## Installing

```sh
go get github.com/ajanuary/afid/libs/go
```

Import as `afidgen` — the package name differs from the last path element, so
use an explicit alias:

```go
import afidgen "github.com/ajanuary/afid/libs/go"
```

## Usage

```go
import (
    "fmt"

    afidgen "github.com/ajanuary/afid/libs/go"
)

// One-off IDs.
resID, err := afidgen.RandomShort("res")
evtID, err := afidgen.RandomLong("evt")

// Bulk generation — build a generator once so the prefix is validated up
// front, not on every ID.
g, err := afidgen.NewLong("evt")
if err != nil {
    // ...
}
for range 1_000 {
    fmt.Println(g.Generate())
}

// Range-over-func iteration.
for id := range g.Iter() {
    fmt.Println(id)
    break
}

// Hot loop? Use AppendTo to skip the per-ID string allocation. It writes
// directly into a caller-supplied buffer.
buf := make([]byte, 0, g.IDLen())
for range 1_000 {
    buf = g.AppendTo(buf[:0])
    // use(buf) as []byte before the next call
}

// Swap in a different randomness source (default is crypto/rand.Reader).
import "math/rand/v2"
chacha := rand.NewChaCha8([32]byte{ /* seed */ })
g, err = afidgen.NewLong("txi", afidgen.WithRand(chacha))
```

## Performance

For bulk generation, build a generator once with `NewShort` or `NewLong`
and reuse it. The prefix is validated at construction, so each call only
pays for the randomness draw and the encoding.

For tight hot loops, `AppendTo` writes the ID directly into a
caller-supplied buffer, avoiding the per-ID `string` allocation that
`Generate` performs.

`RandomShort` / `RandomLong` are convenience helpers that build a fresh
generator on every call. Use them for ad-hoc IDs; reach for the factories
in tight loops.

Run benchmarks with `go test -bench . -benchmem ./...`.

## Concurrency

`Generator` is safe for concurrent use when the underlying `io.Reader` is.
The default (`crypto/rand.Reader`) is concurrent-safe. A bare
`*math/rand/v2.Rand` is not; wrap it or use one per goroutine.

## Developing

From `libs/go/`:

```sh
go test ./...                     # tests
gofmt -l .                        # format check
go fmt ./...                      # apply formatting
go vet ./...                      # vet
go tool staticcheck ./...         # static analysis
go test -bench . -benchmem ./...  # benchmarks
```
