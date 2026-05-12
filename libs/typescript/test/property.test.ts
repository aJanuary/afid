import fc from "fast-check";
import { describe, expect, it } from "vitest";
import { createLongGenerator, createShortGenerator } from "../src/index.js";

const PREFIX_ARB = fc
  .stringMatching(/^[a-z0-9]{3}$/)
  .filter((s) => s.length === 3);

const SHORT_RE = /^[a-z0-9]{3}-[0-9a-hjkmnp-tv-z]{5}-[0-9a-hjkmnp-tv-z]{10}$/;
const LONG_RE = /^[a-z0-9]{3}-[0-9a-hjkmnp-tv-z]{5}-[0-9a-hjkmnp-tv-z]{20}$/;

describe("property: shape", () => {
  it("any valid prefix produces a well-formed short id", () => {
    fc.assert(
      fc.property(PREFIX_ARB, (prefix) => {
        const id = createShortGenerator(prefix)();
        expect(id).toMatch(SHORT_RE);
        expect(id.slice(0, 3)).toBe(prefix);
      }),
    );
  });

  it("any valid prefix produces a well-formed long id", () => {
    fc.assert(
      fc.property(PREFIX_ARB, (prefix) => {
        const id = createLongGenerator(prefix)();
        expect(id).toMatch(LONG_RE);
        expect(id.slice(0, 3)).toBe(prefix);
      }),
    );
  });
});

// Short consumes 75 bits: 9 full bytes + top 3 bits of byte 9. The bottom 5
// bits of byte 9 are discarded, so this arbitrary zeroes them to keep the
// input space canonical — distinct inputs then map to distinct ids.
const SHORT_CANONICAL_BYTES = fc
  .tuple(
    fc.uint8Array({ minLength: 9, maxLength: 9 }),
    fc.integer({ min: 0, max: 7 }),
  )
  .map(([head, top3]) => {
    const out = new Uint8Array(10);
    out.set(head);
    out[9] = top3 << 5;
    return out;
  });

describe("property: encoding", () => {
  it("distinct random byte streams yield distinct ids", () => {
    fc.assert(
      fc.property(
        SHORT_CANONICAL_BYTES,
        SHORT_CANONICAL_BYTES,
        (bytesA, bytesB) => {
          fc.pre(!bytesEqual(bytesA, bytesB));
          const idA = createShortGenerator("res", {
            randomBytes: () => bytesA,
          })();
          const idB = createShortGenerator("res", {
            randomBytes: () => bytesB,
          })();
          expect(idA).not.toBe(idB);
        },
      ),
    );
  });

  it("identical random byte streams yield identical ids", () => {
    fc.assert(
      fc.property(fc.uint8Array({ minLength: 20, maxLength: 20 }), (bytes) => {
        const fill = (): Uint8Array => bytes;
        const idA = createLongGenerator("evt", { randomBytes: fill })();
        const idB = createLongGenerator("evt", { randomBytes: fill })();
        expect(idA).toBe(idB);
      }),
    );
  });
});

function bytesEqual(a: Uint8Array, b: Uint8Array): boolean {
  if (a.length !== b.length) {
    return false;
  }
  for (let i = 0; i < a.length; i++) {
    if (a[i] !== b[i]) {
      return false;
    }
  }
  return true;
}
