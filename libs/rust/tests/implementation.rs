//! Implementation-level tests — these probe the exact bits produced by the
//! Crockford base32 encoder. They are sensitive to the internal byte layout: a
//! refactor of the encoder will likely break them, which is the point.

use afidgen::Generator;
use rand::TryRng;
use std::collections::HashSet;
use std::convert::Infallible;

/// `Rng` that emits a single byte value forever.
struct ConstByte(u8);
impl TryRng for ConstByte {
    type Error = Infallible;
    fn try_next_u32(&mut self) -> Result<u32, Infallible> {
        Ok(u32::from_le_bytes([self.0; 4]))
    }
    fn try_next_u64(&mut self) -> Result<u64, Infallible> {
        Ok(u64::from_le_bytes([self.0; 8]))
    }
    fn try_fill_bytes(&mut self, dst: &mut [u8]) -> Result<(), Infallible> {
        dst.fill(self.0);
        Ok(())
    }
}

/// `Rng` that emits a fixed-length byte sequence, cycling if necessary.
struct FixedBytes {
    bytes: Vec<u8>,
    pos: usize,
}
impl FixedBytes {
    fn new(bytes: Vec<u8>) -> Self {
        assert!(!bytes.is_empty());
        Self { bytes, pos: 0 }
    }
}
impl TryRng for FixedBytes {
    type Error = Infallible;
    fn try_next_u32(&mut self) -> Result<u32, Infallible> {
        let mut b = [0u8; 4];
        self.try_fill_bytes(&mut b)?;
        Ok(u32::from_le_bytes(b))
    }
    fn try_next_u64(&mut self) -> Result<u64, Infallible> {
        let mut b = [0u8; 8];
        self.try_fill_bytes(&mut b)?;
        Ok(u64::from_le_bytes(b))
    }
    fn try_fill_bytes(&mut self, dst: &mut [u8]) -> Result<(), Infallible> {
        for slot in dst.iter_mut() {
            *slot = self.bytes[self.pos % self.bytes.len()];
            self.pos += 1;
        }
        Ok(())
    }
}

#[test]
fn all_zero_bytes_produce_lowest_id_short() {
    let mut g = Generator::short_with_rng("ent", ConstByte(0x00)).unwrap();
    assert_eq!(g.generate(), "ent-00000-0000000000");
}

#[test]
fn all_one_bits_produce_highest_id_short() {
    let mut g = Generator::short_with_rng("ent", ConstByte(0xFF)).unwrap();
    assert_eq!(g.generate(), "ent-zzzzz-zzzzzzzzzz");
}

#[test]
fn all_zero_bytes_produce_lowest_id_long() {
    let mut g = Generator::long_with_rng("ent", ConstByte(0x00)).unwrap();
    assert_eq!(g.generate(), "ent-00000-00000000000000000000");
}

#[test]
fn all_one_bits_produce_highest_id_long() {
    let mut g = Generator::long_with_rng("ent", ConstByte(0xFF)).unwrap();
    assert_eq!(g.generate(), "ent-zzzzz-zzzzzzzzzzzzzzzzzzzz");
}

/// Bulk generation with a real cryptographic RNG should never produce
/// duplicates — at 1 000 ids per run, the birthday-paradox probability is
/// astronomically low (2^-50 ish for short, 2^-100 ish for long).
#[test]
fn bulk_generation_produces_unique_ids_short() {
    let mut g = Generator::short("ent").unwrap();
    let ids: HashSet<String> = (&mut g).take(1_000).collect();
    assert_eq!(ids.len(), 1_000);
}

#[test]
fn bulk_generation_produces_unique_ids_long() {
    let mut g = Generator::long("ent").unwrap();
    let ids: HashSet<String> = (&mut g).take(1_000).collect();
    assert_eq!(ids.len(), 1_000);
}

/// Setting the high bit of any byte that the encoder consumes for the
/// short variant changes the output. For the short variant, both 5-byte
/// groups have at least their high bits flow into a used char, so this
/// holds for all 10 input bytes.
#[test]
fn flipping_an_input_byte_changes_the_id_short() {
    let base = vec![0x00u8; 10];
    let mut g0 = Generator::short_with_rng("ent", FixedBytes::new(base.clone())).unwrap();
    let id0 = g0.generate();

    for i in 0..10 {
        let mut flipped = base.clone();
        flipped[i] = 0x80;
        let mut g = Generator::short_with_rng("ent", FixedBytes::new(flipped)).unwrap();
        let id = g.generate();
        assert_ne!(
            id, id0,
            "flipping byte {i} did not change the id (got {id})"
        );
    }
}
