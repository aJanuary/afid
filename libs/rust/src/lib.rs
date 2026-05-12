//! Generate afid ids — human-friendly random identifiers.
//!
//! An afid is shaped as `<prefix>-<tag>-<suffix>`, where the prefix is 3
//! characters of `[a-z0-9]`, the tag is 5 base32 chars of randomness, and the
//! suffix is 10 (short) or 20 (long) base32 chars of randomness. The base32
//! alphabet is Crockford-style: digits 0-9 followed by lowercase letters,
//! omitting `i`, `l`, `o`, and `u` to avoid visual confusion.
//!
//! There are two flavours of afids: short and long. Which to use depends on
//! your use case.
//!
//! Short afids are designed for scenarios where you are generating less than
//! 10 thousand afids per hour. They carry 75 bits of randomness. e.g.
//! `ent-2sed3-1p3dpw40ds`.
//!
//! Long afids are designed for scenarios where you are generating between 10
//! thousand and a trillion afids per hour, at the cost of longer ids. They
//! carry 125 bits of randomness. e.g. `ent-d3v2s-pp2m300zxs24mspqer3s`.
//!
//! For one-off ids use [`random_short`] or [`random_long`]. To generate many
//! ids, build a [`Generator`] via [`Generator::short`] or [`Generator::long`]
//! and reuse it — that way the prefix is validated a single time. `Generator`
//! also implements [`Iterator`], so `gen.take(n).collect()` and
//! `for id in &mut gen` both work.
//!
//! See the project README for a discussion of collision rates.
//!
//! # Examples
//!
//! ```
//! let id = afidgen::random_short("ent")?;
//! assert_eq!(id.len(), afidgen::SHORT_LEN);
//! # Ok::<(), afidgen::Error>(())
//! ```
//!
//! ```
//! let mut g = afidgen::Generator::long("usr")?;
//! let ids: Vec<String> = (&mut g).take(3).collect();
//! assert_eq!(ids.len(), 3);
//! # Ok::<(), afidgen::Error>(())
//! ```

use rand::Rng;
use rand::rngs::ThreadRng;

/// Length in bytes of a short afid.
pub const SHORT_LEN: usize = PREFIX_LEN + 1 + TAG_LEN + 1 + SHORT_SUFFIX_LEN;
/// Length in bytes of a long afid.
pub const LONG_LEN: usize = PREFIX_LEN + 1 + TAG_LEN + 1 + LONG_SUFFIX_LEN;

const PREFIX_LEN: usize = 3;
const TAG_LEN: usize = 5;
const SHORT_SUFFIX_LEN: usize = 10;
const LONG_SUFFIX_LEN: usize = 20;
const MAX_RANDOM_BYTES: usize = 20;
const MAX_DECODED_CHARS: usize = 32;

/// Crockford-style Base32 alphabet (omits `i`, `l`, `o`, `u`).
const ALPHABET: [u8; 32] = *b"0123456789abcdefghjkmnpqrstvwxyz";

/// Errors that can arise from constructing a [`Generator`] or calling
/// [`random_short`] / [`random_long`] with an invalid prefix.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Error {
    /// The prefix was not exactly 3 characters long.
    InvalidPrefixLength,
    /// The prefix contained a character outside `[a-z0-9]`.
    InvalidPrefixChars,
}

impl std::fmt::Display for Error {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Error::InvalidPrefixLength => f.write_str("prefix must be exactly 3 characters"),
            Error::InvalidPrefixChars => {
                f.write_str("prefix can only contain lowercase letters and numbers")
            }
        }
    }
}

impl std::error::Error for Error {}

fn validate_prefix(prefix: &str) -> Result<[u8; 3], Error> {
    let bytes = prefix.as_bytes();
    if bytes.len() != PREFIX_LEN {
        return Err(Error::InvalidPrefixLength);
    }
    for &b in bytes {
        let ok = b.is_ascii_digit() || b.is_ascii_lowercase();
        if !ok {
            return Err(Error::InvalidPrefixChars);
        }
    }
    Ok([bytes[0], bytes[1], bytes[2]])
}

/// Generates afids with a fixed prefix and variant.
///
/// Build one with [`Generator::short`] or [`Generator::long`] — the prefix is
/// validated once at construction, so subsequent generation is infallible.
/// Pass a custom RNG with [`Generator::short_with_rng`] or
/// [`Generator::long_with_rng`].
pub struct Generator<R: Rng = ThreadRng> {
    prefix: [u8; 3],
    suffix_len: usize,
    rng: R,
}

impl Generator<ThreadRng> {
    /// Build a generator that produces short afids using a thread-local
    /// cryptographic RNG.
    ///
    /// Short afids are designed for scenarios where you are generating less
    /// than 10 thousand afids per hour. They carry 75 bits of randomness.
    pub fn short(prefix: &str) -> Result<Self, Error> {
        Self::short_with_rng(prefix, rand::rng())
    }

    /// Build a generator that produces long afids using a thread-local
    /// cryptographic RNG.
    ///
    /// Long afids are designed for scenarios where you are generating between
    /// 10 thousand and a trillion afids per hour, at the cost of longer ids.
    /// They carry 125 bits of randomness.
    pub fn long(prefix: &str) -> Result<Self, Error> {
        Self::long_with_rng(prefix, rand::rng())
    }
}

impl<R: Rng> Generator<R> {
    /// Build a short-afid generator with a caller-supplied RNG.
    pub fn short_with_rng(prefix: &str, rng: R) -> Result<Self, Error> {
        Ok(Self {
            prefix: validate_prefix(prefix)?,
            suffix_len: SHORT_SUFFIX_LEN,
            rng,
        })
    }

