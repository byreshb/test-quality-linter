package io.github.byreshb.tql.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConstantExpressionsTest {

  private static final CompilationUnit UNIT =
      StaticJavaParser.parse(
          """
          class T {
            private static final int LIMIT = 10;
            static final String NAME = "x" + LIMIT;
            private static final int SELF = SELF;
            private final int notStatic = 3;
            private static final int FROM_CALL = compute();
            private int mutable = 4;
          }
          """);

  private static boolean constant(String expression) {
    Expression e = StaticJavaParser.parseExpression(expression);
    return ConstantExpressions.isConstant(e, UNIT);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "1",
        "\"a\"",
        "'c'",
        "true",
        "null",
        "-1",
        "(1 + 2) * 3",
        "\"a\" + \"b\"",
        "1 < 2 ? \"x\" : \"y\"",
        "(long) 5",
        "LIMIT",
        "T.LIMIT",
        "this.LIMIT",
        "NAME",
        "notStatic",
        "LIMIT * 2 + 1"
      })
  void recognisesConstants(String expression) {
    assertThat(constant(expression)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "x",
        "x + 1",
        "foo()",
        "new Object()",
        "SELF",
        "FROM_CALL",
        "mutable",
        "other.LIMIT",
        "1 < x ? 1 : 2",
        "(int) x"
      })
  void rejectsNonConstants(String expression) {
    assertThat(constant(expression)).isFalse();
  }

  @Test
  void unwrapsParentheses() {
    Expression e = StaticJavaParser.parseExpression("((x))");
    assertThat(ConstantExpressions.unwrap(e).toString()).isEqualTo("x");
  }
}
