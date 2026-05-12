const ALPHABET = new TextEncoder().encode("0123456789abcdefghjkmnpqrstvwxyz");
const PREFIX_RE = /^[a-z0-9]{3}$/;
const PREFIX_LEN = 3;
const TAG_LEN = 5;
const SHORT_SUFFIX_LEN = 10;
const LONG_SUFFIX_LEN = 20;
const DASH = 0x2d;

export type ErrorCode =
  | "INVALID_PREFIX_LENGTH"
  | "INVALID_PREFIX_CHARS"
  | "INVALID_RANDOM_BYTES";

export class AfidError extends Error {
  readonly code: ErrorCode;

  constructor(code: ErrorCode, message: string) {
    super(message);
    this.name = "AfidError";
    this.code = code;
  }
}

export type Generator = () => string;

export interface GeneratorOptions {
  /**
   * Return exactly `n` random bytes.
   * Defaults to `crypto.getRandomValues`.
   */
  randomBytes?: (n: number) => Uint8Array;
}

const defaultRandomBytes = (n: number): Uint8Array => {
  const bytes = new Uint8Array(n);
  crypto.getRandomValues(bytes);
  return bytes;
};

function validatePrefix(prefix: string): void {
  if (prefix.length !== PREFIX_LEN) {
    throw new AfidError(
      "INVALID_PREFIX_LENGTH",
      "prefix must be exactly 3 characters",
    );
  }
  if (!PREFIX_RE.test(prefix)) {
    throw new AfidError(
      "INVALID_PREFIX_CHARS",
      "prefix can only contain lowercase letters and numbers",
    );
  }
}

function createGenerator(
  prefix: string,
  suffixLen: number,
  options?: GeneratorOptions,
): Generator {
  validatePrefix(prefix);

  const randomBytes = options?.randomBytes ?? defaultRandomBytes;
  const nChars = TAG_LEN + suffixLen;
  const nGroups = Math.ceil(nChars / 8);
  const nRandBytes = nGroups * 5;
  const idLen = PREFIX_LEN + 1 + TAG_LEN + 1 + suffixLen;
  const tagOffset = PREFIX_LEN + 1;
  const suffixOffset = PREFIX_LEN + 1 + TAG_LEN + 1;

  // Buffers reused across every generation. The output buffer is
  // overwritten in place — only the tag/suffix slots change, so the
  // prefix bytes and dash separators are written once here.
  const decoded = new Uint8Array(nGroups * 8);
  const out = new Uint8Array(idLen);
  out[0] = prefix.charCodeAt(0);
  out[1] = prefix.charCodeAt(1);
  out[2] = prefix.charCodeAt(2);
  out[PREFIX_LEN] = DASH;
  out[PREFIX_LEN + 1 + TAG_LEN] = DASH;

  // Pre-built views avoid per-call subarray allocation in the hot path.
  const tagView = decoded.subarray(0, TAG_LEN);
  const suffixView = decoded.subarray(TAG_LEN, nChars);

  const decoder = new TextDecoder("latin1");

  return (): string => {
    const randBuf = randomBytes(nRandBytes);
    if (randBuf.length !== nRandBytes) {
      throw new AfidError(
        "INVALID_RANDOM_BYTES",
        `randomBytes must return exactly ${nRandBytes} bytes`,
      );
    }
    for (let g = 0; g < nGroups; g++) {
      const s = g * 5;
      const d = g * 8;
      // biome-ignore lint/style/noNonNullAssertion: bounded by nGroups
      const b0 = randBuf[s]!;
      // biome-ignore lint/style/noNonNullAssertion: bounded by nGroups
      const b1 = randBuf[s + 1]!;
      // biome-ignore lint/style/noNonNullAssertion: bounded by nGroups
      const b2 = randBuf[s + 2]!;
      // biome-ignore lint/style/noNonNullAssertion: bounded by nGroups
      const b3 = randBuf[s + 3]!;
      // biome-ignore lint/style/noNonNullAssertion: bounded by nGroups
      const b4 = randBuf[s + 4]!;
      decoded[d] = ALPHABET[(b0 >>> 3) & 0x1f] as number;
      decoded[d + 1] = ALPHABET[((b0 << 2) | (b1 >>> 6)) & 0x1f] as number;
      decoded[d + 2] = ALPHABET[(b1 >>> 1) & 0x1f] as number;
      decoded[d + 3] = ALPHABET[((b1 << 4) | (b2 >>> 4)) & 0x1f] as number;
      decoded[d + 4] = ALPHABET[((b2 << 1) | (b3 >>> 7)) & 0x1f] as number;
      decoded[d + 5] = ALPHABET[(b3 >>> 2) & 0x1f] as number;
      decoded[d + 6] = ALPHABET[((b3 << 3) | (b4 >>> 5)) & 0x1f] as number;
      decoded[d + 7] = ALPHABET[b4 & 0x1f] as number;
    }
    out.set(tagView, tagOffset);
    out.set(suffixView, suffixOffset);
    return decoder.decode(out);
  };
}

export function createShortGenerator(
  prefix: string,
  options?: GeneratorOptions,
): Generator {
  return createGenerator(prefix, SHORT_SUFFIX_LEN, options);
}

export function createLongGenerator(
  prefix: string,
  options?: GeneratorOptions,
): Generator {
  return createGenerator(prefix, LONG_SUFFIX_LEN, options);
}

export function randomShort(prefix: string): string {
  return createShortGenerator(prefix)();
}

export function randomLong(prefix: string): string {
  return createLongGenerator(prefix)();
}
