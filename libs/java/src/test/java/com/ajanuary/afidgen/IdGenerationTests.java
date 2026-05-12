package com.ajanuary.afidgen;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class IdGenerationTests {
  private static final String B32_CLASS = "[0123456789abcdefghjkmnpqrstvwxyz]";

  static Stream<Arguments> factories() {
    return Stream.of(
        Arguments.of("randomShort", (Function<String, String>) AFID::randomShort, 10),
        Arguments.of("randomLong", (Function<String, String>) AFID::randomLong, 20),
        Arguments.of(
            "shortGenerator", (Function<String, String>) p -> AFID.shortGenerator(p).next(), 10),
        Arguments.of(
            "longGenerator", (Function<String, String>) p -> AFID.longGenerator(p).next(), 20),
        Arguments.of(
            "shortGenerator.stream",
            (Function<String, String>)
                p -> AFID.shortGenerator(p).stream().findFirst().orElseThrow(),
            10),
        Arguments.of(
            "longGenerator.stream",
            (Function<String, String>)
                p -> AFID.longGenerator(p).stream().findFirst().orElseThrow(),
            20));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("factories")
  void generates_ids_with_correct_shape(
      String name, Function<String, String> factory, int suffixLength) {
    var id = factory.apply("ent");
    assertThat(id).matches("^ent-" + B32_CLASS + "{5}-" + B32_CLASS + "{" + suffixLength + "}$");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("factories")
  void errors_if_prefix_is_null(String name, Function<String, String> factory, int suffixLength) {
    var thrown = assertThrows(NullPointerException.class, () -> factory.apply(null));
    assertThat(thrown).hasMessageThat().isEqualTo("prefix");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("factories")
  void errors_if_prefix_is_empty(String name, Function<String, String> factory, int suffixLength) {
    var thrown = assertThrows(IllegalArgumentException.class, () -> factory.apply(""));
    assertThat(thrown).hasMessageThat().isEqualTo("prefix must be exactly 3 characters");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("factories")
  void errors_if_prefix_too_short(String name, Function<String, String> factory, int suffixLength) {
    var thrown = assertThrows(IllegalArgumentException.class, () -> factory.apply("12"));
    assertThat(thrown).hasMessageThat().isEqualTo("prefix must be exactly 3 characters");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("factories")
  void errors_if_prefix_too_long(String name, Function<String, String> factory, int suffixLength) {
    var thrown = assertThrows(IllegalArgumentException.class, () -> factory.apply("1234"));
    assertThat(thrown).hasMessageThat().isEqualTo("prefix must be exactly 3 characters");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("factories")
  void errors_if_prefix_contains_uppercase(
      String name, Function<String, String> factory, int suffixLength) {
    var thrown = assertThrows(IllegalArgumentException.class, () -> factory.apply("ABC"));
    assertThat(thrown)
        .hasMessageThat()
        .isEqualTo("prefix can only contain lowercase letters and numbers");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("factories")
  void errors_if_prefix_contains_punctuation(
      String name, Function<String, String> factory, int suffixLength) {
    var thrown = assertThrows(IllegalArgumentException.class, () -> factory.apply("a_b"));
    assertThat(thrown)
        .hasMessageThat()
        .isEqualTo("prefix can only contain lowercase letters and numbers");
  }
}
