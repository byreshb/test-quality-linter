package io.github.byreshb.tql.rule;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import java.util.Optional;
import java.util.Set;

/**
 * Recognises assertion calls of the common libraries: JUnit 4 and 5, AssertJ, Hamcrest, Google
 * Truth and Playwright's {@code assertThat}. All matching is by method name, so it works without a
 * classpath.
 */
public final class Assertions {

  /** Names of static assertion methods: JUnit, TestNG and Hamcrest. */
  public static final Set<String> STATIC_ASSERTIONS =
      Set.of(
          "assertEquals",
          "assertNotEquals",
          "assertArrayEquals",
          "assertIterableEquals",
          "assertLinesMatch",
          "assertSame",
          "assertNotSame",
          "assertTrue",
          "assertFalse",
          "assertNull",
          "assertNotNull",
          "assertThat",
          "assertThrows",
          "assertThrowsExactly",
          "assertDoesNotThrow",
          "assertAll",
          "assertTimeout",
          "assertTimeoutPreemptively",
          "assertInstanceOf",
          "assertEqualsNoOrder",
          "fail");

  /**
   * Names of the entry points of fluent assertion libraries (AssertJ, Truth, Playwright). A call
   * chain that starts at one of these is an assertion.
   */
  public static final Set<String> FLUENT_ENTRY_POINTS =
      Set.of(
          "assertThat",
          "assertThatThrownBy",
          "assertThatCode",
          "assertThatExceptionOfType",
          "assertThatIllegalArgumentException",
          "assertThatIllegalStateException",
          "assertThatNullPointerException",
          "assertThatNoException",
          "assertThatObject",
          "assertWithMessage",
          "assert_",
          "expect",
          "assertSoftly");

  private Assertions() {}

  /**
   * Whether a call is an assertion: a static assertion method, or the entry point of a fluent
   * assertion chain.
   *
   * @param call the method call
   * @return true when the call asserts something
   */
  public static boolean isAssertion(MethodCallExpr call) {
    String name = call.getNameAsString();
    return STATIC_ASSERTIONS.contains(name) || FLUENT_ENTRY_POINTS.contains(name);
  }

  /**
   * Finds the entry point of the fluent chain a call belongs to, for example the {@code
   * assertThat(x)} in {@code assertThat(x).as("...").isEqualTo(y)}.
   *
   * @param call any call in the chain
   * @return the innermost call whose name is a fluent entry point, or empty when the chain does not
   *     start at one
   */
  public static Optional<MethodCallExpr> fluentRoot(MethodCallExpr call) {
    MethodCallExpr current = call;
    while (true) {
      if (FLUENT_ENTRY_POINTS.contains(current.getNameAsString())) {
        return Optional.of(current);
      }
      Optional<Expression> scope = current.getScope();
      if (scope.isPresent() && scope.get() instanceof MethodCallExpr next) {
        current = next;
      } else {
        return Optional.empty();
      }
    }
  }
}
