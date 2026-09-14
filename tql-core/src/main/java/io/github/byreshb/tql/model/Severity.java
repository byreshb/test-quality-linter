package io.github.byreshb.tql.model;

/** How serious a finding is. The order of the constants is the order of severity. */
public enum Severity {
  /** Worth a look, but the test may still be fine. */
  INFO,
  /** The test very likely proves less than it appears to. */
  WARN,
  /** The test proves nothing. */
  ERROR;

  /**
   * Whether this severity is at least as serious as {@code other}.
   *
   * @param other the severity to compare against
   * @return true when this severity is equal to or more serious than {@code other}
   */
  public boolean atLeast(Severity other) {
    return compareTo(other) >= 0;
  }

  /**
   * Parses a severity name case-insensitively, accepting the common aliases {@code WARNING} and
   * {@code INFORMATION}.
   *
   * @param name the name, for example {@code warn} or {@code ERROR}
   * @return the severity
   * @throws IllegalArgumentException when the name is not a severity
   */
  public static Severity parse(String name) {
    String upper = name.trim().toUpperCase(java.util.Locale.ROOT);
    switch (upper) {
      case "WARNING":
        return WARN;
      case "INFORMATION":
        return INFO;
      default:
        return valueOf(upper);
    }
  }
}
