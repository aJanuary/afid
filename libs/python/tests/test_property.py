import re

from hypothesis import given
from hypothesis.strategies import binary

import afidgen


SHORT_BYTES = binary(min_size=10, max_size=10)
LONG_BYTES = binary(min_size=20, max_size=20)
B32_ALPHABET = "0123456789abcdefghjkmnpqrstvwxyz"


@given(LONG_BYTES)
def test_ids_only_include_base32_chars(value):
    gen = afidgen.Generator.long("ent", randbytes=lambda _n: value)

    id = gen()

    assert re.match(
        rf"^ent-[{B32_ALPHABET}]{{5}}-[{B32_ALPHABET}]{{20}}\Z",
        id,
    )


@given(SHORT_BYTES)
def test_short_ids_are_20_characters_long(value):
    gen = afidgen.Generator.short("ent", randbytes=lambda _n: value)

    assert len(gen()) == 20


@given(LONG_BYTES)
def test_long_ids_are_30_characters_long(value):
    gen = afidgen.Generator.long("ent", randbytes=lambda _n: value)

    assert len(gen()) == 30
