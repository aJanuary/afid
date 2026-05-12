package com.ajanuary.afidgen;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

/**
 * Generate random afid ids.
 *
 * <p>afids are human-friendly random identifiers. They are either 20 or 30 characters long and may
 * contain a-z (lowercase), 0-9 and -. They begin with a 3-character prefix that you can use to
 * identify what the ID is for.
 *
 * <p>There are two flavours of afids: short and long. Which to use depends on your use case.
 *
 * <p>Short afids are designed for scenarios where you are generating less than 10 thousand afids
 * per hour. They carry 75 bits of randomness. e.g. <code>ent-2sed3-1p3dpw40ds</code>
 *
 * <p>Long afids are designed for scenarios where you are generating between 10 thousand and a
 * trillion afids per hour, at the cost of longer ids. They carry 125 bits of randomness. e.g.
 * <code>
 * ent-d3v2s-pp2m300zxs24mspqer3s</code>
 *
 * <p>Convenience methods {@link #randomShort(String)} and {@link #randomLong(String)} exist to
 * allow you to generate ids without much ceremony. However, if you need to generate a lot of ids,
 * or want control over the source of randomness, then first create a generator using {@link
 * #shortGenerator} or {@link #longGenerator}. These will only validate the prefix once when the
 * generator is created, instead of every time an id is generated.
 *
 * <p>Thread-safety: {@link #randomShort(String)}, {@link #randomLong(String)}, and the no-{@link
 * RandomGenerator} factory methods ({@link #shortGenerator(String)}, {@link
 * #longGenerator(String)}) are all thread-safe — they use {@link SecureRandom} internally. The
 * overloads that take a {@link RandomGenerator} produce a {@link Generator} whose thread-safety
 * follows the supplied source; see {@link Generator}'s class javadoc.
 */
public final class AFID {

  private AFID() {
    // Prevent construction
  }

  // Lazy initialization so SecureRandom is only created if default static APIs are used.
  private static final class SecureRandomHolder {
    private static final SecureRandom INSTANCE = new SecureRandom();
  }

  private static SecureRandom defaultRandom() {
    return SecureRandomHolder.INSTANCE;
  }

  /**
   * Create a generator that will generate short afids with the given prefix.
   *
   * <p>Short afids are designed for scenarios where you are generating less than 10 thousand afids
   * per hour. They carry 75 bits of randomness.
   *
   * @param prefix string to prefix at the beginning of the afid ids. Must be 3 characters
   *     consisting of a-z (lowercase) and 0-9.
   * @return a generator that will generate short afids with the given prefix
   */
  public static Generator shortGenerator(String prefix) {
    return new Generator(defaultRandom(), prefix, Generators.SHORT_SUFFIX_LEN);
  }

  /**
   * Create a generator that will generate short afids with the given prefix.
   *
   * <p>Short afids are designed for scenarios where you are generating less than 10 thousand afids
   * per hour. They carry 75 bits of randomness.
   *
   * @param prefix string to prefix at the beginning of the afid ids. Must be 3 characters
   *     consisting of a-z (lowercase) and 0-9.
   * @param random source of randomness used to generate ids. The no-{@code RandomGenerator}
   *     overload uses {@link SecureRandom}; pass a different source here if you want to trade
   *     security for speed (e.g. {@link java.util.concurrent.ThreadLocalRandom}) or for
   *     reproducibility (e.g. a seeded {@link java.util.Random}).
   * @return a generator that will generate short afids with the given prefix
   */
  public static Generator shortGenerator(String prefix, RandomGenerator random) {
    return new Generator(random, prefix, Generators.SHORT_SUFFIX_LEN);
  }

  /**
   * Create a generator that will generate long afids with the given prefix.
   *
   * <p>Long afids are designed for scenarios where you are generating between 10 thousand and a
   * trillion afids per hour, at the cost of longer ids. They carry 125 bits of randomness.
   *
   * @param prefix string to prefix at the beginning of the afid ids. Must be 3 characters
   *     consisting of a-z (lowercase) and 0-9.
   * @return a generator that will generate long afids with the given prefix
   */
  public static Generator longGenerator(String prefix) {
    return new Generator(defaultRandom(), prefix, Generators.LONG_SUFFIX_LEN);
  }

  /**
   * Create a generator that will generate long afids with the given prefix.
   *
   * <p>Long afids are designed for scenarios where you are generating between 10 thousand and a
   * trillion afids per hour, at the cost of longer ids. They carry 125 bits of randomness.
   *
   * @param prefix string to prefix at the beginning of the afid ids. Must be 3 characters
   *     consisting of a-z (lowercase) and 0-9.
   * @param random source of randomness used to generate ids. The no-{@code RandomGenerator}
   *     overload uses {@link SecureRandom}; pass a different source here if you want to trade
   *     security for speed (e.g. {@link java.util.concurrent.ThreadLocalRandom}) or for
   *     reproducibility (e.g. a seeded {@link java.util.Random}).
   * @return a generator that will generate long afids with the given prefix
   */
  public static Generator longGenerator(String prefix, RandomGenerator random) {
    return new Generator(random, prefix, Generators.LONG_SUFFIX_LEN);
  }

  /**
   * Return a short afid with the given prefix.
   *
   * <p>Short afids are designed for scenarios where you are generating less than 10 thousand afids
   * per hour. They carry 75 bits of randomness.
   *
   * <p>If you need to generate a lot of ids, consider using {@link #shortGenerator(String)}, which
   * will validate the prefix once up-front instead of every call.
   *
   * @param prefix string to prefix at the beginning of the afid. Must be 3 characters consisting of
   *     a-z (lowercase) and 0-9.
   * @return a short afid with the given prefix
   */
  public static String randomShort(String prefix) {
    Generators.validatePrefix(prefix);
    return Generators.generate(defaultRandom(), prefix, Generators.SHORT_SUFFIX_LEN);
  }

  /**
   * Return a long afid with the given prefix.
   *
   * <p>Long afids are designed for scenarios where you are generating between 10 thousand and a
   * trillion afids per hour, at the cost of longer ids. They carry 125 bits of randomness.
   *
   * <p>If you need to generate a lot of ids, consider using {@link #longGenerator(String)}, which
   * will validate the prefix once up-front instead of every call.
   *
   * @param prefix string to prefix at the beginning of the afid. Must be 3 characters consisting of
   *     a-z (lowercase) and 0-9.
   * @return a long afid with the given prefix
   */
  public static String randomLong(String prefix) {
    Generators.validatePrefix(prefix);
    return Generators.generate(defaultRandom(), prefix, Generators.LONG_SUFFIX_LEN);
  }
}
