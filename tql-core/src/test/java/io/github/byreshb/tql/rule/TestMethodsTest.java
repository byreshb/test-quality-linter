package io.github.byreshb.tql.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.jupiter.api.Test;

class TestMethodsTest {

  private static final String SOURCE =
      """
      class T {
        @org.junit.jupiter.api.Test
        void qualified() {}

        @Test(expected = IllegalStateException.class)
        public void junit4() {}

        @ParameterizedTest
        void parameterized(int x) {}

        @Test(expectedExceptions = RuntimeException.class)
        public void testng() {}

        void helper() {}

        @Override
        public String toString() { return ""; }
      }
      """;

  @Test
  void recognisesTestAnnotationsInSimpleAndQualifiedForm() {
    CompilationUnit unit = StaticJavaParser.parse(SOURCE);
    assertThat(TestMethods.in(unit))
        .extracting(MethodDeclaration::getNameAsString)
        .containsExactly("qualified", "junit4", "parameterized", "testng");
  }

  @Test
  void recognisesExpectedExceptionAttributes() {
    CompilationUnit unit = StaticJavaParser.parse(SOURCE);
    assertThat(TestMethods.in(unit))
        .filteredOn(TestMethods::expectsException)
        .extracting(MethodDeclaration::getNameAsString)
        .containsExactly("junit4", "testng");
  }
}
