package io.github.byreshb.tql.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.MethodCallExpr;
import org.junit.jupiter.api.Test;

class AssertionsTest {

  private static MethodCallExpr call(String expression) {
    return StaticJavaParser.parseExpression(expression).asMethodCallExpr();
  }

  @Test
  void recognisesStaticAndFluentAssertions() {
    assertThat(Assertions.isAssertion(call("assertEquals(1, x)"))).isTrue();
    assertThat(Assertions.isAssertion(call("Assertions.assertEquals(1, x)"))).isTrue();
    assertThat(Assertions.isAssertion(call("assertThat(x)"))).isTrue();
    assertThat(Assertions.isAssertion(call("assertThatThrownBy(() -> x)"))).isTrue();
    assertThat(Assertions.isAssertion(call("fail(\"boom\")"))).isTrue();
    assertThat(Assertions.isAssertion(call("service.save(x)"))).isFalse();
    assertThat(Assertions.isAssertion(call("verify(mock).save(x)"))).isFalse();
    assertThat(Assertions.isAssertion(call("service.fail()"))).isFalse();
    assertThat(Assertions.isAssertion(call("this.assertThat(x)"))).isFalse();
    assertThat(Assertions.isAssertion(call("org.junit.Assert.assertTrue(x)"))).isTrue();
    assertThat(Assertions.hasStaticLikeScope(call("foo().assertThat(x)"))).isFalse();
  }

  @Test
  void findsTheRootOfAFluentChain() {
    MethodCallExpr terminal = call("assertThat(x).as(\"d\").isEqualTo(y)");
    assertThat(Assertions.fluentRoot(terminal))
        .map(MethodCallExpr::getNameAsString)
        .contains("assertThat");
    assertThat(Assertions.fluentRoot(call("x.foo().bar()"))).isEmpty();
    assertThat(Assertions.fluentRoot(call("assertThat(x)"))).isPresent();
  }
}
