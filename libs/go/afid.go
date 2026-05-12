// Package afidgen generates afid ids — human-friendly random identifiers.
//
// An afid is shaped as <prefix>-<tag>-<suffix>, where the prefix is 3
// characters of [a-z0-9], the tag is 5 base32 chars of randomness, and the
// suffix is 10 (short) or 20 (long) base32 chars of randomness. The base32
// alphabet is Crockford-style: digits 0-9 followed by lowercase letters,
// omitting i, l, o, and u to avoid visual confusion.
//
// There are two flavours of afids: short and long. Which to use depends on
// your use case.
//
// Short afids are designed for scenarios where you are generating less than
// 10 thousand afids per hour. They carry 75 bits of randomness, e.g.
// "ent-2sed3-1p3dpw40ds".
//
// Long afids are designed for scenarios where you are generating between 10
// thousand and a trillion afids per hour, at the cost of longer ids. They
// carry 125 bits of randomness, e.g. "ent-d3v2s-pp2m300zxs24mspqer3s".
//
// For one-off ids use [RandomShort] or [RandomLong]. To generate many ids,
// build a [Generator] via [NewShort] or [NewLong] and reuse it — that way
// the prefix is validated a single time. [Generator.Iter] returns an
// [iter.Seq] suitable for range-over-func.
//
// See the project README for a discussion of collision rates.
package afidgen

import (
	"crypto/rand"
	"errors"
	"io"
	"iter"
	"slices"
	"sync"
)

const (
	prefixLen       = 3
	tagLen          = 5
	shortSuffixLen  = 10
	longSuffixLen   = 20
	longLen         = prefixLen + 1 + tagLen + 1 + longSuffixLen
	maxRandomBytes  = 20
	maxDecodedChars = 32
)

// Crockford-style base32 alphabet (omits i, l, o, u).
const alphabet = "0123456789abcdefghjkmnpqrstvwxyz"

// Errors returned by [NewShort], [NewLong], [RandomShort] and [RandomLong]
// when the supplied prefix is invalid. Compare with [errors.Is].
var (
	ErrInvalidPrefixLength = errors.New("prefix must be exactly 3 characters")
	ErrInvalidPrefixChars  = errors.New("prefix can only contain lowercase letters and numbers")
	ErrNilRandReader       = errors.New("random reader cannot be nil")
)

// Option configures a [Generator] at construction time.
type Option func(*config)

type config struct {
	rand io.Reader
}

// WithRand replaces the default randomness source. The reader is consumed
// with [io.ReadFull] once per id; if a [Generator.Generate] or
// [Generator.AppendTo] call cannot fill the buffer, it panics. The default
// is [crypto/rand.Reader], which never returns an error.
//
// A Generator's concurrent-use safety follows the supplied reader. The
// default reader is concurrent-safe; a bare [math/rand/v2.Rand] is not.
func WithRand(r io.Reader) Option {
	return func(c *config) { c.rand = r }
}

// Generator produces afids with a fixed prefix and variant. Construct with
// [NewShort] or [NewLong]; the prefix is validated once at construction.
//
// After construction the Generator is immutable, so concurrent use is safe
// iff the underlying [io.Reader] is. See [WithRand].
type Generator struct {
	prefix    [prefixLen]byte
	suffixLen int
	idLen     int
	nBytes    int
	rand      io.Reader
}

// NewShort builds a generator that produces short afids.
//
// Short afids are designed for scenarios where you are generating less than
// 10 thousand afids per hour. They carry 75 bits of randomness.
func NewShort(prefix string, opts ...Option) (*Generator, error) {
	return newGenerator(prefix, shortSuffixLen, opts)
}

// NewLong builds a generator that produces long afids.
//
// Long afids are designed for scenarios where you are generating between 10
// thousand and a trillion afids per hour, at the cost of longer ids. They
// carry 125 bits of randomness.
func NewLong(prefix string, opts ...Option) (*Generator, error) {
	return newGenerator(prefix, longSuffixLen, opts)
}

func newGenerator(prefix string, suffixLen int, opts []Option) (*Generator, error) {
	p, err := validatePrefix(prefix)
	if err != nil {
		return nil, err
	}
	cfg := config{rand: rand.Reader}
	for _, o := range opts {
		o(&cfg)
	}
	if cfg.rand == nil {
		return nil, ErrNilRandReader
	}
	nChars := tagLen + suffixLen
	// b32 consumes 5-byte groups producing 8 chars; pick the smallest
	// multiple-of-5 byte count that covers nChars.
	nGroups := (nChars + 7) / 8
	return &Generator{
		prefix:    p,
		suffixLen: suffixLen,
		idLen:     prefixLen + 1 + tagLen + 1 + suffixLen,
		nBytes:    nGroups * 5,
		rand:      cfg.rand,
	}, nil
}

