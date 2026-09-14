package io.github.byreshb.tql.rule;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import java.util.List;
import java.util.Set;

/** Recognises test methods of the common Java test frameworks by their annotations. */
public final class TestMethods {

  /** Simple names of annotations that mark a method as a test in JUnit 4, JUnit 5 and TestNG. */
  public static final Set<String> TEST_ANNOTATIONS =
      Set.of("Test", "ParameterizedTest", "RepeatedTest", "TestFactory", "TestTemplate");

  private TestMethods() {}

  /**
   * Whether a method carries a test annotation. Both simple ({@code @Test}) and qualified ({@code
   * @org.junit.jupiter.api.Test}) forms are recognised.
   *
   * @param method the method
   * @return true when it is a test method
   */
  public static boolean isTest(MethodDeclaration method) {
    for (AnnotationExpr annotation : method.getAnnotations()) {
      if (TEST_ANNOTATIONS.contains(annotation.getName().getIdentifier())) {
        return true;
      }
    }
    return false;
  }

  /**
   * All test methods in a file, in source order.
   *
   * @param unit the parsed file
   * @return the test methods
   */
  public static List<MethodDeclaration> in(CompilationUnit unit) {
    return unit.findAll(MethodDeclaration.class).stream().filter(TestMethods::isTest).toList();
  }

  /**
   * Whether a test method declares an expected exception through JUnit 4's {@code @Test(expected =
   * X.class)} or TestNG's {@code @Test(expectedExceptions = X.class)}.
   *
   * @param method the method
   * @return true when the framework itself asserts that an exception is thrown
   */
  public static boolean expectsException(MethodDeclaration method) {
    return method.getAnnotations().stream()
        .filter(a -> TEST_ANNOTATIONS.contains(a.getName().getIdentifier()))
        .flatMap(a -> a.toNormalAnnotationExpr().stream())
        .flatMap(a -> a.getPairs().stream())
        .anyMatch(
            pair ->
                pair.getNameAsString().equals("expected")
                    || pair.getNameAsString().equals("expectedExceptions"));
  }
}
