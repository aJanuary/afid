package afidgen_test

import (
	"testing"

	afidgen "github.com/ajanuary/afid/libs/go"
	"github.com/google/uuid"
)

func BenchmarkGenerateShort(b *testing.B) {
	g, err := afidgen.NewShort("ent")
	if err != nil {
		b.Fatal(err)
	}
	b.ResetTimer()
	for b.Loop() {
		_ = g.Generate()
	}
}

func BenchmarkGenerateLong(b *testing.B) {
	g, err := afidgen.NewLong("ent")
	if err != nil {
		b.Fatal(err)
	}
	b.ResetTimer()
	for b.Loop() {
		_ = g.Generate()
	}
}

func BenchmarkAppendToShort(b *testing.B) {
	g, err := afidgen.NewShort("ent")
	if err != nil {
		b.Fatal(err)
	}
	buf := make([]byte, 0, g.IDLen())
	b.ResetTimer()
	for b.Loop() {
		buf = g.AppendTo(buf[:0])
	}
}

func BenchmarkAppendToLong(b *testing.B) {
	g, err := afidgen.NewLong("ent")
	if err != nil {
		b.Fatal(err)
	}
	buf := make([]byte, 0, g.IDLen())
	b.ResetTimer()
	for b.Loop() {
		buf = g.AppendTo(buf[:0])
	}
}

func BenchmarkRandomShort(b *testing.B) {
	for b.Loop() {
		if _, err := afidgen.RandomShort("ent"); err != nil {
			b.Fatal(err)
		}
	}
}

func BenchmarkRandomLong(b *testing.B) {
	for b.Loop() {
		if _, err := afidgen.RandomLong("ent"); err != nil {
			b.Fatal(err)
		}
	}
}

// Baseline: github.com/google/uuid — the most common Go UUIDv4 library
// (Go's stdlib does not include a UUID type).
func BenchmarkUUIDv4(b *testing.B) {
	for b.Loop() {
		_ = uuid.NewString()
	}
}
