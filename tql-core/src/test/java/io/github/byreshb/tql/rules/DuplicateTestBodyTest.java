package io.github.byreshb.tql.rules;

import static io.github.byreshb.tql.LintSupport.lint;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;

class DuplicateTestBodyTest {

  private final DuplicateTestBody rule = new DuplicateTestBody();

  private static String testClass(String members) {
    return """
    import static org.junit.jupiter.api.Assertions.*;
    import org.junit.jupiter.api.Test;

    class SampleTest {
    """
        + members
        + "\n}\n";
  }

  @Test
  void flagsAnExactDuplicateBody() {
    String members =
        """
          @Test
          void savesWithTwoItems() {
            Order order = service.save(new Order("widget", 2));
            assertEquals(2, order.quantity());
          }

          @Test
          void savesAnotherOrder() {
            Order order = service.save(new Order("widget", 2));
            assertEquals(2, order.quantity());
          }
        """;
    List<Finding> findings = lint(rule, testClass(members));
    assertThat(findings).hasSize(1);
    Finding finding = findings.get(0);
    assertThat(finding.ruleId()).isEqualTo("TQL008");
    assertThat(finding.severity()).isEqualTo(Severity.WARN);
    assertThat(finding.message())
        .isEqualTo("Test 'savesAnotherOrder' has the same body as 'savesWithTwoItems'");
    assertThat(finding.fixHint()).contains("different case");
  }

  @Test
  void ignoresFormattingAndCommentDifferences() {
    String members =
        """
          @Test
          void a() {
            // set up
            int x = 1;
            assertEquals(1, x);   // check
          }

          @Test
          void b() {
            int   x   =   1;
            // different comment entirely
            assertEquals(1, x);
          }
        """;
    assertThat(lint(rule, testClass(members))).hasSize(1);
  }

  @Test
  void flagsEveryExtraCopyInALargerGroup() {
    String members =
        """
          @Test
          void a() {
            int x = 1;
            assertEquals(1, x);
          }

          @Test
          void b() {
            int x = 1;
            assertEquals(1, x);
          }

          @Test
          void c() {
            int x = 1;
            assertEquals(1, x);
          }
        """;
    assertThat(lint(rule, testClass(members)))
        .extracting(Finding::message)
        .containsExactly("Test 'b' has the same body as 'a'", "Test 'c' has the same body as 'a'");
  }

  @Test
  void doesNotCompareTrivialOneStatementBodies() {
    String members =
        """
          @Test
          void a() {
            assertTrue(true);
          }

          @Test
          void b() {
            assertTrue(true);
          }
        """;
    assertThat(lint(rule, testClass(members))).isEmpty();
  }

  @Test
  void leavesMethodsWithDifferentBodiesAlone() {
    String members =
        """
          @Test
          void a() {
            int x = 1;
            assertEquals(1, x);
          }

          @Test
          void b() {
            int x = 2;
            assertEquals(2, x);
          }

          void helper() {
            int x = 1;
            assertEquals(1, x);
          }
        """;
    assertThat(lint(rule, testClass(members))).isEmpty();
  }

  @Test
  void comparesOnlyWithinTheSameClass() {
    String source =
        """
        import static org.junit.jupiter.api.Assertions.*;
        import org.junit.jupiter.api.Test;

        class FirstTest {
          @Test
          void a() {
            int x = 1;
            assertEquals(1, x);
          }
        }
        class SecondTest {
          @Test
          void b() {
            int x = 1;
            assertEquals(1, x);
          }
        }
        """;
    assertThat(lint(rule, source)).isEmpty();
  }

  @Test
  void describesItself() {
    assertThat(rule.id()).isEqualTo("TQL008");
    assertThat(rule.name()).isEqualTo("DuplicateTestBody");
    assertThat(rule.description()).contains("identical");
  }
}
