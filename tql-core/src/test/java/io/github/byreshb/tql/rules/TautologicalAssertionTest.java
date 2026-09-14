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

class TautologicalAssertionTest {

  private final TautologicalAssertion rule = new TautologicalAssertion();

  private List<Finding> lintStatement(String statement) {
    return lint(
        rule,
        testClass(
            """
            @Test
            void t() {
              Object x = new Object();
              int[] arr = {1};
              String s = "a";
            """
                + statement
                + "\n  }"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "assertEquals(x, x);",
        "assertEquals(x, (x));",
        "assertEquals(\"message\", x, x);",
        "assertEquals(x, x, \"message\");",
        "assertEquals(1.0, 1.0, 0.01);",
        "assertSame(x, x);",
        "assertArrayEquals(arr, arr);",
        "assertIterableEquals(List.of(x), List.of(x));",
        "assertEquals(s.length(), s.length());",
        "Assertions.assertEquals(x.hashCode(), x.hashCode());",
        "assertTrue(true);",
        "assertTrue(x == x);",
        "assertTrue(x.equals(x));",
        "assertTrue((s.length() >= s.length()));",
        "assertFalse(false);",
        "assertFalse(x != x);",
        "assertFalse(s.length() < s.length());",
        "assertNotNull(new Object());",
        "assertNotNull(new int[0]);",
        "assertNotNull(\"literal\");",
        "assertNotNull(() -> 1);",
        "assertThat(x).isEqualTo(x);",
        "assertThat(x).as(\"desc\").isSameAs(x);",
        "assertThat(List.of(x)).containsExactlyElementsOf(List.of(x));",
        "assertThat(new Object()).isNotNull();",
        "assertThat(true).isTrue();",
        "assertThat(false).isFalse();",
        "assertThat(x, is(x));",
        "assertThat(x, equalTo(x));",
        "assertThat(\"reason\", x, is(equalTo(x)));",
        "assertThat(x, sameInstance(x));",
        "assertThat(new Object(), notNullValue());",
      })
  void flagsAssertionsThatCannotFail(String statement) {
    List<Finding> findings = lintStatement(statement);
    assertThat(findings).hasSize(1);
    Finding finding = findings.get(0);
    assertThat(finding.ruleId()).isEqualTo("TQL001");
    assertThat(finding.severity()).isEqualTo(Severity.ERROR);
    assertThat(finding.line()).isEqualTo(15);
    assertThat(finding.message()).containsAnyOf("cannot fail", "with itself");
    assertThat(finding.fixHint()).isNotBlank();
    assertThat(finding.snippet()).contains(statement);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "assertEquals(1, x.hashCode());",
        "assertEquals(\"message\", 2, s.length());",
        "assertEquals(s, s.trim());",
        "assertNotEquals(x, x);",
        "assertSame(x, s);",
        "assertTrue(s.isEmpty());",
        "assertTrue(x == s);",
        "assertTrue(x.equals(s));",
        "assertFalse(s.isBlank());",
        "assertFalse(1 < 2);",
        "assertNotNull(x);",
        "assertNotNull(null);",
        "assertNotNull(s.trim());",
        "assertThat(x).isEqualTo(s);",
        "assertThat(x).isNotNull();",
        "assertThat(s.isEmpty()).isTrue();",
        "assertThat(x, is(s));",
        "assertThat(x, notNullValue());",
        "assertThat(s, hasLength(1));",
        "assertThat(x).usingRecursiveComparison().isEqualTo(s);",
        "assertEquals(x);",
        "someHelper().isEqualTo(x);",
      })
  void leavesGenuineAssertionsAlone(String statement) {
    assertThat(lintStatement(statement)).isEmpty();
  }

  @Test
  void describesRuleAndTruncatesLongExpressions() {
    assertThat(rule.id()).isEqualTo("TQL001");
    assertThat(rule.name()).isEqualTo("TautologicalAssertion");
    assertThat(rule.description()).contains("never fail").endsWith(".");
    List<Finding> findings =
        lintStatement(
            "assertEquals(someVeryLongMethodName(anotherArgument, yetAnother, andMore),"
                + " someVeryLongMethodName(anotherArgument, yetAnother, andMore));");
    assertThat(findings).hasSize(1);
    assertThat(findings.get(0).message()).contains("...").hasSizeLessThan(120);
  }

  @Test
  void flagsEveryOffendingCallInAFile() {
    List<Finding> findings =
        lint(
            rule,
            testClass(
                """
                  @Test
                  void a() {
                    assertTrue(true);
                  }

                  @Test
                  void b() {
                    assertFalse(false);
                    assertEquals(1, 2);
                  }

                  private void helper(Object o) {
                    assertSame(o, o);
                  }
                """));
    assertThat(findings).extracting(Finding::line).containsExactly(12, 17, 22);
  }
}
