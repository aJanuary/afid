"""Generate afid ids — human-friendly random identifiers.

An afid is shaped as ``<prefix>-<tag>-<suffix>``, where the prefix is 3
characters of ``[a-z0-9]``, the tag is 5 base32 chars of randomness, and the
suffix is 10 (short) or 20 (long) base32 chars of randomness. The base32
alphabet is Crockford-style: digits 0-9 followed by lowercase letters,
omitting ``i``, ``l``, ``o``, and ``u`` to avoid visual confusion.

There are two flavours of afids: short and long. Which to use depends on
your use case.

Short afids are designed for scenarios where you are generating less than 10
thousand afids per hour. They carry 75 bits of randomness. e.g.
``ent-2sed3-1p3dpw40ds``

Long afids are designed for scenarios where you are generating between 10
thousand and a trillion afids per hour, at the cost of longer ids. They carry
125 bits of randomness. e.g. ``ent-d3v2s-pp2m300zxs24mspqer3s``

For one-off ids use :func:`random_short` or :func:`random_long`. To generate
many ids, build a :class:`Generator` via :meth:`Generator.short` or
:meth:`Generator.long` and reuse it — that way the prefix is validated a
single time. ``Generator`` is callable and iterable, so ``gen()``,
``next(gen)``, and ``for id in gen: ...`` all work.

See the project README for a discussion of collision rates.
"""

import base64
import re
import secrets
from collections.abc import Callable, Iterator
from typing import Literal

__all__ = ["Generator", "random_short", "random_long"]

_B32_TRANSLATE = bytes.maketrans(
    b"ABCDEFGHIJKLMNOPQRSTUVWXYZ234567",
    b"0123456789abcdefghjkmnpqrstvwxyz",
)
_PREFIX_LEN = 3
_TAG_LEN = 5
_SHORT_SUFFIX_LEN = 10
_LONG_SUFFIX_LEN = 20
_PREFIX_RE = re.compile(rf"[a-z0-9]{{{_PREFIX_LEN}}}\Z")


class Generator:
    """Generates afids with a fixed prefix and suffix length.

    Construct via :meth:`Generator.short` or :meth:`Generator.long`. Instances
    are callable (``gen()``), implement the iterator protocol (``next(gen)``,
    ``for id in gen: ...``), and are safe to reuse.

    Raises:
        ValueError: If ``prefix`` is not 3 chars of ``[a-z0-9]``, or
            ``randbytes`` returns the wrong number of bytes.
    """

    def __init__(
        self,
        prefix: str,
        suffix_length: Literal[10, 20],
        randbytes: Callable[[int], bytes],
    ) -> None:
        if len(prefix) != _PREFIX_LEN:
            raise ValueError("prefix must be exactly 3 characters")
        if not _PREFIX_RE.match(prefix):
            raise ValueError("prefix can only contain lowercase letters and numbers")
        if randbytes is None:
            raise ValueError("randbytes cannot be None")

        self.prefix = prefix
        self.suffix_length = suffix_length
        self._randbytes = randbytes
        # base64.b32encode consumes 5-byte groups -> 8 output chars; pick the
        # smallest multiple-of-5 byte count that covers the encoded output
        # and drop the unused trailing chars.
        self._n_bytes = ((_TAG_LEN + suffix_length + 7) // 8) * 5

    @classmethod
    def short(
        cls,
        prefix: str,
        *,
        randbytes: Callable[[int], bytes] = secrets.token_bytes,
    ) -> "Generator":
        """Build a generator that produces short afids.

        Short afids are designed for scenarios where you are generating less
        than 10 thousand afids per hour. They carry 75 bits of randomness.

        Pass ``randbytes=`` to override the randomness source (defaults to
        :func:`secrets.token_bytes`). Must return exactly ``n`` bytes when
        called as ``randbytes(n)``.
        """
        return cls(prefix, _SHORT_SUFFIX_LEN, randbytes)

    @classmethod
    def long(
        cls,
        prefix: str,
        *,
        randbytes: Callable[[int], bytes] = secrets.token_bytes,
    ) -> "Generator":
        """Build a generator that produces long afids.

        Long afids are designed for scenarios where you are generating
        between 10 thousand and a trillion afids per hour, at the cost of
        longer ids. They carry 125 bits of randomness.

        Pass ``randbytes=`` to override the randomness source (defaults to
        :func:`secrets.token_bytes`). Must return exactly ``n`` bytes when
        called as ``randbytes(n)``.
        """
        return cls(prefix, _LONG_SUFFIX_LEN, randbytes)

    def __call__(self) -> str:
        """Return a freshly generated afid."""
        random_bytes = self._randbytes(self._n_bytes)
        if len(random_bytes) != self._n_bytes:
            raise ValueError(f"randbytes must return exactly {self._n_bytes} bytes")
        encoded = base64.b32encode(random_bytes).translate(_B32_TRANSLATE)
        return (
            f"{self.prefix}-"
            f"{encoded[:_TAG_LEN].decode('ascii')}-"
            f"{encoded[_TAG_LEN : _TAG_LEN + self.suffix_length].decode('ascii')}"
        )

    __next__ = __call__

    def __iter__(self) -> Iterator[str]:
        return self


def random_short(prefix: str) -> str:
    """Return a short afid with the given prefix.

    Short afids are designed for scenarios where you are generating less than
    10 thousand afids per hour. They carry 75 bits of randomness.

    For bulk generation, use :meth:`Generator.short` so the prefix is
    validated only once.
    """
    return Generator.short(prefix)()


def random_long(prefix: str) -> str:
    """Return a long afid with the given prefix.

    Long afids are designed for scenarios where you are generating between 10
    thousand and a trillion afids per hour, at the cost of longer ids. They
    carry 125 bits of randomness.

    For bulk generation, use :meth:`Generator.long` so the prefix is
    validated only once.
    """
    return Generator.long(prefix)()
