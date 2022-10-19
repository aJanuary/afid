package com.ajanuary.afid;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.UniqueElements;
import org.junit.jupiter.api.Test;

public class ImplementationTests {
  // These tests know too much about the implementation, and are testing things that aren't
  // a part of the spec, but just happen to be how the functions are implemented.
  // For example, there isn't anything that dictates the order of the alphabet, so the random
  // source returning 0 could validly translate into an id of 12345-qwertyupasdfghjklzxc.
  // However, I don't know how to test things like not overflowing otherwise, and I'm not confident
  // enough in the bit-twiddling to not regression test them.

  @Test
  void works_with_lowest_value() {
    var random = spy(RandomGenerator.class);
    when(random.nextLong()).thenReturn(0L);
    var generator = AFID.longGenerator(random, "my-prefix");

    var id = generator.next();

    assertThat(id).isEqualTo("my-prefix-00000-00000000000000000000");
  }

  @Test
  void works_with_highest_value() {
    var random = spy(RandomGenerator.class);
    when(random.nextLong()).thenReturn(0x01FFFFFF00000000L);
    var generator = AFID.longGenerator(random, "my-prefix");

    var id = generator.next();

    assertThat(id).isEqualTo("my-prefix-zzzzz-zzzzzzzzzzzzzzzzzzzz");
  }

  @Property
  void distinct_random_numbers_yield_distinct_ids(
      @ForAll @UniqueElements List<@IntRange(max = 0x1FFFFFF) Integer> values) {
    List<String> ids =
        values.stream()
            .map(
                value -> {
                  var random = spy(RandomGenerator.class);
                  when(random.nextInt()).thenReturn(value);
                  var generator = AFID.longGenerator(random, "my-prefix");
                  return generator.next();
                })
            .collect(Collectors.toList());
    assertThat(ids).containsNoDuplicates();
  }
}
