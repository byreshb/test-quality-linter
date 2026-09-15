package io.github.byreshb.tql.benchmark;

/** Checks whether a password meets a simple strength policy. */
public final class PasswordValidator {

  private PasswordValidator() {}

  /**
   * Whether a password is strong: at least 8 characters, with a digit and an upper-case letter.
   *
   * @param password the password, possibly {@code null}
   * @return true when it meets the policy
   */
  public static boolean isStrong(String password) {
    if (password == null || password.length() < 8) {
      return false;
    }
    boolean hasDigit = password.chars().anyMatch(Character::isDigit);
    boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
    return hasDigit && hasUpper;
  }
}
