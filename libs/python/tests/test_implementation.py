import afid
from hypothesis import given
from hypothesis.strategies import integers, lists


# These tests know too much about the implementation, and are testing things
# that aren't a part of the spec, but just happen to be how the functions are
# implemented. For example, there isn't anything that dictates the order of the
# alphabet, so the random source returning 0 could validly translate into an id
# of 12345-qwertyupasdfghjklzxc. However, I don't know how to test things like
# not overflowing otherwise, and I'm not confident enough in the bit-twiddling
# to not regression test them.


def test_works_with_lowest_value():
    def getrandbits(n):
        return 0

    generator = afid.long_generator("my-prefix", rand=getrandbits)

    id = generator.next()

    assert id == "my-prefix-00000-00000000000000000000"


def test_works_with_highest_value():
    def getrandbits(n):
        return 0x01FFFFFF

    generator = afid.long_generator("my-prefix", rand=getrandbits)

    id = generator.next()

    assert id == "my-prefix-zzzzz-zzzzzzzzzzzzzzzzzzzz"


@given(lists(integers(max_value=0x1FFFFFF), unique=True))
def test_distinct_random_numbers_yield_distinct_ids(values):
    ids = [
        afid.long_generator("my-prefix", rand=lambda n: value).next()
        for value in values
    ]

    assert len(ids) == len(set(ids))
