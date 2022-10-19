package com.ajanuary.afid;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.random.RandomGenerator;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

public class PropertyTests {
  @Property
  void does_not_crash_when_generating_ids(@ForAll long value) {
    var random = spy(RandomGenerator.class);
    when(random.nextLong()).thenReturn(value);
    var generator = AFID.longGenerator(random, "my-prefix");

    assertDoesNotThrow(generator::next);
  }

  @Property
  void ids_only_include_base32_chars(@ForAll long value) {
    var random = spy(RandomGenerator.class);
    when(random.nextLong()).thenReturn(value);
    var generator = AFID.longGenerator(random, "my-prefix");

    var id = generator.next();

    assertThat(id)
        .matches(
            "^my-prefix-[0123456789abcdefghjkmnpqrstvwxyz]{5}-[0123456789abcdefghjkmnpqrstvwxyz]{20}$");
  }

  @Property
  void short_ids_are_17_characters_long_after_prefix(@ForAll long value) {
    var random = spy(RandomGenerator.class);
    when(random.nextLong()).thenReturn(value);
    var generator = AFID.shortGenerator(random, "my-prefix");

    var id = generator.next();

    assertThat(id).hasLength(26);
  }

  @Property
  void long_ids_are_27_characters_long_after_prefix(@ForAll long value) {
    var random = spy(RandomGenerator.class);
    when(random.nextLong()).thenReturn(value);
    var generator = AFID.longGenerator(random, "my-prefix");

    var id = generator.next();

    assertThat(id).hasLength(36);
  }
}
