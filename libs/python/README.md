# afid

A Python library for generating [afid ids](https://github.com/aJanuary/afid).

## Installing
```sh
pip install afid
```

## Usage

```python
import afid

# Generate a short ID with the prefix "resource".
resource_id = afid.random_short("resource")

# Generate a long ID with the prefix "event".
event_id = afid.random_long("event")

# Create a generator. This is more efficient to create multiple afids, because
# it only needs to validate the prefix once when you create the generator,
# instead of every time you generate and id.
generator = afid.long_generator("event")
event_ids = [generator.next() for x in range(1_000)]

# Create a generator for a given variant. This form is only really useful if
# you're writing utility functions, where you don't know what variant you will
# need up-front.
generator = afid.generator(afid.SHORT_VARIANT, "widget")
widget_id = generator.next()

# By default, generators use random.getrandbits. You can override it when
# creating a generator to provide your own source of randomness.
# The function must take a number of bits of randomness to generator, and return
# an integer with that number of random bits set in the lower bits.
generator = afid.long_generator("tx", rand=my_random_func)
tx_id = generator.next()
```

## Developing

### Setup
```sh
poetry install
```

### Formatting code
```sh
poetry run black **/*.py
```

### Running tests
```sh
poetry run pytest
```