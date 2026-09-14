package io.github.byreshb.tql.output;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.LintResult;
import io.github.byreshb.tql.model.ParseProblem;
import io.github.byreshb.tql.model.Severity;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConsoleReporterTest {

  private static final String ESC = String.valueOf((char) 27);

  private static final LintResult RESULT =
      new LintResult(
          3,
          List.of(
              new Finding(
                  "TQL001",
                  Severity.ERROR,
                  Path.of("src/test/java/FooTest.java"),
                  12,
                  5,
                  "assertEquals compares x with itself and cannot fail",
                  "Compare against an expected value",
                  Optional.of("assertEquals(x, x);")),
              new Finding(
                  "TQL003",
                  Severity.WARN,
                  Path.of("src/test/java/FooTest.java"),
                  20,
                  3,
                  "Test has no assertion",
                  "Assert on the outcome",
                  Optional.empty()),
              new Finding(
                  "TQL010",
                  Severity.INFO,
                  Path.of("src/test/java/BarTest.java"),
                  4,
                  9,
                  "Message contradicts assertion",
                  "Reword",
                  Optional.of("assertTrue(ok, \"should not be ok\");"))),
          List.of(new ParseProblem(Path.of("src/test/java/Broken.java"), 7, "Parse error")));

  @Test
  void groupsByFileAndPrintsSummaryWithoutColour() {
    String text = new ConsoleReporter(false).render(RESULT);
    assertThat(text)
        .isEqualTo(
            """
            src/test/java/BarTest.java
              4:9    INFO   TQL010  Message contradicts assertion
                     | assertTrue(ok, "should not be ok");
                     fix: Reword

            src/test/java/FooTest.java
              12:5   ERROR  TQL001  assertEquals compares x with itself and cannot fail
                     | assertEquals(x, x);
                     fix: Compare against an expected value
              20:3   WARN   TQL003  Test has no assertion
                     fix: Assert on the outcome

            Could not parse src/test/java/Broken.java:7: Parse error
            3 findings in 3 files (1 error, 1 warning, 1 info)
            """);
  }

  @Test
  void coloursSeveritiesWhenEnabled() {
    String text = new ConsoleReporter(true).render(RESULT);
    assertThat(text)
        .contains(ESC + "[31mERROR")
        .contains(ESC + "[33mWARN")
        .contains(ESC + "[32mINFO")
        .contains(ESC + "[36mTQL001" + ESC + "[0m");
    assertThat(new ConsoleReporter(false).render(RESULT)).doesNotContain(ESC);
  }

  @Test
  void summarisesCleanRunsAndSingularCounts() {
    assertThat(ConsoleReporter.summary(LintResult.empty(1))).isEqualTo("No findings in 1 file.");
    assertThat(ConsoleReporter.summary(LintResult.empty(4))).isEqualTo("No findings in 4 files.");
    LintResult one =
        new LintResult(
            1,
            List.of(
                new Finding(
                    "TQL003", Severity.WARN, Path.of("A.java"), 1, 1, "m", "f", Optional.empty())),
            List.of());
    assertThat(ConsoleReporter.summary(one))
        .isEqualTo("1 finding in 1 file (0 errors, 1 warning, 0 info)");
    assertThat(new ConsoleReporter(false).render(LintResult.empty(0)))
        .isEqualTo("No findings in 0 files.\n");
    assertThat(ConsoleReporter.forTerminal()).isNotNull();
  }
}
