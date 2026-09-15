package io.github.byreshb.tql.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordValidatorTest {

  @Test
  void acceptsAStrongPassword() {
    assertTrue(PasswordValidator.isStrong("Abcdefg1"));
  }

  @Test
  void rejectsAPasswordThatIsTooShort() {
    assertFalse(PasswordValidator.isStrong("Ab1"));
  }

  @Test
  void rejectsAPasswordWithoutADigit() {
    assertFalse(PasswordValidator.isStrong("Abcdefgh"));
  }

  @Test
  void rejectsAPasswordWithoutAnUpperCaseLetter() {
    assertFalse(PasswordValidator.isStrong("abcdefg1"));
  }

  @Test
  void rejectsANullPassword() {
    assertFalse(PasswordValidator.isStrong(null));
  }

  @Weak({"TQL001"})
  @Test
  void validatesLongPassword() {
    boolean result = PasswordValidator.isStrong("Abcdefg1");
    assertEquals(result, result);
  }

  @Weak({"TQL002"})
  @Test
  void checksPolicyConstant() {
    assertTrue(3 + 5 == 8);
  }

  @Weak({"TQL003", "TQL011"})
  @Test
  void checksStrongPassword() {
    PasswordValidator.isStrong("Abcdefg1");
  }

  @Weak({"TQL010"})
  @Test
  void rejectsShortPasswords() {
    assertFalse(PasswordValidator.isStrong("short"), "short password should be strong enough");
  }
}
