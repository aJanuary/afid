package com.ajanuary.afidgen;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.UniqueElements;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

public class ImplementationTests {

  private static Answer<Void> fillWith(byte value) {
    return inv -> {
      Arrays.fill((byte[]) inv.getArgument(0), value);
      return null;
    };
  }

  private static Answer<Void> fillFromInt(int value) {
    return inv -> {
      byte[] bytes = inv.getArgument(0);
      for (int i = 0; i < bytes.length; i++) {
        bytes[i] = (byte) (value >>> (8 * (3 - (i & 3))));
      }
      return null;
    };
  }

  // These tests know too much about the implementation, and are testing things that aren't
  // a part of the spec, but just happen to be how the functions are implemented.
  // For example, there isn't anything that dictates the order of the alphabet, so the random
  // source returning 0 could validly translate into an id of 12345-qwertyupasdfghjklzxc.
  // However, I don't know how to test things like not overflowing otherwise, and I'm not confident
  // enough in the bit-twiddling to not regression test them.

  @Test
  void works_with_lowest_value() {
    var random = mock(RandomGenerator.class);
    doAnswer(fillWith((byte) 0)).when(random).nextBytes(any(byte[].class));
    var generator = AFID.longGenerator("ent", random);

    var id = generator.next();

    assertThat(id).isEqualTo("ent-00000-00000000000000000000");
  }

  @Test
  void works_with_highest_value() {
    var random = mock(RandomGenerator.class);
    doAnswer(fillWith((byte) 0xFF)).when(random).nextBytes(any(byte[].class));
    var generator = AFID.longGenerator("ent", random);

    var id = generator.next();

    assertThat(id).isEqualTo("ent-zzzzz-zzzzzzzzzzzzzzzzzzzz");
  }

  @Property
  void distinct_random_numbers_yield_distinct_ids(
      @ForAll @UniqueElements List<@IntRange(max = 0x1FFFFFF) Integer> values) {
    List<String> ids =
        values.stream()
            .map(
                value -> {
                  var random = mock(RandomGenerator.class);
                  doAnswer(fillFromInt(value)).when(random).nextBytes(any(byte[].class));
                  var generator = AFID.longGenerator("ent", random);
                  return generator.next();
                })
            .collect(Collectors.toList());
    assertThat(ids).containsNoDuplicates();
  }
}
