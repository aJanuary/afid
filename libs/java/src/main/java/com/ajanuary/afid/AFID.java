package com.ajanuary.afid;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

/**
 * Generate random afid ids.
 *
 * <p>afids are designed to be human-friendly random identifiers. They are between 18 and 48
 * characters long and may contain a-z (lowercase), 0-9 and -. They begin with a prefix that you can
 * use to identify what the ID is for.
 *
 * <p>There are two flavours of afids: short and long. Which to use depends on your use case.
 *
 * <p>Short afids are designed for scenarios where you are generating less than 10 thousand ids per
 * hour. Short afids allow for longer prefixes. e.g. <code>entity-2sed3-1p3dpw40ds</code>
 *
 * <p>Long afids are designed for scenarios where you are generating between 10 thousand and a
 * billion ids per hour, at the cost of longer afids and less space available for prefixes. e.g.
 * <code>event-d3v2s-pl2m300zxs24mspqer3s</code>
 *
 * <p>Convenience methods {@link #randomShort(String)} and {@link #randomLong(String)} exist to
 * allow you to generate ids without much ceremony. However, if you need to generate a lot of ids,
 * or want control over the source of randomness, then first create a generator using {@link
 * #shortGenerator} or {@link #longGenerator}. These will only validate the prefix once when the
 * generator is created, instead of every time an id is generated.
 */
public class AFID {

  private AFID() {
    // Prevent construction
  }

  /**
   * Create a generator that will generate short afids with the given prefix.
   *
   * <p>Short afids are designed for scenarios where you are generating less than 10 thousand afids
   * per hour. They allow for shorter afids and longer prefixes.
   *
   * @param prefix string to prefix at the beginning of the afid ids. Must be between 1 and 31
   *     characters consisting of a-z (lowercase), 0-9 and -.
   * @return a generator that will generate short afids with the given prefix
   */
  public static Generator shortGenerator(String prefix) {
    return new Generator(new SecureRandom(), Variant.SHORT, prefix);
  }

  /**
   * Create a generator that will generate short afids with the given prefix.
   *
   * <p>Short afids are designed for scenarios where you are generating less than 10 thousand afids
   * per hour. They allow for shorter afids and longer prefixes.
   *
   * @param random source of randomness to use when generating ids
   * @param prefix string to prefix at the beginning of the afid ids. Must be between 1 and 31
   *     characters consisting of a-z (lowercase), 0-9 and -.
   * @return a generator that will generate short afids with the given prefix
   */
  public static Generator shortGenerator(RandomGenerator random, String prefix) {
    return new Generator(random, Variant.SHORT, prefix);
  }

  /**
   * Create a generator that will generate long afids with the given prefix.
   *
   * <p>Long afids is designed for scenarios where you are generating between 10 thousand and a
   * billion afids per hour, at the cost of longer afids and less space available for prefixes.
   *
   * @param prefix string to prefix at the beginning of the afid ids. Must be between 1 and 21
   *     characters consisting of a-z (lowercase), 0-9 and -.
   * @return a generator that will generate long afids with the given prefix
   */
  public static Generator longGenerator(String prefix) {
    return new Generator(new SecureRandom(), Variant.LONG, prefix);
  }

  /**
   * Create a generator that will generate long afids with the given prefix.
   *
   * <p>Long afids is designed for scenarios where you are generating between 10 thousand and a
   * billion afids per hour, at the cost of longer afids and less space available for prefixes.
   *
   * @param random source of randomness to use when generating ids
   * @param prefix string to prefix at the beginning of the afid ids. Must be between 1 and 21
   *     characters consisting of a-z (lowercase), 0-9 and -.
   * @return a generator that will generate long afids with the given prefix
   */
  public static Generator longGenerator(RandomGenerator random, String prefix) {
    return new Generator(random, Variant.LONG, prefix);
  }

  /**
   * Return a generator that will generate afids with a given variant and prefix.
   *
   * @param variant variant of the afids to generate
   * @param prefix string to prefix to the beginning of the afid ids. Must be between 1 and 21
   *     characters for long afids, or between 1 and 31 for short afids. May contain only a-z
   *     (lowercase), 0-9 and -.
   * @return a generator that will generate afids of the given variant with the given prefix
   */
  public static Generator generator(Variant variant, String prefix) {
    return new Generator(new SecureRandom(), variant, prefix);
  }

  /**
   * Return a generator that will generate afids with a given variant and prefix.
   *
   * @param random source of randomness to use when generating ids
   * @param variant variant of the afids to generate
   * @param prefix string to prefix to the beginning of the afid ids. Must be between 1 and 21
   *     characters for long afids, or between 1 and 31 for short afids. May contain only a-z
   *     (lowercase), 0-9 and -.
   * @return a generator that will generate afids of the given variant with the given prefix
   */
  public static Generator generator(RandomGenerator random, Variant variant, String prefix) {
    return new Generator(random, variant, prefix);
  }

  /**
   * Return a short afid with the given prefix.
   *
   * <p>Short afids are designed for scenarios where you are generating less than 10 thousand afids
   * per hour. They allow for shorter afids and longer prefixes.
   *
   * <p>If you need to generate a lot of ids, consider using {@link #shortGenerator(String)}, which
   * will validate the prefix once up-front instead of every call.
   *
   * @param prefix string to prefix at the beginning of the afid. Must be between 1 and 21
   *     characters consisting of a-z (lowercase), 0-9 and -.
   * @return a short afid with the given prefix
   */
  public static String randomShort(String prefix) {
    return shortGenerator(prefix).next();
  }

  /**
   * Return a long afid with the given prefix.
   *
   * <p>Long afids is designed for scenarios where you are generating between 10 thousand and a
   * billion afids per hour, at the cost of longer afids and less space available for prefixes.
   *
   * <p>If you need to generate a lot of ids, consider using {@link #longGenerator(String)}, which
   * will validate the prefix once up-front instead of every call.
   *
   * @param prefix string to prefix at the beginning of the afid. Must be between 1 and 9 characters
   *     consisting of a-z (lowercase), 0-9 and -.
   * @return a long afid with the given prefix
   */
  public static String randomLong(String prefix) {
    return longGenerator(prefix).next();
  }
}
