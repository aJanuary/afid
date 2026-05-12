package afidgen_test

import (
	"strings"
	"testing"

	afidgen "github.com/ajanuary/afid/libs/go"
)

// padBytes returns a non-empty byte slice that the fuzz target can feed to
// the fixed reader. Fuzz inputs may be empty; we wrap them to a sentinel.
func padBytes(b []byte) []byte {
	if len(b) == 0 {
		return []byte{0}
	}
	return b
}

func checkShape(t *testing.T, prefix, id string, suffixLen int) {
	t.Helper()
	if len(id) != 3+1+5+1+suffixLen {
		t.Fatalf("length: got %d, want %d, id=%q", len(id), 3+1+5+1+suffixLen, id)
	}
	if id[:3] != prefix {
		t.Fatalf("prefix: got %q, want %q", id[:3], prefix)
	}
	if id[3] != '-' || id[9] != '-' {
		t.Fatalf("missing separators: %q", id)
	}
	for i := 4; i < len(id); i++ {
		if i == 9 {
			continue
		}
		if strings.IndexByte(alphabet, id[i]) < 0 {
			t.Fatalf("char %q at %d not in Crockford alphabet: %q", id[i], i, id)
		}
	}
}

func FuzzShortShape(f *testing.F) {
	f.Add([]byte{0x00})
	f.Add([]byte{0xFF})
	f.Add([]byte{0xDE, 0xAD, 0xBE, 0xEF})
	f.Fuzz(func(t *testing.T, seed []byte) {
		g, err := afidgen.NewShort("ent", afidgen.WithRand(newFixedReader(padBytes(seed))))
		if err != nil {
			t.Fatal(err)
		}
		for range 10 {
			checkShape(t, "ent", g.Generate(), 10)
		}
	})
}

func FuzzLongShape(f *testing.F) {
	f.Add([]byte{0x00})
	f.Add([]byte{0xFF})
	f.Add([]byte{0xDE, 0xAD, 0xBE, 0xEF})
	f.Fuzz(func(t *testing.T, seed []byte) {
		g, err := afidgen.NewLong("ent", afidgen.WithRand(newFixedReader(padBytes(seed))))
		if err != nil {
			t.Fatal(err)
		}
		for range 10 {
			checkShape(t, "ent", g.Generate(), 20)
		}
	})
}

func FuzzGenerateMatchesAppendTo(f *testing.F) {
	f.Add([]byte{0x00})
	f.Add([]byte{0xFF})
	f.Add([]byte{0xDE, 0xAD, 0xBE, 0xEF})
	f.Fuzz(func(t *testing.T, seed []byte) {
		padded := padBytes(seed)
		g1, err := afidgen.NewLong("xyz", afidgen.WithRand(newFixedReader(padded)))
		if err != nil {
			t.Fatal(err)
		}
		g2, err := afidgen.NewLong("xyz", afidgen.WithRand(newFixedReader(padded)))
		if err != nil {
			t.Fatal(err)
		}
		got := g1.Generate()
		buf := g2.AppendTo(nil)
		if got != string(buf) {
			t.Fatalf("Generate=%q AppendTo=%q", got, buf)
		}
	})
}
