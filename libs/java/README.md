# afidgen

A Java library for generating [afid ids](https://github.com/ajanuary/afid).

Published to Maven Central as
[`com.ajanuary:afidgen`](https://central.sonatype.com/artifact/com.ajanuary/afidgen).
Each release ships the main jar plus sources and Javadoc jars.

## Requirements

Java 17 or later.

## Installing

### Gradle
```groovy
implementation 'com.ajanuary:afidgen:1.0.0'
```

### Maven
```xml
<dependency>
    <groupId>com.ajanuary</groupId>
    <artifactId>afidgen</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage

```java
import com.ajanuary.afidgen.AFID;
import com.ajanuary.afidgen.Generator;

// Generate a short ID with the prefix "res".
String resourceId = AFID.randomShort("res");

// Generate a long ID with the prefix "evt".
String eventId = AFID.randomLong("evt");

// Create a generator. This is more efficient when generating multiple afids,
// because it only needs to validate the prefix once when you create the
// generator, instead of every time you generate an ID.
Generator eventIdGenerator = AFID.longGenerator("evt");
List<String> eventIds = new ArrayList<>();
for (int i = 0; i < 1_000; i++) {
  eventIds.add(eventIdGenerator.next());
}

// Generators expose a Stream too — bound it with .limit().
List<String> moreEventIds = eventIdGenerator.stream().limit(1_000).toList();

// By default, generators use java.security.SecureRandom. You can override it
// when creating a generator to provide your own source of randomness.
Generator txIdGenerator = AFID.longGenerator("txi", new Random());
String txId = txIdGenerator.next();
```

## Performance

For bulk generation, build a generator once with `AFID.shortGenerator`
or `AFID.longGenerator` and reuse it. The prefix is validated at
construction, so each call only pays for the randomness draw and the
encoding.

`AFID.randomShort` / `AFID.randomLong` are convenience helpers that
build a fresh generator on every call. Use them for ad-hoc IDs; reach
for the factories in tight loops.

Run benchmarks with `./gradlew jmh`.

## Concurrency

A `Generator` is safe for concurrent use if and only if the underlying
`RandomGenerator` is. Generators built without a `RandomGenerator` use
`java.security.SecureRandom` and are thread-safe. `Random` and
`ThreadLocalRandom` are also thread-safe; `SplittableRandom` is not.

## Developing

From `libs/java/`:

```sh
./gradlew check          # spotless check, tests
./gradlew test           # tests only
./gradlew spotlessApply  # apply formatting
./gradlew jmh            # benchmarks
```
