package afidgen_test

import (
	"errors"
	"strings"
	"testing"

	afidgen "github.com/aJanuary/afid/libs/go"
)

const alphabet = "0123456789abcdefghjkmnpqrstvwxyz"

func isAlphabetChar(c byte) bool {
	return strings.IndexByte(alphabet, c) >= 0
}

func assertShortShape(t *testing.T, prefix, id string) {
	t.Helper()
	assertShape(t, prefix, id, 10)
}

func assertLongShape(t *testing.T, prefix, id string) {
	t.Helper()
	assertShape(t, prefix, id, 20)
}

func assertShape(t *testing.T, prefix, id string, suffixLen int) {
	t.Helper()
	wantLen := 3 + 1 + 5 + 1 + suffixLen
	if len(id) != wantLen {
		t.Fatalf("wrong total length: got %d, want %d (id=%q)", len(id), wantLen, id)
	}
	parts := strings.Split(id, "-")
	if len(parts) != 3 {
		t.Fatalf("expected three '-' separated parts: %q", id)
	}
	if parts[0] != prefix {
		t.Fatalf("prefix mismatch: got %q, want %q", parts[0], prefix)
	}
	if len(parts[1]) != 5 {
		t.Fatalf("tag length: got %d, want 5", len(parts[1]))
	}
	if len(parts[2]) != suffixLen {
		t.Fatalf("suffix length: got %d, want %d", len(parts[2]), suffixLen)
	}
	for i := 0; i < len(parts[1]); i++ {
		if !isAlphabetChar(parts[1][i]) {
			t.Fatalf("tag char %q not in Crockford alphabet: %q", parts[1][i], id)
		}
	}
	for i := 0; i < len(parts[2]); i++ {
		if !isAlphabetChar(parts[2][i]) {
			t.Fatalf("suffix char %q not in Crockford alphabet: %q", parts[2][i], id)
		}
	}
}

func TestRandomShortShape(t *testing.T) {
	id, err := afidgen.RandomShort("ent")
	if err != nil {
		t.Fatal(err)
	}
	assertShortShape(t, "ent", id)
	if len(id) != 20 {
		t.Fatalf("short length: got %d, want 20", len(id))
	}
}

func TestRandomLongShape(t *testing.T) {
	id, err := afidgen.RandomLong("ent")
	if err != nil {
		t.Fatal(err)
	}
	assertLongShape(t, "ent", id)
	if len(id) != 30 {
		t.Fatalf("long length: got %d, want 30", len(id))
	}
}

func TestShortGeneratorGenerate(t *testing.T) {
	g, err := afidgen.NewShort("usr")
	if err != nil {
		t.Fatal(err)
	}
	for range 50 {
		assertShortShape(t, "usr", g.Generate())
	}
}

func TestLongGeneratorGenerate(t *testing.T) {
	g, err := afidgen.NewLong("usr")
	if err != nil {
		t.Fatal(err)
	}
	for range 50 {
		assertLongShape(t, "usr", g.Generate())
	}
}

func TestGeneratorIter(t *testing.T) {
	g, err := afidgen.NewShort("xyz")
	if err != nil {
		t.Fatal(err)
	}
	var ids []string
	for id := range g.Iter() {
		ids = append(ids, id)
		if len(ids) == 5 {
			break
		}
	}
	if len(ids) != 5 {
		t.Fatalf("expected 5 ids, got %d", len(ids))
	}
	for _, id := range ids {
		assertShortShape(t, "xyz", id)
	}
}

func TestAppendToWritesToBuffer(t *testing.T) {
	g, err := afidgen.NewShort("ent")
	if err != nil {
		t.Fatal(err)
	}
	buf := make([]byte, 0, g.IDLen())
	buf = g.AppendTo(buf)
	if len(buf) != g.IDLen() {
		t.Fatalf("length: got %d, want %d", len(buf), g.IDLen())
	}
	assertShortShape(t, "ent", string(buf))
}

func TestAppendToPreservesExistingBytes(t *testing.T) {
	g, err := afidgen.NewLong("ent")
	if err != nil {
		t.Fatal(err)
	}
	buf := []byte("prefix:")
	buf = g.AppendTo(buf)
	if !strings.HasPrefix(string(buf), "prefix:") {
		t.Fatalf("AppendTo clobbered existing bytes: %q", buf)
	}
	assertLongShape(t, "ent", string(buf[len("prefix:"):]))
}