    /// Build a long-afid generator with a caller-supplied RNG.
    pub fn long_with_rng(prefix: &str, rng: R) -> Result<Self, Error> {
        Ok(Self {
            prefix: validate_prefix(prefix)?,
            suffix_len: LONG_SUFFIX_LEN,
            rng,
        })
    }

    /// The prefix this generator was constructed with.
    pub fn prefix(&self) -> &str {
        // SAFETY: validate_prefix guarantees every byte is ASCII.
        unsafe { std::str::from_utf8_unchecked(&self.prefix) }
    }

    /// Length in bytes of every id this generator produces. Useful for
    /// sizing a buffer for [`generate_into`](Self::generate_into).
    pub fn id_len(&self) -> usize {
        PREFIX_LEN + 1 + TAG_LEN + 1 + self.suffix_len
    }

    /// Generate a fresh afid as an owned `String`.
    pub fn generate(&mut self) -> String {
        let mut buf = [0u8; LONG_LEN];
        let len = self.id_len();
        self.write_id(&mut buf[..len]);
        // SAFETY: write_id only writes ASCII bytes (digits, lowercase letters,
        // and '-'), so the prefix-truncated slice is valid UTF-8.
        unsafe { String::from_utf8_unchecked(buf[..len].to_vec()) }
    }

    /// Generate a fresh afid into a caller-supplied byte buffer, returning a
    /// `&str` view into the populated portion.
    ///
    /// # Panics
    ///
    /// Panics if `buf.len() < self.id_len()`. Use [`SHORT_LEN`] / [`LONG_LEN`]
    /// (or call [`id_len`](Self::id_len)) to size the buffer.
    pub fn generate_into<'b>(&mut self, buf: &'b mut [u8]) -> &'b str {
        let len = self.id_len();
        assert!(
            buf.len() >= len,
            "buffer too small: need {} bytes, got {}",
            len,
            buf.len(),
        );
        let slot = &mut buf[..len];
        self.write_id(slot);
        // SAFETY: write_id only writes ASCII bytes.
        unsafe { std::str::from_utf8_unchecked(slot) }
    }

    fn write_id(&mut self, out: &mut [u8]) {
        debug_assert_eq!(out.len(), self.id_len());

        let suffix_len = self.suffix_len;
        let n_chars = TAG_LEN + suffix_len;
        let n_groups = n_chars.div_ceil(8);
        let n_bytes = n_groups * 5;

        let mut rnd = [0u8; MAX_RANDOM_BYTES];
        self.rng.fill_bytes(&mut rnd[..n_bytes]);

        let mut decoded = [0u8; MAX_DECODED_CHARS];
        for g in 0..n_groups {
            extract8(&rnd, g * 5, &mut decoded, g * 8);
        }

        out[..PREFIX_LEN].copy_from_slice(&self.prefix);
        out[PREFIX_LEN] = b'-';
        out[PREFIX_LEN + 1..PREFIX_LEN + 1 + TAG_LEN].copy_from_slice(&decoded[..TAG_LEN]);
        out[PREFIX_LEN + 1 + TAG_LEN] = b'-';
        out[PREFIX_LEN + 2 + TAG_LEN..].copy_from_slice(&decoded[TAG_LEN..n_chars]);
    }
}

impl<R: Rng> Iterator for Generator<R> {
    type Item = String;

    fn next(&mut self) -> Option<String> {
        Some(self.generate())
    }
}

impl<R: Rng> std::fmt::Debug for Generator<R> {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("Generator")
            .field("prefix", &self.prefix())
            .field("id_len", &self.id_len())
            .finish_non_exhaustive()
    }
}

/// Decode 5 input bytes into 8 base32 characters using the Crockford alphabet.
#[inline]
fn extract8(src: &[u8; MAX_RANDOM_BYTES], s: usize, dst: &mut [u8; MAX_DECODED_CHARS], d: usize) {
    let b0 = u32::from(src[s]);
    let b1 = u32::from(src[s + 1]);
    let b2 = u32::from(src[s + 2]);
    let b3 = u32::from(src[s + 3]);
    let b4 = u32::from(src[s + 4]);
    dst[d] = ALPHABET[((b0 >> 3) & 0x1F) as usize];
    dst[d + 1] = ALPHABET[(((b0 << 2) | (b1 >> 6)) & 0x1F) as usize];
    dst[d + 2] = ALPHABET[((b1 >> 1) & 0x1F) as usize];
    dst[d + 3] = ALPHABET[(((b1 << 4) | (b2 >> 4)) & 0x1F) as usize];
    dst[d + 4] = ALPHABET[(((b2 << 1) | (b3 >> 7)) & 0x1F) as usize];
    dst[d + 5] = ALPHABET[((b3 >> 2) & 0x1F) as usize];
    dst[d + 6] = ALPHABET[(((b3 << 3) | (b4 >> 5)) & 0x1F) as usize];
    dst[d + 7] = ALPHABET[(b4 & 0x1F) as usize];
}

/// Return a short afid with the given prefix.
///
/// Short afids are designed for scenarios where you are generating less than
/// 10 thousand afids per hour. They carry 75 bits of randomness.
///
/// For bulk generation, build a [`Generator::short`] once so the prefix is
/// validated only once.
pub fn random_short(prefix: &str) -> Result<String, Error> {
    Ok(Generator::short(prefix)?.generate())
}

/// Return a long afid with the given prefix.
///
/// Long afids are designed for scenarios where you are generating between 10
/// thousand and a trillion afids per hour, at the cost of longer ids. They
/// carry 125 bits of randomness.
///
/// For bulk generation, build a [`Generator::long`] once so the prefix is
/// validated only once.
pub fn random_long(prefix: &str) -> Result<String, Error> {
    Ok(Generator::long(prefix)?.generate())
}
