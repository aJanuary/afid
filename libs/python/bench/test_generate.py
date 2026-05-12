import uuid

import afidgen


def test_random_short(benchmark):
    benchmark(afidgen.random_short, "ent")


def test_random_long(benchmark):
    benchmark(afidgen.random_long, "ent")


def test_short_generator(benchmark):
    gen = afidgen.Generator.short("ent")
    benchmark(gen)


def test_long_generator(benchmark):
    gen = afidgen.Generator.long("ent")
    benchmark(gen)


def test_uuid4(benchmark):
    # Baseline: stdlib UUIDv4 as a string.
    benchmark(lambda: str(uuid.uuid4()))
