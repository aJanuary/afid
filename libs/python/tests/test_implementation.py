from hypothesis import given
from hypothesis.strategies import binary, integers, lists, tuples

import afidgen


# Long consumes 125 bits: 15 full bytes + top 5 bits of byte 15. Bottom 3 bits
# of byte 15 and bytes 16-19 are discarded, so build canonical 20-byte
# sequences where those bits are zero — distinct inputs then map to distinct
# ids.
def _canonical_long_bytes():
    return tuples(
        binary(min_size=15, max_size=15), integers(min_value=0, max_value=31)
    ).map(lambda pair: pair[0] + bytes([pair[1] << 3]) + b"\x00" * 4)


# These tests know too much about the implementation, and are testing things
# that aren't a part of the spec, but just happen to be how the functions are
# implemented. For example, there isn't anything that dictates the order of the
# alphabet, so the random source returning all-zero bytes could validly
# translate into an id of `ent-qwert-yupasdfghjklzxcvbnm0`. However, regression
# tests on the boundary values are the cheapest way to catch a base32-mapping
# regression.


def test_works_with_lowest_value():
    gen = afidgen.Generator.long("ent", randbytes=lambda n: bytes(n))

    assert gen() == "ent-00000-00000000000000000000"


def test_works_with_highest_value():
    gen = afidgen.Generator.long("ent", randbytes=lambda n: b"\xff" * n)

    assert gen() == "ent-zzzzz-zzzzzzzzzzzzzzzzzzzz"


@given(lists(_canonical_long_bytes(), unique=True))
def test_distinct_byte_sequences_yield_distinct_ids(byte_sequences):
    ids = [
        afidgen.Generator.long("ent", randbytes=lambda _n, s=seq: s)()
        for seq in byte_sequences
    ]

    assert len(ids) == len(set(ids))
