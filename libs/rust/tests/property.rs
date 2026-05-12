use afidgen::{Generator, LONG_LEN, SHORT_LEN};
use proptest::prelude::*;
use rand::TryRng;
use std::convert::Infallible;

/// A deterministic `Rng` that hands out a fixed byte sequence. Wraps an
/// internal cursor; advancing past the end re-cycles from the start.
struct FixedBytes {
    bytes: Vec<u8>,
    pos: usize,
}

impl FixedBytes {
    fn new(bytes: Vec<u8>) -> Self {
        assert!(!bytes.is_empty(), "FixedBytes needs at least one byte");
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

const ALPHABET: &str = "0123456789abcdefghjkmnpqrstvwxyz";

fn check_shape(prefix: &str, id: &str, suffix_len: usize) {
    assert_eq!(id.len(), 3 + 1 + 5 + 1 + suffix_len);
    assert_eq!(&id[..3], prefix);
    assert_eq!(&id[3..4], "-");
    assert_eq!(&id[9..10], "-");
    for c in id[4..9].chars().chain(id[10..].chars()) {
        assert!(ALPHABET.contains(c), "{c:?} not in Crockford alphabet");
    }
}

proptest! {
    #[test]
    fn short_ids_have_valid_shape_for_any_seed(bytes in prop::collection::vec(any::<u8>(), 20..21)) {
        let mut g = Generator::short_with_rng("ent", FixedBytes::new(bytes)).unwrap();
        for _ in 0..10 {
            let id = g.generate();
            check_shape("ent", &id, 10);
        }
    }

    #[test]
    fn long_ids_have_valid_shape_for_any_seed(bytes in prop::collection::vec(any::<u8>(), 20..21)) {
        let mut g = Generator::long_with_rng("ent", FixedBytes::new(bytes)).unwrap();
        for _ in 0..10 {
            let id = g.generate();
            check_shape("ent", &id, 20);
        }
    }

    #[test]
    fn generate_into_matches_generate_byte_for_byte(bytes in prop::collection::vec(any::<u8>(), 20..21)) {
        // Same RNG state should produce the same id whether we route through
        // generate() (allocating) or generate_into() (zero-alloc).
        let mut g1 = Generator::long_with_rng("xyz", FixedBytes::new(bytes.clone())).unwrap();
        let mut g2 = Generator::long_with_rng("xyz", FixedBytes::new(bytes)).unwrap();

        let owned = g1.generate();
        let mut buf = [0u8; LONG_LEN];
        let borrowed = g2.generate_into(&mut buf);
        prop_assert_eq!(owned.as_str(), borrowed);
    }

    #[test]
    fn iterator_emits_correctly_shaped_ids(bytes in prop::collection::vec(any::<u8>(), 20..21), n in 0usize..50) {
        let g = Generator::short_with_rng("usr", FixedBytes::new(bytes)).unwrap();
        let ids: Vec<String> = g.take(n).collect();
        prop_assert_eq!(ids.len(), n);
        for id in &ids {
            prop_assert_eq!(id.len(), SHORT_LEN);
        }
    }
}