func TestAppendToZeroAllocOnReusedBuffer(t *testing.T) {
	g, err := afidgen.NewLong("ent")
	if err != nil {
		t.Fatal(err)
	}
	buf := make([]byte, 0, g.IDLen())
	// Warm allocations (one-time growth of buf if any).
	buf = g.AppendTo(buf[:0])
	allocs := testing.AllocsPerRun(100, func() {
		buf = g.AppendTo(buf[:0])
	})
	if allocs != 0 {
		t.Fatalf("AppendTo should be zero-alloc on a sized buffer; got %v allocs/op", allocs)
	}
}

func TestPrefixGetter(t *testing.T) {
	g, err := afidgen.NewShort("a0z")
	if err != nil {
		t.Fatal(err)
	}
	if g.Prefix() != "a0z" {
		t.Fatalf("Prefix: got %q, want %q", g.Prefix(), "a0z")
	}
}

func TestIDLen(t *testing.T) {
	gs, err := afidgen.NewShort("ent")
	if err != nil {
		t.Fatal(err)
	}
	if gs.IDLen() != 20 {
		t.Fatalf("short IDLen: got %d, want 20", gs.IDLen())
	}
	gl, err := afidgen.NewLong("ent")
	if err != nil {
		t.Fatal(err)
	}
	if gl.IDLen() != 30 {
		t.Fatalf("long IDLen: got %d, want 30", gl.IDLen())
	}
}

func TestPrefixTooShortRejected(t *testing.T) {
	if _, err := afidgen.RandomShort("ab"); !errors.Is(err, afidgen.ErrInvalidPrefixLength) {
		t.Fatalf("got %v, want ErrInvalidPrefixLength", err)
	}
	if _, err := afidgen.NewShort("ab"); !errors.Is(err, afidgen.ErrInvalidPrefixLength) {
		t.Fatalf("got %v, want ErrInvalidPrefixLength", err)
	}
}

func TestPrefixTooLongRejected(t *testing.T) {
	if _, err := afidgen.RandomShort("abcd"); !errors.Is(err, afidgen.ErrInvalidPrefixLength) {
		t.Fatalf("got %v, want ErrInvalidPrefixLength", err)
	}
}

func TestEmptyPrefixRejected(t *testing.T) {
	if _, err := afidgen.RandomShort(""); !errors.Is(err, afidgen.ErrInvalidPrefixLength) {
		t.Fatalf("got %v, want ErrInvalidPrefixLength", err)
	}
}

func TestUppercasePrefixRejected(t *testing.T) {
	if _, err := afidgen.RandomShort("ABC"); !errors.Is(err, afidgen.ErrInvalidPrefixChars) {
		t.Fatalf("got %v, want ErrInvalidPrefixChars", err)
	}
	if _, err := afidgen.RandomLong("aBc"); !errors.Is(err, afidgen.ErrInvalidPrefixChars) {
		t.Fatalf("got %v, want ErrInvalidPrefixChars", err)
	}
}

func TestPunctuationInPrefixRejected(t *testing.T) {
	cases := []string{"a-b", "a b", "a.b"}
	for _, c := range cases {
		if _, err := afidgen.RandomShort(c); !errors.Is(err, afidgen.ErrInvalidPrefixChars) {
			t.Fatalf("prefix %q: got %v, want ErrInvalidPrefixChars", c, err)
		}
	}
}

func TestAllDigitsPrefixAllowed(t *testing.T) {
	id, err := afidgen.RandomShort("000")
	if err != nil {
		t.Fatal(err)
	}
	assertShortShape(t, "000", id)
}

func TestAllLettersPrefixAllowed(t *testing.T) {
	id, err := afidgen.RandomLong("abc")
	if err != nil {
		t.Fatal(err)
	}
	assertLongShape(t, "abc", id)
}

func TestErrorMessages(t *testing.T) {
	if got, want := afidgen.ErrInvalidPrefixLength.Error(), "prefix must be exactly 3 characters"; got != want {
		t.Fatalf("got %q, want %q", got, want)
	}
	if got, want := afidgen.ErrInvalidPrefixChars.Error(), "prefix can only contain lowercase letters and numbers"; got != want {
		t.Fatalf("got %q, want %q", got, want)
	}
	if got, want := afidgen.ErrNilRandReader.Error(), "random reader cannot be nil"; got != want {
		t.Fatalf("got %q, want %q", got, want)
	}
}

func TestNilRandReaderRejected(t *testing.T) {
	if _, err := afidgen.NewShort("ent", afidgen.WithRand(nil)); !errors.Is(err, afidgen.ErrNilRandReader) {
		t.Fatalf("got %v, want ErrNilRandReader", err)
	}
	if _, err := afidgen.NewLong("ent", afidgen.WithRand(nil)); !errors.Is(err, afidgen.ErrNilRandReader) {
		t.Fatalf("got %v, want ErrNilRandReader", err)
	}
}
