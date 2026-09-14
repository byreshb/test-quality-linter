package io.github.byreshb.tql.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LintResultTest {

  private static Finding finding(String file, int line, String rule, Severity severity) {
    return new Finding(rule, severity, Path.of(file), line, 1, "m", "f", Optional.empty());
  }

  @Test
  void sortsFindingsByFileLineAndRule() {
    LintResult result =
        new LintResult(
            2,
            List.of(
                finding("B.java", 3, "TQL002", Severity.WARN),
                finding("A.java", 9, "TQL001", Severity.ERROR),
                finding("A.java", 2, "TQL003", Severity.INFO),
                finding("A.java", 2, "TQL001", Severity.INFO)),
            List.of());
    assertThat(result.findings())
        .extracting(f -> f.file() + ":" + f.line() + ":" + f.ruleId())
        .containsExactly(
            "A.java:2:TQL001", "A.java:2:TQL003", "A.java:9:TQL001", "B.java:3:TQL002");
    assertThat(result.byFile().keySet()).containsExactly(Path.of("A.java"), Path.of("B.java"));
    assertThat(result.countByRule())
        .containsExactly(Map.entry("TQL001", 2L), Map.entry("TQL002", 1L), Map.entry("TQL003", 1L));
    assertThat(result.countBySeverity())
        .containsEntry(Severity.ERROR, 1L)
        .containsEntry(Severity.WARN, 1L)
        .containsEntry(Severity.INFO, 2L);
    assertThat(result.maxSeverity()).contains(Severity.ERROR);
    assertThat(result.hasFindingsAtOrAbove(Severity.ERROR)).isTrue();
    assertThat(result.isClean()).isFalse();
  }

  @Test
  void emptyResultIsClean() {
    LintResult result = LintResult.empty(3);
    assertThat(result.filesScanned()).isEqualTo(3);
    assertThat(result.isClean()).isTrue();
    assertThat(result.maxSeverity()).isEmpty();
    assertThat(result.hasFindingsAtOrAbove(Severity.INFO)).isFalse();
    assertThat(result.countBySeverity()).containsEntry(Severity.WARN, 0L);
  }

  @Test
  void parseProblemsMakeResultUnclean() {
    LintResult result =
        new LintResult(1, List.of(), List.of(new ParseProblem(Path.of("A.java"), 3, "oops")));
    assertThat(result.isClean()).isFalse();
    assertThat(result.problems()).hasSize(1);
  }
}
