package io.github.byreshb.tql.rules;

import static io.github.byreshb.tql.LintSupport.lint;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.Severity;
import io.github.byreshb.tql.model.SourceFile;
import io.github.byreshb.tql.rule.RuleConfig;
import io.github.byreshb.tql.rule.RuleRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DisabledWithoutReasonTest {

  private final DisabledWithoutReason rule =
      new DisabledWithoutReason((file, line) -> Optional.empty());

  private static String testClass(String members) {
    return """
    import org.junit.jupiter.api.Disabled;
    import org.junit.jupiter.api.Test;
    import org.junit.Ignore;

    class SampleTest {
    """
        + members
        + "\n}\n";
  }

  @Test
  void flagsAMarkerDisabledWithNoReason() {
    List<Finding> findings =
        lint(
            rule,
            testClass(
                """
                  @Disabled
                  @Test
                  void t() {
                  }
                """));
    assertThat(findings).hasSize(1);
    Finding finding = findings.get(0);
    assertThat(finding.ruleId()).isEqualTo("TQL009");
    assertThat(finding.severity()).isEqualTo(Severity.WARN);
    assertThat(finding.message()).isEqualTo("@Disabled has no reason");
    assertThat(finding.fixHint()).contains("@Disabled(");
  }

  @Test
  void flagsAnEmptyStringReason() {
    List<Finding> findings =
        lint(
            rule,
            testClass(
                """
                  @Disabled("")
                  @Test
                  void t() {
                  }
                """));
    assertThat(findings).hasSize(1);
  }

  @Test
  void acceptsASingleMemberReason() {
    List<Finding> findings =
        lint(
            rule,
            testClass(
                """
                  @Disabled("blocked by ISSUE-42")
                  @Test
                  void t() {
                  }
                """));
    assertThat(findings).isEmpty();
  }

  @Test
  void acceptsANamedValueOrReasonAttribute() {
    List<Finding> findings =
        lint(
            rule,
            testClass(
                """
                  @Disabled(value = "flaky")
                  @Test
                  void withValue() {
                  }
                """));
    assertThat(findings).isEmpty();

    List<Finding> ignoreFindings =
        lint(
            rule,
            testClass(
                """
                  @Ignore("flaky")
                  @Test
                  void withReason() {
                  }
                """));
    assertThat(ignoreFindings).isEmpty();
  }

  @Test
  void flagsAClassLevelDisableWithoutAReason() {
    String source =
        """
        import org.junit.jupiter.api.Disabled;
        import org.junit.jupiter.api.Test;

        @Disabled
        class SampleTest {
          @Test
          void t() {
          }
        }
        """;
    assertThat(lint(rule, source))
        .extracting(Finding::message)
        .containsExactly("@Disabled has no reason");
  }

  @Test
  void flagsJUnit4IgnoreWithNoReason() {
    List<Finding> findings =
        lint(
            rule,
            testClass(
                """
                  @Ignore
                  @Test
                  void t() {
                  }
                """));
    assertThat(findings).extracting(Finding::message).containsExactly("@Ignore has no reason");
  }

  @Test
  void doesNotCheckAgeWhenMaxAgeDaysIsNotConfigured() {
    DisabledWithoutReason ageRule =
        new DisabledWithoutReason((file, line) -> Optional.of(Instant.EPOCH));
    List<Finding> findings =
        lint(ageRule, testClass("  @Disabled(\"ok\")\n  @Test\n  void t() {\n  }\n"));
    assertThat(findings).isEmpty();
  }

  @Test
  void flagsAnOldDisableWhenMaxAgeDaysIsConfigured() {
    DisabledWithoutReason ageRule =
        new DisabledWithoutReason((file, line) -> Optional.of(Instant.EPOCH));
    RuleConfig config = RuleConfig.builder().option("TQL009", "maxAgeDays", 30).build();
    List<Finding> findings =
        new io.github.byreshb.tql.engine.Linter(RuleRegistry.of(ageRule), config)
            .lint(
                List.of(
                    SourceFile.of(
                        "SampleTest.java",
                        testClass("  @Disabled(\"ok\")\n  @Test\n  void t() {\n  }\n"))))
            .findings();
    assertThat(findings).hasSize(1);
    assertThat(findings.get(0).message()).contains("stood for").contains("30-day limit");
  }

  @Test
  void skipsTheAgeCheckWhenGitCannotResolveTheLine() {
    DisabledWithoutReason ageRule = new DisabledWithoutReason((file, line) -> Optional.empty());
    RuleConfig config = RuleConfig.builder().option("TQL009", "maxAgeDays", 1).build();
    List<Finding> findings =
        new io.github.byreshb.tql.engine.Linter(RuleRegistry.of(ageRule), config)
            .lint(
                List.of(
                    SourceFile.of(
                        "SampleTest.java",
                        testClass("  @Disabled(\"ok\")\n  @Test\n  void t() {\n  }\n"))))
            .findings();
    assertThat(findings).isEmpty();
  }

  @Test
  void describesItself() {
    assertThat(rule.id()).isEqualTo("TQL009");
    assertThat(rule.name()).isEqualTo("DisabledWithoutReason");
    assertThat(rule.description()).contains("no reason");
  }

  @Test
  void productionConstructorUsesShellGitBlame() {
    assertThat(new DisabledWithoutReason()).isNotNull();
  }
}
