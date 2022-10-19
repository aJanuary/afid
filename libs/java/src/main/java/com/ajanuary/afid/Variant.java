package com.ajanuary.afid;

/** The variant of afid. */
public enum Variant {
  /**
   * Short afids are designed for scenarios where you are generating less than 10 thousand ids per
   * hour.
   *
   * <p>Short afids allow for prefixes of up to 19 characters.
   */
  SHORT(10),
  /**
   * Long afids are designed for scenarios where you are generating between 10 thousand and a
   * billion ids per hour.
   *
   * <p>Long afids allow for a prefix of up to 9 characters.
   */
  LONG(20);

  private int suffixLength;

  Variant(int suffixLength) {
    this.suffixLength = suffixLength;
  }

  /**
   * @return the length of the suffix for this type
   */
  public int suffixLength() {
    return suffixLength;
  }
}
