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

class ContradictoryMessageTest {

  private final ContradictoryMessage rule = new ContradictoryMessage();

  private List<Finding> lintStatement(String statement) {
    return lint(
        rule,
        testClass("  @Test\n  void t() {\n    boolean ok = true;\n    " + statement + "\n  }"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "assertTrue(ok, \"should not be ok\");",
        "assertTrue(ok, \"value should never be present\");",
        "assertTrue(\"should not be ok\", ok);",
        "assertTrue(ok, \"must not happen\");",
        "assertFalse(ok, \"should be ok\");",
        "assertFalse(\"should be ok\", ok);",
      })
  void flagsAContradictoryMessage(String statement) {
    List<Finding> findings = lintStatement(statement);
    assertThat(findings).hasSize(1);
    Finding finding = findings.get(0);
    assertThat(finding.ruleId()).isEqualTo("TQL010");
    assertThat(finding.severity()).isEqualTo(Severity.INFO);
    assertThat(finding.message()).contains("contradicts");
    assertThat(finding.fixHint()).contains("Reword");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "assertTrue(ok, \"should be ok\");",
        "assertTrue(ok);",
        "assertFalse(ok, \"should not be ok\");",
        "assertFalse(ok, \"must not be ok\");",
        "assertFalse(ok);",
        "assertEquals(1, 1, \"should not differ\");",
        "assertTrue(ok, unknownMessage());",
      })
  void leavesConsistentOrUnrecognisedMessagesAlone(String statement) {
    assertThat(lintStatement(statement)).isEmpty();
  }

  @Test
  void describesItself() {
    assertThat(rule.id()).isEqualTo("TQL010");
    assertThat(rule.name()).isEqualTo("ContradictoryMessage");
    assertThat(rule.description()).contains("opposite");
  }
}
