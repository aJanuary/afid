import afid
import re
import pytest
from unittest.mock import MagicMock


def test_random_short_generates_ids_with_correct_shape():
    id = afid.random_short("my-prefix")

    assert re.match(r"my-prefix-[a-z0-9]{5}-[a-z0-9]{10}$", id)


def test_random_short_errors_if_prefix_is_empty():
    with pytest.raises(ValueError) as raised:
        afid.random_short("")

    assert str(raised.value) == "prefix must not be empty"


def test_random_short_errors_if_prefix_too_long():
    # maximum length doesn't throw
    afid.random_short("123456789-123456789-123456789-1")

    # longer than maximum length does throw
    with pytest.raises(ValueError) as raised:
        afid.random_short("123456789-123456789-123456789-12")

    assert str(raised.value) == "prefix cannot be longer than 31 characters"


def test_random_short_errors_if_prefix_contains_invalid_chars():
    # valid characters don't throw
    afid.random_short("abcde1234")
    afid.random_short("fghij5678")
    afid.random_short("klmnopr90")
    afid.random_short("stuvwxyz-")

    # invalid characters do throw
    with pytest.raises(ValueError) as raised:
        afid.random_short("ABC")
    assert (
        str(raised.value) == "prefix can only contain lowercase letters, numbers, or -"
    )

    with pytest.raises(ValueError) as raised:
        afid.random_short("a_b_c")
    assert (
        str(raised.value) == "prefix can only contain lowercase letters, numbers, or -"
    )


def test_random_long_generates_ids_with_correct_shape():
    id = afid.random_long("my-prefix")

    assert re.match(r"my-prefix-[a-z0-9]{5}-[a-z0-9]{20}$", id)


def test_random_long_errors_if_prefix_is_empty():
    with pytest.raises(ValueError) as raised:
        afid.random_long("")

    assert str(raised.value) == "prefix must not be empty"


def test_random_long_errors_if_prefix_too_long():
    # maximum length doesn't throw
    afid.random_long("123456789-123456789-1")

    # longer than maximum length does throw
    with pytest.raises(ValueError) as raised:
        afid.random_long("123456789-123456789-12")

    assert str(raised.value) == "prefix cannot be longer than 21 characters"


def test_random_long_errors_if_prefix_contains_invalid_chars():
    # valid characters don't throw
    afid.random_long("abcde1234")
    afid.random_long("fghij5678")
    afid.random_long("klmnopr90")
    afid.random_long("stuvwxyz-")

    # invalid characters do throw
    with pytest.raises(ValueError) as raised:
        afid.random_long("ABC")
    assert (
        str(raised.value) == "prefix can only contain lowercase letters, numbers, or -"
    )

    with pytest.raises(ValueError) as raised:
        afid.random_long("a_b_c")
    assert (
        str(raised.value) == "prefix can only contain lowercase letters, numbers, or -"
    )


def test_short_generator_returns_generator_with_correct_attributes():
    generator = afid.short_generator("my-prefix")

    assert generator.variant == afid.VARIANT_SHORT
    assert generator.prefix == "my-prefix"


def test_short_generator_generates_ids_with_correct_shape():
    generator = afid.short_generator("my-prefix")

    id = generator.next()

    assert re.match(r"my-prefix-[a-z0-9]{5}-[a-z0-9]{10}$", id)


def test_short_generator_uses_random_if_provided():
    getrandbits = MagicMock(return_value=0)
    generator = afid.short_generator("my-prefix", rand=getrandbits)

    generator.next()

    getrandbits.assert_called()


def test_long_generator_returns_generator_with_correct_attributes():
    generator = afid.long_generator("my-prefix")

    assert generator.variant == afid.VARIANT_LONG
    assert generator.prefix == "my-prefix"


def test_long_generator_generates_ids_with_correct_shape():
    generator = afid.long_generator("my-prefix")

    id = generator.next()

    assert re.match(r"my-prefix-[a-z0-9]{5}-[a-z0-9]{20}$", id)


def test_long_generator_uses_random_if_provided():
    getrandbits = MagicMock(return_value=0)
    generator = afid.long_generator("my-prefix", rand=getrandbits)

    generator.next()

    getrandbits.assert_called()


def test_generator_returns_generator_with_correct_attributes():
    short_generator = afid.short_generator("my-prefix")
    assert short_generator.variant == afid.VARIANT_SHORT
    assert short_generator.prefix == "my-prefix"

    long_generator = afid.long_generator("my-prefix")
    assert long_generator.variant == afid.VARIANT_LONG
    assert long_generator.prefix == "my-prefix"


def test_generator_generators_id_with_correct_shape():
    short_generator = afid.short_generator("my-prefix")
    short_id = short_generator.next()
    assert re.match(r"my-prefix-[a-z0-9]{5}-[a-z0-9]{10}$", short_id)

    long_generator = afid.long_generator("my-prefix")
    long_id = long_generator.next()
    assert re.match(r"my-prefix-[a-z0-9]{5}-[a-z0-9]{20}$", long_id)


def test_generator_uses_random_if_provided():
    short_getrandbits = MagicMock(return_value=0)
    short_generator = afid.short_generator("my-prefix", rand=short_getrandbits)
    short_generator.next()
    short_getrandbits.assert_called()

    long_getrandbits = MagicMock(return_value=0)
    long_generator = afid.long_generator("my-prefix", rand=long_getrandbits)
    long_generator.next()
    long_getrandbits.assert_called()
