from hypothesis import given
from hypothesis.strategies import integers
import afid
import re


@given(integers())
def test_ids_only_include_base32_chars(value):
    generator = afid.long_generator("my-prefix", rand=lambda n: value)

    id = generator.next()

    assert re.match(
        r"^my-prefix-[0123456789abcdefghjkmnpqrstvwxyz]{5}-[0123456789abcdefghjkmnpqrstvwxyz]{20}",
        id,
    )


@given(integers())
def test_short_ids_are_17_characters_long_after_prefix(value):
    generator = afid.short_generator("my-prefix", rand=lambda n: value)

    id = generator.next()

    assert len(id) == 26


@given(integers())
def test_short_ids_are_27_characters_long_after_prefix(value):
    generator = afid.long_generator("my-prefix", rand=lambda n: value)

    id = generator.next()

    assert len(id) == 36
