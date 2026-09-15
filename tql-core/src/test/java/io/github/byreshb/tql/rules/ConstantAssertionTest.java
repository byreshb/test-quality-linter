package io.github.byreshb.tql.rules;

import static io.github.byreshb.tql.LintSupport.lint;
import static io.github.byreshb.tql.LintSupport.testClass;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConstantAssertionTest {

  private final ConstantAssertion rule = new ConstantAssertion();

  private List<Finding> lintStatement(String statement) {
    return lint(
        rule,
        testClass(
            """
            private static final int EXPECTED = 4;

            @Test
            void t() {
              int x = compute();
              String s = "a";
            """
                + statement
                + "\n  }"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "assertEquals(2, 1 + 1);",
        "assertEquals(\"message\", 2, 1 + 1);",
        "assertEquals(2, 1 + 1, \"message\");",
        "assertEquals(\"ab\", \"a\" + \"b\");",
        "assertEquals(EXPECTED, 4);",
        "assertEquals(EXPECTED, 2 * 2);",
        "assertNotEquals(1, 2);",
        "assertSame(\"a\", \"b\");",
        "assertTrue(1 < 2);",
        "assertTrue(EXPECTED > 0);",
        "assertFalse(1 > 2 || false);",
        "assertThat(1 + 1).isEqualTo(2);",
        "assertThat(\"abc\").startsWith(\"a\");",
        "assertThat(EXPECTED).isGreaterThan(3);",
        "assertThat(1 < 2).isTrue();",
        "assertThat(\"a\", is(\"b\"));",
        "assertThat(1 + 1, is(equalTo(2)));",
        "assertThat(3, greaterThan(2));",
      })
  void flagsAssertionsOverConstants(String statement) {
    List<Finding> findings = lintStatement(statement);
    assertThat(findings).hasSize(1);
    Finding finding = findings.get(0);
    assertThat(finding.ruleId()).isEqualTo("TQL002");
    assertThat(finding.severity()).isEqualTo(Severity.ERROR);
    assertThat(finding.line()).isEqualTo(16);
    assertThat(finding.message()).containsAnyOf("constant", "constants");
    assertThat(finding.fixHint()).contains("code under test");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "assertEquals(4, x);",
        "assertEquals(EXPECTED, x);",
        "assertEquals(\"a\", s);",
        "assertEquals(x, x);",
        "assertEquals(1, 1);",
        "assertEquals(\"a\", \"a\");",
        "assertTrue(true);",
        "assertFalse(false);",
        "assertTrue(x > 0);",
        "assertTrue(s.isEmpty());",
        "assertThat(x).isEqualTo(4);",
        "assertThat(4).isEqualTo(x);",
        "assertThat(true).isTrue();",
        "assertThat(\"a\").isEqualTo(\"a\");",
        "assertThat(\"a\", is(\"a\"));",
        "assertThat(s, is(\"a\"));",
        "assertThat(1, is(x));",
        "assertThat(List.of(1)).hasSize(1);",
        "assertEquals(1);",
        "assertNull(null);",
        "assertThat(\"a\", hasLength(1));",
        "assertThat(2).isEqualTo(x, 4);",
      })
  void leavesGenuineOrOtherwiseFlaggedAssertionsAlone(String statement) {
    assertThat(lintStatement(statement)).isEmpty();
  }

  @Test
  void describesItself() {
    assertThat(rule.id()).isEqualTo("TQL002");
    assertThat(rule.name()).isEqualTo("ConstantAssertion");
    assertThat(rule.defaultSeverity()).isEqualTo(Severity.ERROR);
    assertThat(rule.description()).contains("constants");
  }
}
