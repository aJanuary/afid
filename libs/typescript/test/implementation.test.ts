import { describe, expect, it } from "vitest";
import { createLongGenerator, createShortGenerator } from "../src/index.js";

describe("boundary values", () => {
  it("encodes all-zero bytes as the lowest character (short)", () => {
    const gen = createShortGenerator("ent", {
      randomBytes: (n) => new Uint8Array(n).fill(0),
    });
    expect(gen()).toBe("ent-00000-0000000000");
  });

  it("encodes all-zero bytes as the lowest character (long)", () => {
    const gen = createLongGenerator("ent", {
      randomBytes: (n) => new Uint8Array(n).fill(0),
    });
    expect(gen()).toBe("ent-00000-00000000000000000000");
  });

  it("encodes all-0xff bytes as the highest character (short)", () => {
    const gen = createShortGenerator("ent", {
      randomBytes: (n) => new Uint8Array(n).fill(0xff),
    });
    expect(gen()).toBe("ent-zzzzz-zzzzzzzzzz");
  });

  it("encodes all-0xff bytes as the highest character (long)", () => {
    const gen = createLongGenerator("ent", {
      randomBytes: (n) => new Uint8Array(n).fill(0xff),
    });
    expect(gen()).toBe("ent-zzzzz-zzzzzzzzzzzzzzzzzzzz");
  });
});

describe("collisions", () => {
  it("produces distinct ids over a large run (short)", () => {
    const gen = createShortGenerator("res");
    const ids = new Set<string>();
    for (let i = 0; i < 10_000; i++) {
      ids.add(gen());
    }
    expect(ids.size).toBe(10_000);
  });

  it("produces distinct ids over a large run (long)", () => {
    const gen = createLongGenerator("evt");
    const ids = new Set<string>();
    for (let i = 0; i < 10_000; i++) {
      ids.add(gen());
    }
    expect(ids.size).toBe(10_000);
  });
});

describe("prefix preservation", () => {
  it("preserves the exact prefix bytes verbatim", () => {
    for (const prefix of ["abc", "xyz", "000", "999", "a0b", "z9z"]) {
      const id = createShortGenerator(prefix)();
      expect(id.slice(0, 3)).toBe(prefix);
      expect(id[3]).toBe("-");
      expect(id[9]).toBe("-");
    }
  });
});

describe("randomBytes contract", () => {
  it("requests the right number of bytes (short)", () => {
    let requested = -1;
    const gen = createShortGenerator("res", {
      randomBytes: (n) => {
        requested = n;
        return new Uint8Array(n);
      },
    });
    gen();
    // 5 (tag) + 10 (suffix) = 15 chars; ceil(15/8) = 2 groups; 2*5 = 10 bytes.
    expect(requested).toBe(10);
  });

  it("requests the right number of bytes (long)", () => {
    let requested = -1;
    const gen = createLongGenerator("evt", {
      randomBytes: (n) => {
        requested = n;
        return new Uint8Array(n);
      },
    });
    gen();
    // 5 (tag) + 20 (suffix) = 25 chars; ceil(25/8) = 4 groups; 4*5 = 20 bytes.
    expect(requested).toBe(20);
  });
});
