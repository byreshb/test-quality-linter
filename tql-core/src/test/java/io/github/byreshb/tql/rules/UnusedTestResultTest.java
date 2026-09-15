package io.github.byreshb.tql.rules;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.byreshb.tql.engine.Linter;
import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.Severity;
import io.github.byreshb.tql.model.SourceFile;
import io.github.byreshb.tql.rule.RuleRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;

class UnusedTestResultTest {

  private final UnusedTestResult rule = new UnusedTestResult();

  private List<Finding> lintWithSymbols(String body) {
    String source =
        """
        import static org.junit.jupiter.api.Assertions.*;
        import java.util.*;
        import org.junit.jupiter.api.Test;

        class SampleTest {
          @Test
          void t() {
        %s
          }
        }
        """
            .formatted(body);
    Linter linter =
        new Linter(
            RuleRegistry.of(rule), io.github.byreshb.tql.rule.RuleConfig.defaults(), List.of());
    return linter.lint(List.of(SourceFile.of("SampleTest.java", source))).findings();
  }

  @Test
  void producesNothingWithoutSymbolResolution() {
    String source =
        """
        import org.junit.jupiter.api.Test;

        class SampleTest {
          @Test
          void t() {
            "hello".length();
          }
        }
        """;
    Linter linter = Linter.withDefaults();
    assertThat(linter.lint(List.of(SourceFile.of("SampleTest.java", source))).findings())
        .filteredOn(f -> f.ruleId().equals("TQL011"))
        .isEmpty();
  }

  @Test
  void flagsADiscardedValueLikeResultWhenSymbolsAreResolved() {
    List<Finding> findings = lintWithSymbols("    \"hello\".length();\n    assertTrue(true);");
    assertThat(findings).hasSize(1);
    Finding finding = findings.get(0);
    assertThat(finding.ruleId()).isEqualTo("TQL011");
    assertThat(finding.severity()).isEqualTo(Severity.WARN);
    assertThat(finding.message()).contains("\"hello\".length()").contains("discarded");
    assertThat(finding.fixHint()).contains("Assert on the returned value");
  }

  @Test
  void flagsAnIntegerFromAList() {
    List<Finding> findings =
        lintWithSymbols(
            "    List<Integer> numbers = new ArrayList<>();\n"
                + "    numbers.get(0);\n"
                + "    assertTrue(true);");
    assertThat(findings)
        .extracting(Finding::message)
        .anySatisfy(m -> assertThat(m).contains("numbers.get(0)"));
  }

  @Test
  void acceptsWhenTheResultIsAssignedOrPassedToAnAssertion() {
    assertThat(lintWithSymbols("    int len = \"hello\".length();\n    assertEquals(5, len);"))
        .isEmpty();
    assertThat(lintWithSymbols("    assertEquals(5, \"hello\".length());")).isEmpty();
  }

  @Test
  void acceptsVoidCallsAssertionsAndReferencedResults() {
    assertThat(lintWithSymbols("    List<Integer> l = new ArrayList<>();\n    l.clear();"))
        .isEmpty();
    assertThat(lintWithSymbols("    assertEquals(1, 1);")).isEmpty();
    assertThat(lintWithSymbols("    \"hello\".length();\n    assertEquals(5, \"hello\".length());"))
        .isEmpty();
  }

  @Test
  void describesItself() {
    assertThat(rule.id()).isEqualTo("TQL011");
    assertThat(rule.name()).isEqualTo("UnusedTestResult");
    assertThat(rule.description()).contains("discarded");
  }
}
