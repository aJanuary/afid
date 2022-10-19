# afid

A Java library for generating [afid ids](https://github.com/aJanuary/afid).

## Installing

### Gradle
```groovy
implementation 'com.ajanuary:afid:0.1.0'
```

### Maven
```xml
<dependency>
    <groupId>com.ajanuary</groupId>
    <artifactId>afid</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Usage

```java
import com.ajanuary.afid.AFID;
import com.ajanuary.afid.AFID.Variant;

// Generate a short ID with the prefix "resource".
String resourceId = AFID.randomShort("resource");

// Generate a long ID with the prefix "event".
String eventId = AFID.randomLong("event");

// Create a generator. This is more efficient to create multiple afids, because
// it only needs to validate the prefix once when you create the generator,
// instead of every time you generate and id.
AFID.Generator generator = AFID.longGenerator("event");
List<String> eventIds = new ArrayList<>();
for (int i = 0; i < 1_000; i++) {
  eventIds.add(generator.next());
}

// Create a generator for a given variant. This form is only really useful if
// you're writing utility functions, where you don't know what variant you will
// need up-front.
AFID.Generator generator = AFID.generator(Variant.SHORT, "widget");
String widgetId = generator.next();

// By default, generators use java.security.SecureRandom. You can override it
// when creating a generator to provide your own source of randomness.
AFID.Generator generator = AFID.longGenerator(new Random(), "tx")
String txId = generator.next();
```

## Developing

### Formatting code
```sh
./gradlew spotlessApply
```

### Running checks
```sh
./gradlew check
```