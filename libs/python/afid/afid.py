from collections import namedtuple
from collections.abc import Callable
from enum import Enum
import random
import re


Variant = namedtuple("Variant", ["name", "suffix_length"])


class Variants(Enum):
    SHORT = Variant("short", 10)
    LONG = Variant("long", 20)


class Generator:
    MAX_LEN = 48
    TAG_LEN = 5
    PREFIX_PATTERN = re.compile("^[a-z0-9-]$")

    def __init__(self, rand: Callable[[int], int], variant: Variant, prefix: str):
        self.rand = rand
        self.variant = variant
        self.prefix = prefix

    def __validate_params(self, variant: Variant, prefix: str):
        if len(prefix) == 0:
            raise ValueError("prefix must not be empty")

        max_prefix_len = self.MAX_LEN - self.TAG_LEN - variant.suffix_length - 2
        if len(prefix) > max_prefix_len:
            raise ValueError(f"prefix cannot be longer than {max_prefix_len} characters")

        if not self.PREFIX_PATTERN.match(prefix):
            raise ValueError("prefix can only contain lowercase letters, numbers, or -")


class AFID:
    def short_generator(prefix: str) -> Generator:
        return Generator(random.getrandbits, Variant.SHORT, prefix)

    def long_generator(prefix: str) -> Generator:
        pass

    def generator(variant: Variant, prefix: str) -> Generator:
        pass

    def random_short(prefix: str) -> str:
        "a"

    def random_long(prefix: str) -> str:
        "b"

    def random(variant: Variant, prefix: str) -> str:
        "c"
