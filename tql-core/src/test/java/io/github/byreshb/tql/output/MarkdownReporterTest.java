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

class MarkdownReporterTest {

  private final MarkdownReporter reporter = new MarkdownReporter();

  @Test
  void rendersAHeadingSummaryAndOneTablePerFile() {
    LintResult result =
        new LintResult(
            2,
            List.of(
                new Finding(
                    "TQL001",
                    Severity.ERROR,
                    Path.of("A.java"),
                    3,
                    5,
                    "message with a | pipe",
                    "fix hint",
                    Optional.empty()),
                new Finding(
                    "TQL003",
                    Severity.WARN,
                    Path.of("B.java"),
                    1,
                    1,
                    "no assertion",
                    "assert something",
                    Optional.empty())),
            List.of(new ParseProblem(Path.of("C.java"), 9, "syntax error")));

    String markdown = reporter.render(result);

    assertThat(markdown).startsWith("## Test Quality Linter\n\n");
    assertThat(markdown).contains(ConsoleReporter.summary(result));
    assertThat(markdown).contains("### `A.java`");
    assertThat(markdown)
        .contains("| 3:5 | ERROR | `TQL001` | message with a \\| pipe | fix hint |");
    assertThat(markdown).contains("### `B.java`");
    assertThat(markdown).contains("| 1:1 | WARN | `TQL003` | no assertion | assert something |");
    assertThat(markdown).contains("### Files that could not be parsed");
    assertThat(markdown).contains("- `C.java`:9: syntax error");
  }

  @Test
  void omitsTheProblemsSectionWhenThereAreNone() {
    LintResult result = LintResult.empty(1);
    String markdown = reporter.render(result);
    assertThat(markdown).doesNotContain("could not be parsed");
    assertThat(markdown).contains("No findings in 1 file.");
  }

  @Test
  void escapesNewlinesInMessages() {
    LintResult result =
        new LintResult(
            1,
            List.of(
                new Finding(
                    "TQL001",
                    Severity.ERROR,
                    Path.of("A.java"),
                    1,
                    1,
                    "line one\nline two",
                    "f",
                    Optional.empty())),
            List.of());
    assertThat(reporter.render(result)).contains("line one line two");
  }
}
