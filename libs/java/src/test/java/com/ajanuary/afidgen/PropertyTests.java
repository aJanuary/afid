package com.ajanuary.afidgen;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.random.RandomGenerator;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.mockito.stubbing.Answer;

public class PropertyTests {

  private static Answer<Void> fillFromInt(int value) {
    return inv -> {
      byte[] bytes = inv.getArgument(0);
      for (int i = 0; i < bytes.length; i++) {
        bytes[i] = (byte) (value >>> (8 * (3 - (i & 3))));
      }
      return null;
    };
  }

  @Property
  void does_not_crash_when_generating_ids(@ForAll int value) {
    var random = mock(RandomGenerator.class);
    doAnswer(fillFromInt(value)).when(random).nextBytes(any(byte[].class));
    var generator = AFID.longGenerator("ent", random);

    assertDoesNotThrow(generator::next);
  }

  @Property
  void ids_only_include_base32_chars(@ForAll int value) {
    var random = mock(RandomGenerator.class);
    doAnswer(fillFromInt(value)).when(random).nextBytes(any(byte[].class));
    var generator = AFID.longGenerator("ent", random);

    var id = generator.next();

    assertThat(id)
        .matches(
            "^ent-[0123456789abcdefghjkmnpqrstvwxyz]{5}-[0123456789abcdefghjkmnpqrstvwxyz]{20}$");
  }

  @Property
  void short_ids_are_20_characters_long(@ForAll int value) {
    var random = mock(RandomGenerator.class);
    doAnswer(fillFromInt(value)).when(random).nextBytes(any(byte[].class));
    var generator = AFID.shortGenerator("ent", random);

    var id = generator.next();

    assertThat(id).hasLength(20);
  }

  @Property
  void long_ids_are_30_characters_long(@ForAll int value) {
    var random = mock(RandomGenerator.class);
    doAnswer(fillFromInt(value)).when(random).nextBytes(any(byte[].class));
    var generator = AFID.longGenerator("ent", random);

    var id = generator.next();

    assertThat(id).hasLength(30);
  }
}
