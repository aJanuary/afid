import { describe, expect, it } from "vitest";
import {
  AfidError,
  createLongGenerator,
  createShortGenerator,
  randomLong,
  randomShort,
} from "../src/index.js";

const SHORT_RE = /^[a-z0-9]{3}-[0-9a-hjkmnp-tv-z]{5}-[0-9a-hjkmnp-tv-z]{10}$/;
const LONG_RE = /^[a-z0-9]{3}-[0-9a-hjkmnp-tv-z]{5}-[0-9a-hjkmnp-tv-z]{20}$/;

describe("randomShort", () => {
  it("produces an id matching the short shape", () => {
    const id = randomShort("res");
    expect(id).toMatch(SHORT_RE);
    expect(id).toHaveLength(20);
    expect(id.startsWith("res-")).toBe(true);
  });
});

describe("randomLong", () => {
  it("produces an id matching the long shape", () => {
    const id = randomLong("evt");
    expect(id).toMatch(LONG_RE);
    expect(id).toHaveLength(30);
    expect(id.startsWith("evt-")).toBe(true);
  });
});

describe.each([
  { name: "randomShort", fn: randomShort },
  { name: "randomLong", fn: randomLong },
])("$name prefix validation", ({ fn }) => {
  it("validates the prefix length", () => {
    expect(() => fn("ab")).toThrowError(AfidError);
    expect(() => fn("abcd")).toThrowError(AfidError);
  });

  it("validates the prefix characters", () => {
    expect(() => fn("AB1")).toThrowError(AfidError);
    expect(() => fn("a-b")).toThrowError(AfidError);
    expect(() => fn("a b")).toThrowError(AfidError);
  });

  it("attaches the right error code", () => {
    try {
      fn("ab");
      expect.fail("should have thrown");
    } catch (err) {
      expect(err).toBeInstanceOf(AfidError);
      expect((err as AfidError).code).toBe("INVALID_PREFIX_LENGTH");
    }

    try {
      fn("ABC");
      expect.fail("should have thrown");
    } catch (err) {
      expect(err).toBeInstanceOf(AfidError);
      expect((err as AfidError).code).toBe("INVALID_PREFIX_CHARS");
    }
  });
});

describe.each([
  {
    name: "createShortGenerator",
    create: createShortGenerator,
    shape: SHORT_RE,
    nRandBytes: 10,
    zeroId: "res-00000-0000000000",
  },
  {
    name: "createLongGenerator",
    create: createLongGenerator,
    shape: LONG_RE,
    nRandBytes: 20,
    zeroId: "res-00000-00000000000000000000",
  },
])("$name", ({ create, shape, nRandBytes, zeroId }) => {
  it("returns a callable that produces ids of the right shape", () => {
    const gen = create("res");
    const id = gen();
    expect(id).toMatch(shape);
  });

  it("validates the prefix once at construction time", () => {
    expect(() => create("AB")).toThrowError(AfidError);
  });

  it("works with Array.from for bulk generation", () => {
    const gen = create("res");
    const ids = Array.from({ length: 100 }, gen);
    expect(ids).toHaveLength(100);
    expect(new Set(ids).size).toBe(100);
    for (const id of ids) {
      expect(id).toMatch(shape);
    }
  });

  it("accepts an injected randomBytes source", () => {
    let calls = 0;
    const gen = create("res", {
      randomBytes: (n) => {
        calls++;
        return new Uint8Array(n).fill(0);
      },
    });
    const id = gen();
    expect(calls).toBe(1);
    expect(id).toBe(zeroId);
  });

  it("rejects wrong randomBytes length", () => {
    const gen = create("res", {
      randomBytes: (n) => new Uint8Array(n - 1),
    });
    expect(() => gen()).toThrowError(
      new RegExp(`randomBytes must return exactly ${nRandBytes} bytes`),
    );
    try {
      gen();
      expect.fail("should have thrown");
    } catch (err) {
      expect(err).toBeInstanceOf(AfidError);
      expect((err as AfidError).code).toBe("INVALID_RANDOM_BYTES");
    }
  });
});