func validatePrefix(prefix string) ([prefixLen]byte, error) {
	var out [prefixLen]byte
	if len(prefix) != prefixLen {
		return out, ErrInvalidPrefixLength
	}
	for i := range prefixLen {
		c := prefix[i]
		if !((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z')) {
			return out, ErrInvalidPrefixChars
		}
		out[i] = c
	}
	return out, nil
}

// Prefix returns the prefix this generator was constructed with.
func (g *Generator) Prefix() string {
	return string(g.prefix[:])
}

// IDLen returns the byte length of every id this generator produces.
// Useful for sizing a buffer for [Generator.AppendTo].
func (g *Generator) IDLen() int { return g.idLen }

// Generate returns a freshly generated afid.
func (g *Generator) Generate() string {
	var buf [longLen]byte
	g.write(buf[:g.idLen])
	return string(buf[:g.idLen])
}

// AppendTo appends a freshly generated afid to dst and returns the extended
// slice. The hot-path idiom for zero per-id allocation is:
//
//	buf := make([]byte, 0, g.IDLen())
//	for /* ... */ {
//	    buf = g.AppendTo(buf[:0])
//	    use(buf)
//	}
func (g *Generator) AppendTo(dst []byte) []byte {
	dst = slices.Grow(dst, g.idLen)
	start := len(dst)
	dst = dst[:start+g.idLen]
	g.write(dst[start:])
	return dst
}

// Iter returns an infinite iterator of fresh afids, for use with
// range-over-func:
//
//	for id := range g.Iter() {
//	    // ...
//	    if done { break }
//	}
func (g *Generator) Iter() iter.Seq[string] {
	return func(yield func(string) bool) {
		for yield(g.Generate()) {
		}
	}
}

// rndPool reuses random-byte buffers across calls. Passing a stack-allocated
// array through io.Reader escapes it to the heap, so we pool one to keep the
// hot path zero-alloc while leaving the Generator concurrent-safe.
var rndPool = sync.Pool{
	New: func() any { return new([maxRandomBytes]byte) },
}

func (g *Generator) write(out []byte) {
	rnd := rndPool.Get().(*[maxRandomBytes]byte)
	if _, err := io.ReadFull(g.rand, rnd[:g.nBytes]); err != nil {
		rndPool.Put(rnd)
		panic("afidgen: random source failed: " + err.Error())
	}
	var decoded [maxDecodedChars]byte
	nGroups := g.nBytes / 5
	for i := range nGroups {
		extract8(rnd, i*5, &decoded, i*8)
	}
	rndPool.Put(rnd)
	out[0] = g.prefix[0]
	out[1] = g.prefix[1]
	out[2] = g.prefix[2]
	out[3] = '-'
	copy(out[4:9], decoded[:tagLen])
	out[9] = '-'
	copy(out[10:], decoded[tagLen:tagLen+g.suffixLen])
}

// extract8 decodes 5 input bytes into 8 base32 chars using the Crockford
// alphabet.
func extract8(src *[maxRandomBytes]byte, s int, dst *[maxDecodedChars]byte, d int) {
	b0 := uint32(src[s])
	b1 := uint32(src[s+1])
	b2 := uint32(src[s+2])
	b3 := uint32(src[s+3])
	b4 := uint32(src[s+4])
	dst[d] = alphabet[(b0>>3)&0x1F]
	dst[d+1] = alphabet[((b0<<2)|(b1>>6))&0x1F]
	dst[d+2] = alphabet[(b1>>1)&0x1F]
	dst[d+3] = alphabet[((b1<<4)|(b2>>4))&0x1F]
	dst[d+4] = alphabet[((b2<<1)|(b3>>7))&0x1F]
	dst[d+5] = alphabet[(b3>>2)&0x1F]
	dst[d+6] = alphabet[((b3<<3)|(b4>>5))&0x1F]
	dst[d+7] = alphabet[b4&0x1F]
}

// RandomShort returns a short afid with the given prefix.
//
// Short afids are designed for scenarios where you are generating less than
// 10 thousand afids per hour. They carry 75 bits of randomness.
//
// For bulk generation, build a [Generator] via [NewShort] once so the
// prefix is validated only once.
func RandomShort(prefix string) (string, error) {
	g, err := NewShort(prefix)
	if err != nil {
		return "", err
	}
	return g.Generate(), nil
}

// RandomLong returns a long afid with the given prefix.
//
// Long afids are designed for scenarios where you are generating between 10
// thousand and a trillion afids per hour, at the cost of longer ids. They
// carry 125 bits of randomness.
//
// For bulk generation, build a [Generator] via [NewLong] once so the
// prefix is validated only once.
func RandomLong(prefix string) (string, error) {
	g, err := NewLong(prefix)
	if err != nil {
		return "", err
	}
	return g.Generate(), nil
}
