from collections import namedtuple
from collections.abc import Callable
import random
import re


# TODO: Figure out how to do this enum. Arbitrary suffix lengths are difficult to support.
Variant = namedtuple("Variant", ["name", "suffix_length"])
VARIANT_SHORT = Variant("short", 10)
VARIANT_LONG = Variant("long", 20)


class Generator:
    MAX_LEN = 48
    TAG_LEN = 5
    PREFIX_PATTERN = re.compile("^[a-z0-9-]+$")
    BASE32_ALPHABET = "0123456789abcdefghjkmnpqrstvwxyz"

    def __init__(self, rand: Callable[[int], int], variant: Variant, prefix: str):
        self.__validate_params(variant, prefix)
        self.rand = rand
        self.variant = variant
        self.prefix = prefix

    def __validate_params(self, variant: Variant, prefix: str):
        if len(prefix) == 0:
            raise ValueError("prefix must not be empty")

        max_prefix_len = self.MAX_LEN - self.TAG_LEN - variant.suffix_length - 2
        if len(prefix) > max_prefix_len:
            raise ValueError(
                f"prefix cannot be longer than {max_prefix_len} characters"
            )

        if not self.PREFIX_PATTERN.match(prefix):
            raise ValueError("prefix can only contain lowercase letters, numbers, or -")

    def next(self):
        id = (
            self.prefix
            + "-"
            + self._generate_5_chars()
            + "-"
            + self._generate_5_chars()
            + self._generate_5_chars()
        )
        if self.variant == VARIANT_LONG:
            id += self._generate_5_chars() + self._generate_5_chars()
        return id

    def _generate_5_chars(self):
        src = self.rand(25)
        return (
            self.BASE32_ALPHABET[(src & 0x001F00000) >> 20]
            + self.BASE32_ALPHABET[(src & 0x000F8000) >> 15]
            + self.BASE32_ALPHABET[(src & 0x0007C00) >> 10]
            + self.BASE32_ALPHABET[(src & 0x000003E0) >> 5]
            + self.BASE32_ALPHABET[(src & 0x0000001F)]
        )


def short_generator(
    prefix: str, rand: Callable[[int], int] = random.getrandbits
) -> Generator:
    return Generator(rand, VARIANT_SHORT, prefix)


def long_generator(
    prefix: str, rand: Callable[[int], int] = random.getrandbits
) -> Generator:
    return Generator(rand, VARIANT_LONG, prefix)


def generator(
    variant: Variant, prefix: str, rand: Callable[[int], int] = random.getrandbits
) -> Generator:
    return Generator(rand, variant, prefix)


def random_short(prefix: str) -> str:
    return short_generator(prefix).next()


def random_long(prefix: str) -> str:
    return long_generator(prefix).next()
