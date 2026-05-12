package com.ajanuary.afidgen;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.random.RandomGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("#shortGenerator")
public class ShortGeneratorTests {
  private static final String B32_CLASS = "[0123456789abcdefghjkmnpqrstvwxyz]";

  @Test
  void exposes_prefix() {
    var generator = AFID.shortGenerator("ent");

    assertThat(generator.prefix()).isEqualTo("ent");
  }

  @Test
  void uses_random_if_provided() {
    var random = mock(RandomGenerator.class);
    var generator = AFID.shortGenerator("ent", random);

    generator.next();

    verify(random, atLeastOnce()).nextBytes(any(byte[].class));
  }

  @Test
  void errors_if_random_is_null() {
    var thrown = assertThrows(NullPointerException.class, () -> AFID.shortGenerator("ent", null));
    assertThat(thrown).hasMessageThat().isEqualTo("random");
  }

  @Test
  void stream_generates_ids() {
    var ids = AFID.shortGenerator("ent").stream().limit(3).toList();

    assertAll(
        () -> assertThat(ids).hasSize(3),
        () -> assertThat(ids.get(0)).matches("^ent-" + B32_CLASS + "{5}-" + B32_CLASS + "{10}$"),
        () -> assertThat(ids.get(1)).matches("^ent-" + B32_CLASS + "{5}-" + B32_CLASS + "{10}$"),
        () -> assertThat(ids.get(2)).matches("^ent-" + B32_CLASS + "{5}-" + B32_CLASS + "{10}$"));
  }
}
