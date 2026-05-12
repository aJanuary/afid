import itertools
import re
from unittest.mock import MagicMock

import pytest

import afidgen


SHORT_RE = re.compile(r"ent-[0-9a-hjkmnp-tv-z]{5}-[0-9a-hjkmnp-tv-z]{10}\Z")
LONG_RE = re.compile(r"ent-[0-9a-hjkmnp-tv-z]{5}-[0-9a-hjkmnp-tv-z]{20}\Z")


def test_random_short_generates_ids_with_correct_shape():
    assert SHORT_RE.match(afidgen.random_short("ent"))


def test_random_long_generates_ids_with_correct_shape():
    assert LONG_RE.match(afidgen.random_long("ent"))


def test_generator_short_produces_short_ids():
    assert SHORT_RE.match(afidgen.Generator.short("ent")())


def test_generator_long_produces_long_ids():
    assert LONG_RE.match(afidgen.Generator.long("ent")())


@pytest.mark.parametrize("build", [afidgen.Generator.short, afidgen.Generator.long])
def test_generator_exposes_prefix(build):
    assert build("ent").prefix == "ent"


def test_generator_is_callable():
    gen = afidgen.Generator.short("ent")
    assert SHORT_RE.match(gen())


def test_generator_supports_next_builtin():
    gen = afidgen.Generator.short("ent")
    assert SHORT_RE.match(next(gen))


def test_generator_is_iterable():
    gen = afidgen.Generator.short("ent")

    ids = list(itertools.islice(gen, 3))

    assert len(ids) == 3
    assert all(SHORT_RE.match(i) for i in ids)


def test_generator_uses_randbytes_if_provided():
    randbytes = MagicMock(side_effect=lambda n: bytes(n))

    afidgen.Generator.short("ent", randbytes=randbytes)()

    randbytes.assert_called_once()


@pytest.mark.parametrize("build", [afidgen.Generator.short, afidgen.Generator.long])
def test_generator_rejects_none_randbytes(build):
    with pytest.raises(ValueError, match="randbytes cannot be None"):
        build("ent", randbytes=None)


@pytest.mark.parametrize(
    "build,expected", [(afidgen.Generator.short, 10), (afidgen.Generator.long, 20)]
)
def test_generator_rejects_wrong_randbytes_length(build, expected):
    gen = build("ent", randbytes=lambda n: bytes(max(n - 1, 0)))

    with pytest.raises(
        ValueError, match=rf"randbytes must return exactly {expected} bytes"
    ):
        gen()


@pytest.mark.parametrize("factory", [afidgen.random_short, afidgen.random_long])
@pytest.mark.parametrize("prefix", ["", "ab", "abcd", "aaaaa"])
def test_random_helpers_reject_wrong_length_prefix(factory, prefix):
    with pytest.raises(ValueError, match="prefix must be exactly 3 characters"):
        factory(prefix)


@pytest.mark.parametrize("factory", [afidgen.random_short, afidgen.random_long])
@pytest.mark.parametrize("prefix", ["ABC", "a_b", "a-b", "a b"])
def test_random_helpers_reject_invalid_characters(factory, prefix):
    with pytest.raises(
        ValueError, match="prefix can only contain lowercase letters and numbers"
    ):
        factory(prefix)


@pytest.mark.parametrize("build", [afidgen.Generator.short, afidgen.Generator.long])
@pytest.mark.parametrize("prefix", ["", "ab", "abcd"])
def test_generator_rejects_wrong_length_prefix(build, prefix):
    with pytest.raises(ValueError, match="prefix must be exactly 3 characters"):
        build(prefix)


@pytest.mark.parametrize("build", [afidgen.Generator.short, afidgen.Generator.long])
@pytest.mark.parametrize("prefix", ["ABC", "a_b", "a-b", "a b"])
def test_generator_rejects_invalid_characters(build, prefix):
    with pytest.raises(
        ValueError, match="prefix can only contain lowercase letters and numbers"
    ):
        build(prefix)


@pytest.mark.parametrize("prefix", ["abc", "xyz", "012", "a1b", "z0z"])
def test_generator_accepts_valid_prefixes(prefix):
    # Should not raise.
    afidgen.Generator.short(prefix)
    afidgen.Generator.long(prefix)
