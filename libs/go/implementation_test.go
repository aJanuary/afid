// Implementation-level tests — these probe the exact bits produced by the
// Crockford base32 encoder. They are sensitive to the internal byte layout: a
// refactor of the encoder will likely break them, which is the point.

package afidgen_test

import (
	"testing"

	afidgen "github.com/ajanuary/afid/libs/go"
)

// constReader emits a single byte value forever.
type constReader byte

func (c constReader) Read(p []byte) (int, error) {
	for i := range p {
		p[i] = byte(c)
	}
	return len(p), nil
}

// fixedReader emits a fixed sequence, cycling if necessary.
type fixedReader struct {
	bytes []byte
	pos   int
}

func newFixedReader(bytes []byte) *fixedReader {
	if len(bytes) == 0 {
		panic("fixedReader needs at least one byte")
	}
	return &fixedReader{bytes: bytes}
}

func (f *fixedReader) Read(p []byte) (int, error) {
	for i := range p {
		p[i] = f.bytes[f.pos%len(f.bytes)]
		f.pos++
	}
	return len(p), nil
}

func TestAllZeroBytesProduceLowestShortID(t *testing.T) {
	g, err := afidgen.NewShort("ent", afidgen.WithRand(constReader(0x00)))
	if err != nil {
		t.Fatal(err)
	}
	if got, want := g.Generate(), "ent-00000-0000000000"; got != want {
		t.Fatalf("got %q, want %q", got, want)
	}
}

func TestAllOneBitsProduceHighestShortID(t *testing.T) {
	g, err := afidgen.NewShort("ent", afidgen.WithRand(constReader(0xFF)))
	if err != nil {
		t.Fatal(err)
	}
	if got, want := g.Generate(), "ent-zzzzz-zzzzzzzzzz"; got != want {
		t.Fatalf("got %q, want %q", got, want)
	}
}

func TestAllZeroBytesProduceLowestLongID(t *testing.T) {
	g, err := afidgen.NewLong("ent", afidgen.WithRand(constReader(0x00)))
	if err != nil {
		t.Fatal(err)
	}
	if got, want := g.Generate(), "ent-00000-00000000000000000000"; got != want {
		t.Fatalf("got %q, want %q", got, want)
	}
}

func TestAllOneBitsProduceHighestLongID(t *testing.T) {
	g, err := afidgen.NewLong("ent", afidgen.WithRand(constReader(0xFF)))
	if err != nil {
		t.Fatal(err)
	}
	if got, want := g.Generate(), "ent-zzzzz-zzzzzzzzzzzzzzzzzzzz"; got != want {
		t.Fatalf("got %q, want %q", got, want)
	}
}

func TestBulkUniquenessShort(t *testing.T) {
	g, err := afidgen.NewShort("ent")
	if err != nil {
		t.Fatal(err)
	}
	seen := make(map[string]struct{}, 1000)
	for range 1000 {
		seen[g.Generate()] = struct{}{}
	}
	if len(seen) != 1000 {
		t.Fatalf("expected 1000 unique ids, got %d", len(seen))
	}
}

func TestBulkUniquenessLong(t *testing.T) {
	g, err := afidgen.NewLong("ent")
	if err != nil {
		t.Fatal(err)
	}
	seen := make(map[string]struct{}, 1000)
	for range 1000 {
		seen[g.Generate()] = struct{}{}
	}
	if len(seen) != 1000 {
		t.Fatalf("expected 1000 unique ids, got %d", len(seen))
	}
}

// Flipping the high bit of any byte the encoder consumes for the short
// variant must change the output: all 10 bytes contribute to a used char.
func TestFlippingInputByteChangesShortID(t *testing.T) {
	base := make([]byte, 10)
	g0, err := afidgen.NewShort("ent", afidgen.WithRand(newFixedReader(base)))
	if err != nil {
		t.Fatal(err)
	}
	baseID := g0.Generate()

	for i := range 10 {
		flipped := make([]byte, 10)
		copy(flipped, base)
		flipped[i] = 0x80
		g, err := afidgen.NewShort("ent", afidgen.WithRand(newFixedReader(flipped)))
		if err != nil {
			t.Fatal(err)
		}
		if got := g.Generate(); got == baseID {
			t.Fatalf("flipping byte %d did not change the id (got %q)", i, got)
		}
	}
}

func TestAppendToMatchesGenerate(t *testing.T) {
	bytes := []byte{0xDE, 0xAD, 0xBE, 0xEF, 0x12, 0x34, 0x56, 0x78, 0x9A, 0xBC, 0xDE, 0xF0, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88}
	g1, err := afidgen.NewLong("xyz", afidgen.WithRand(newFixedReader(bytes)))
	if err != nil {
		t.Fatal(err)
	}
	g2, err := afidgen.NewLong("xyz", afidgen.WithRand(newFixedReader(bytes)))
	if err != nil {
		t.Fatal(err)
	}
	got := g1.Generate()
	buf := g2.AppendTo(nil)
	if got != string(buf) {
		t.Fatalf("Generate=%q AppendTo=%q", got, buf)
	}
}
