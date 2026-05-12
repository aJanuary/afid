use afidgen::{Error, Generator, LONG_LEN, SHORT_LEN, random_long, random_short};

const ALPHABET: &str = "0123456789abcdefghjkmnpqrstvwxyz";

fn is_alphabet_char(c: char) -> bool {
    ALPHABET.contains(c)
}

fn assert_short_shape(prefix: &str, id: &str) {
    assert_shape(prefix, id, 10);
}

fn assert_long_shape(prefix: &str, id: &str) {
    assert_shape(prefix, id, 20);
}

fn assert_shape(prefix: &str, id: &str, suffix_len: usize) {
    assert_eq!(
        id.len(),
        3 + 1 + 5 + 1 + suffix_len,
        "wrong total length: {id}"
    );
    let parts: Vec<&str> = id.split('-').collect();
    assert_eq!(parts.len(), 3, "expected three '-' separated parts: {id}");
    assert_eq!(parts[0], prefix);
    assert_eq!(parts[1].len(), 5);
    assert_eq!(parts[2].len(), suffix_len);
    for c in parts[1].chars().chain(parts[2].chars()) {
        assert!(
            is_alphabet_char(c),
            "char {c:?} not in Crockford alphabet: {id}"
        );
    }
}

#[test]
fn random_short_has_right_shape() {
    let id = random_short("ent").unwrap();
    assert_short_shape("ent", &id);
    assert_eq!(id.len(), SHORT_LEN);
}

#[test]
fn random_long_has_right_shape() {
    let id = random_long("ent").unwrap();
    assert_long_shape("ent", &id);
    assert_eq!(id.len(), LONG_LEN);
}

#[test]
fn short_generator_generate_has_right_shape() {
    let mut g = Generator::short("usr").unwrap();
    for _ in 0..50 {
        let id = g.generate();
        assert_short_shape("usr", &id);
    }
}

#[test]
fn long_generator_generate_has_right_shape() {
    let mut g = Generator::long("usr").unwrap();
    for _ in 0..50 {
        let id = g.generate();
        assert_long_shape("usr", &id);
    }
}

#[test]
fn generator_implements_iterator() {
    let mut g = Generator::short("xyz").unwrap();
    let ids: Vec<String> = (&mut g).take(5).collect();
    assert_eq!(ids.len(), 5);
    for id in &ids {
        assert_short_shape("xyz", id);
    }
}

#[test]
fn generate_into_writes_to_buffer_without_allocating() {
    let mut g = Generator::short("ent").unwrap();
    let mut buf = [0u8; SHORT_LEN];
    let id = g.generate_into(&mut buf);
    assert_eq!(id.len(), SHORT_LEN);
    assert_short_shape("ent", id);
}

#[test]
fn generate_into_accepts_oversized_buffer() {
    let mut g = Generator::short("ent").unwrap();
    let mut buf = [0u8; 64];
    let id = g.generate_into(&mut buf);
    assert_eq!(id.len(), SHORT_LEN);
    assert_short_shape("ent", id);
}

#[test]
#[should_panic(expected = "buffer too small")]
fn generate_into_panics_when_buffer_too_small() {
    let mut g = Generator::long("ent").unwrap();
    let mut buf = [0u8; SHORT_LEN]; // long needs 30, short buffer is 20
    let _ = g.generate_into(&mut buf);
}

#[test]
fn prefix_getter_returns_constructor_value() {
    let g = Generator::short("a0z").unwrap();
    assert_eq!(g.prefix(), "a0z");
}

#[test]
fn id_len_matches_variant() {
    assert_eq!(Generator::short("ent").unwrap().id_len(), SHORT_LEN);
    assert_eq!(Generator::long("ent").unwrap().id_len(), LONG_LEN);
}

#[test]
fn prefix_too_short_is_rejected() {
    assert_eq!(random_short("ab").unwrap_err(), Error::InvalidPrefixLength);
    assert_eq!(
        Generator::short("ab").unwrap_err(),
        Error::InvalidPrefixLength
    );
}

#[test]
fn prefix_too_long_is_rejected() {
    assert_eq!(
        random_short("abcd").unwrap_err(),
        Error::InvalidPrefixLength
    );
}

#[test]
fn empty_prefix_is_rejected() {
    assert_eq!(random_short("").unwrap_err(), Error::InvalidPrefixLength);
}

#[test]
fn uppercase_prefix_is_rejected() {
    assert_eq!(random_short("ABC").unwrap_err(), Error::InvalidPrefixChars);
    assert_eq!(random_long("aBc").unwrap_err(), Error::InvalidPrefixChars);
}

#[test]
fn punctuation_in_prefix_is_rejected() {
    assert_eq!(random_short("a-b").unwrap_err(), Error::InvalidPrefixChars);
    assert_eq!(random_short("a b").unwrap_err(), Error::InvalidPrefixChars);
    assert_eq!(random_short("a.b").unwrap_err(), Error::InvalidPrefixChars);
}

#[test]
fn all_digits_prefix_is_allowed() {
    let id = random_short("000").unwrap();
    assert_short_shape("000", &id);
}

#[test]
fn all_letters_prefix_is_allowed() {
    let id = random_long("abc").unwrap();
    assert_long_shape("abc", &id);
}

#[test]
fn error_messages_are_useful() {
    assert_eq!(
        Error::InvalidPrefixLength.to_string(),
        "prefix must be exactly 3 characters",
    );
    assert_eq!(
        Error::InvalidPrefixChars.to_string(),
        "prefix can only contain lowercase letters and numbers",
    );
}
