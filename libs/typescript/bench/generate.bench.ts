import { randomUUID } from "node:crypto";
import { bench, describe } from "vitest";
import {
  createLongGenerator,
  createShortGenerator,
  randomLong,
  randomShort,
} from "../src/index.js";

const shortGen = createShortGenerator("res");
const longGen = createLongGenerator("evt");

describe("one-off shortcuts", () => {
  bench("randomShort", () => {
    randomShort("res");
  });

  bench("randomLong", () => {
    randomLong("evt");
  });
});

describe("prebuilt generator", () => {
  bench("createShortGenerator().()", () => {
    shortGen();
  });

  bench("createLongGenerator().()", () => {
    longGen();
  });
});

describe("baseline", () => {
  bench("crypto.randomUUID (UUIDv4)", () => {
    randomUUID();
  });
});
