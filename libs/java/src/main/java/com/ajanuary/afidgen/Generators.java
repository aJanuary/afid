package com.ajanuary.afidgen;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.random.RandomGenerator;
import java.util.regex.Pattern;

final class Generators {

  static final int SHORT_SUFFIX_LEN = 10;
  static final int LONG_SUFFIX_LEN = 20;

  private static final int PREFIX_LEN = 3;
  private static final int TAG_LEN = 5;
  private static final Pattern PREFIX_REGEX = Pattern.compile("^[a-z0-9]{" + PREFIX_LEN + "}$");
  private static final byte[] BASE32_ALPHABET =
      new byte[] {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
        'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'v', 'w', 'x', 'y', 'z'
      };

  private Generators() {}

  static void validatePrefix(String prefix) {
    Objects.requireNonNull(prefix, "prefix");

    if (prefix.length() != PREFIX_LEN) {
      throw new IllegalArgumentException("prefix must be exactly " + PREFIX_LEN + " characters");
    }

    if (!PREFIX_REGEX.matcher(prefix).matches()) {
      throw new IllegalArgumentException("prefix can only contain lowercase letters and numbers");
    }
  }

  static String generate(RandomGenerator random, String prefix, int suffixLength) {
    int randomCharsToUse = TAG_LEN + suffixLength;
    int randomGroupCount = (randomCharsToUse + 7) / 8;
    int randomByteLen = randomGroupCount * 5;
    int totalLen = PREFIX_LEN + 1 + TAG_LEN + 1 + suffixLength;

    byte[] rnd = new byte[randomByteLen];
    random.nextBytes(rnd);

    byte[] decoded = new byte[randomGroupCount * 8];
    for (int g = 0; g < randomGroupCount; g++) {
      extract8Bytes(rnd, g * 5, decoded, g * 8);
    }

    byte[] idBytes = new byte[totalLen];
    for (int i = 0; i < PREFIX_LEN; i++) {
      idBytes[i] = (byte) prefix.charAt(i);
    }
    idBytes[PREFIX_LEN] = (byte) '-';
    System.arraycopy(decoded, 0, idBytes, PREFIX_LEN + 1, TAG_LEN);
    idBytes[PREFIX_LEN + 1 + TAG_LEN] = (byte) '-';
    System.arraycopy(
        decoded, TAG_LEN, idBytes, PREFIX_LEN + 1 + TAG_LEN + 1, randomCharsToUse - TAG_LEN);
    return new String(idBytes, StandardCharsets.ISO_8859_1);
  }

  private static void extract8Bytes(byte[] src, int s, byte[] dst, int d) {
    int b0 = src[s] & 0xFF;
    int b1 = src[s + 1] & 0xFF;
    int b2 = src[s + 2] & 0xFF;
    int b3 = src[s + 3] & 0xFF;
    int b4 = src[s + 4] & 0xFF;
    dst[d] = BASE32_ALPHABET[(b0 >>> 3) & 0x1F];
    dst[d + 1] = BASE32_ALPHABET[((b0 << 2) | (b1 >>> 6)) & 0x1F];
    dst[d + 2] = BASE32_ALPHABET[(b1 >>> 1) & 0x1F];
    dst[d + 3] = BASE32_ALPHABET[((b1 << 4) | (b2 >>> 4)) & 0x1F];
    dst[d + 4] = BASE32_ALPHABET[((b2 << 1) | (b3 >>> 7)) & 0x1F];
    dst[d + 5] = BASE32_ALPHABET[(b3 >>> 2) & 0x1F];
    dst[d + 6] = BASE32_ALPHABET[((b3 << 3) | (b4 >>> 5)) & 0x1F];
    dst[d + 7] = BASE32_ALPHABET[b4 & 0x1F];
  }
}
