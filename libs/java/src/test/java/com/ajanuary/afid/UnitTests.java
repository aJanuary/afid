package com.ajanuary.afid;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.random.RandomGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class UnitTests {
  @Nested
  @DisplayName("#randomShort")
  class randomShort {
    @Test
    void generates_ids_with_correct_shape() {
      var id = AFID.randomShort("my-prefix");

      assertThat(id).matches("^my-prefix-[a-z0-9]{5}-[a-z0-9]{10}$");
    }

    @Test
    void errors_if_prefix_is_empty() {
      var thrown = assertThrows(IllegalArgumentException.class, () -> AFID.randomShort(""));
      assertThat(thrown).hasMessageThat().isEqualTo("prefix must not be empty");
    }

    @Test
    void errors_if_prefix_too_long() {
      assertDoesNotThrow(() -> AFID.randomShort("123456789-123456789-123456789-1"));

      var thrown =
          assertThrows(
              IllegalArgumentException.class,
              () -> AFID.randomShort("123456789-123456789-123456789-12"));
      assertThat(thrown).hasMessageThat().isEqualTo("prefix cannot be longer than 31 characters");
    }

    @Test
    void errors_if_prefix_contains_invalid_chars() {
      assertDoesNotThrow(() -> AFID.randomShort("abcde1234"));
      assertDoesNotThrow(() -> AFID.randomShort("fghij5678"));
      assertDoesNotThrow(() -> AFID.randomShort("klmnopr90"));
      assertDoesNotThrow(() -> AFID.randomShort("stuvwxyz-"));

      var thrown1 = assertThrows(IllegalArgumentException.class, () -> AFID.randomShort("ABC"));
      assertThat(thrown1)
          .hasMessageThat()
          .isEqualTo("prefix can only contain lowercase letters, numbers, or -");

      var thrown2 = assertThrows(IllegalArgumentException.class, () -> AFID.randomShort("a_b_c"));
      assertThat(thrown2)
          .hasMessageThat()
          .isEqualTo("prefix can only contain lowercase letters, numbers, or -");
    }
  }

  @Nested
  @DisplayName("#randomLong")
  class randomLong {
    @Test
    void generates_ids_with_correct_shape() {
      var id = AFID.randomLong("my-prefix");

      assertThat(id).matches("^my-prefix-[a-z0-9]{5}-[a-z0-9]{20}$");
    }

    @Test
    void errors_if_prefix_is_empty() {
      var thrown = assertThrows(IllegalArgumentException.class, () -> AFID.randomLong(""));
      assertThat(thrown).hasMessageThat().isEqualTo("prefix must not be empty");
    }

    @Test
    void errors_if_prefix_too_long() {
      assertDoesNotThrow(() -> AFID.randomLong("123456789-123456789-1"));

      var thrown =
          assertThrows(
              IllegalArgumentException.class, () -> AFID.randomLong("123456789-123456789-12"));
      assertThat(thrown).hasMessageThat().isEqualTo("prefix cannot be longer than 21 characters");
    }

    @Test
    void errors_if_prefix_contains_invalid_chars() {
      assertDoesNotThrow(() -> AFID.randomLong("abcde1234"));
      assertDoesNotThrow(() -> AFID.randomLong("fghij5678"));
      assertDoesNotThrow(() -> AFID.randomLong("klmnopr90"));
      assertDoesNotThrow(() -> AFID.randomLong("stuvwxyz-"));

      var thrown1 = assertThrows(IllegalArgumentException.class, () -> AFID.randomLong("ABC"));
      assertThat(thrown1)
          .hasMessageThat()
          .isEqualTo("prefix can only contain lowercase letters, numbers, or -");

      var thrown2 = assertThrows(IllegalArgumentException.class, () -> AFID.randomLong("a_b_c"));
      assertThat(thrown2)
          .hasMessageThat()
          .isEqualTo("prefix can only contain lowercase letters, numbers, or -");
    }
  }

  @Nested
  @DisplayName("#shortGenerator")
  class shortGenerator {
    // We assume the code shares the implementation for these with randomShort,
    // so don't bother repeating their tests here.

    @Test
    void returns_generator_with_correct_properties() {
      var generator = AFID.shortGenerator("my-prefix");

      assertAll(
          () -> assertThat(generator).isNotNull(),
          () -> assertThat(generator.variant()).isEqualTo(Variant.SHORT),
          () -> assertThat(generator.prefix()).isEqualTo("my-prefix"));
    }

    @Test
    void generates_ids_with_correct_shape() {
      var generator = AFID.shortGenerator("my-prefix");

      var id = generator.next();

      assertThat(id).matches("^my-prefix-[a-z0-9]{5}-[a-z0-9]{10}$");
    }

    @Test
    void uses_random_if_provided() {
      var random = spy(RandomGenerator.class);
      var generator = AFID.shortGenerator(random, "my-prefix");

      generator.next();

      // The default implementation for all the methods on RandomGenerator delegate to nextLong,
      // so if it was used at all for randomness, nextLong will have been called.
      verify(random, atLeastOnce()).nextLong();
    }
  }

  @Nested
  @DisplayName("#longGenerator")
  class longGenerator {
    // We assume the code shares the implementation for these with randomLong,
    // so don't bother repeating their tests here.

    @Test
    void returns_generator_with_correct_properties() {
      var generator = AFID.longGenerator("my-prefix");

      assertAll(
          () -> assertThat(generator).isNotNull(),
          () -> assertThat(generator.variant()).isEqualTo(Variant.LONG),
          () -> assertThat(generator.prefix()).isEqualTo("my-prefix"));
    }

    @Test
    void generates_ids_with_correct_shape() {
      var generator = AFID.longGenerator("my-prefix");

      var id = generator.next();

      assertThat(id).matches("^my-prefix-[a-z0-9]{5}-[a-z0-9]{20}$");
    }

    @Test
    void uses_random_if_provided() {
      var random = spy(RandomGenerator.class);
      var generator = AFID.longGenerator(random, "my-prefix");

      generator.next();

      // The default implementation for all the methods on RandomGenerator delegate to nextLong,
      // so if it was used at all for randomness, nextLong will have been called.
      verify(random, atLeastOnce()).nextLong();
    }
  }

  @Nested
  @DisplayName("#generator")
  class generator {
    @Test
    void returns_generator_with_correct_properties() {
      var shortGenerator = AFID.generator(Variant.SHORT, "my-prefix");
      var longGenerator = AFID.generator(Variant.LONG, "my-prefix");

      assertAll(
          () -> assertThat(shortGenerator).isNotNull(),
          () -> assertThat(shortGenerator.variant()).isEqualTo(Variant.SHORT),
          () -> assertThat(shortGenerator.prefix()).isEqualTo("my-prefix"),
          () -> assertThat(longGenerator).isNotNull(),
          () -> assertThat(longGenerator.variant()).isEqualTo(Variant.LONG),
          () -> assertThat(longGenerator.prefix()).isEqualTo("my-prefix"));
    }

    @Test
    void generates_ids_with_correct_shape() {
      var shortGenerator = AFID.generator(Variant.SHORT, "my-prefix");
      var longGenerator = AFID.generator(Variant.LONG, "my-prefix");

      var shortId = shortGenerator.next();
      var longId = longGenerator.next();

      assertAll(
          () -> assertThat(shortId).matches("^my-prefix-[a-z0-9]{5}-[a-z0-9]{10}$"),
          () -> assertThat(longId).matches("^my-prefix-[a-z0-9]{5}-[a-z0-9]{20}$"));
    }

    @Test
    void uses_random_if_provided() {
      var shortRandom = spy(RandomGenerator.class);
      var shortGenerator = AFID.generator(shortRandom, Variant.SHORT, "my-prefix");
      var longRandom = spy(RandomGenerator.class);
      var longGenerator = AFID.generator(longRandom, Variant.LONG, "my-prefix");

      shortGenerator.next();
      longGenerator.next();

      // The default implementation for all the methods on RandomGenerator delegate to nextLong,
      // so if it was used at all for randomness, nextLong will have been called.
      verify(shortRandom, atLeastOnce()).nextLong();
      verify(longRandom, atLeastOnce()).nextLong();
    }
  }
}
