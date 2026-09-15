package io.github.byreshb.tql.rules;

import static io.github.byreshb.tql.LintSupport.lint;
import static io.github.byreshb.tql.LintSupport.testClass;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;

class VerifyOnlyTest {

  private final VerifyOnly rule = new VerifyOnly();

  private List<Finding> lintBody(String body) {
    return lint(rule, testClass("  @Test\n  void t() {\n" + body + "\n  }"));
  }

  @Test
  void flagsATestThatOnlyVerifies() {
    List<Finding> findings = lintBody("service.save(order);\nverify(mock).save(order);");
    assertThat(findings).hasSize(1);
    Finding finding = findings.get(0);
    assertThat(finding.ruleId()).isEqualTo("TQL005");
    assertThat(finding.severity()).isEqualTo(Severity.WARN);
    assertThat(finding.message()).contains("only verifies");
    assertThat(finding.fixHint()).contains("Assert on the state");
  }

  @Test
  void flagsEveryVerificationVariant() {
    assertThat(lintBody("verifyNoInteractions(mock);")).hasSize(1);
    assertThat(lintBody("verifyNoMoreInteractions(mock);")).hasSize(1);
    assertThat(lintBody("inOrder(mock).verify(mock).save(order);")).hasSize(1);
  }

  @Test
  void acceptsATestThatAlsoAssertsOnTheOutcome() {
    assertThat(
            lintBody(
                "Order saved = service.save(order);\n"
                    + "verify(mock).save(order);\n"
                    + "assertEquals(order, saved);"))
        .isEmpty();
  }

  @Test
  void acceptsATestWithoutAnyVerification() {
    assertThat(lintBody("service.save(order);")).isEmpty();
  }

  @Test
  void skipsTestsThatExpectAnException() {
    String members =
        """
          @Test(expected = IllegalStateException.class)
          public void t() {
            verify(mock).save(order);
          }
        """;
    assertThat(lint(rule, testClass(members))).isEmpty();
    assertThat(rule.description()).contains("verifications");
  }
}
