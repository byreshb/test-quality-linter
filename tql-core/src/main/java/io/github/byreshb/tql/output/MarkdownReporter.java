package io.github.byreshb.tql.output;

import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.LintResult;
import io.github.byreshb.tql.model.ParseProblem;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * GitHub-flavoured Markdown, meant to be posted as a pull request comment: a heading with the
 * summary line, one table per file with a row per finding, and a bullet list of files that could
 * not be parsed.
 */
public final class MarkdownReporter implements Reporter {

  @Override
  public void write(LintResult result, Appendable out) throws IOException {
    out.append("## Test Quality Linter\n\n");
    out.append(ConsoleReporter.summary(result)).append("\n\n");
    for (Map.Entry<Path, List<Finding>> entry : result.byFile().entrySet()) {
      writeFileSection(entry.getKey(), entry.getValue(), out);
    }
    if (!result.problems().isEmpty()) {
      writeProblems(result.problems(), out);
    }
  }

  private static void writeFileSection(Path file, List<Finding> findings, Appendable out)
      throws IOException {
    out.append("### `").append(file.toString()).append("`\n\n");
    out.append("| Line | Severity | Rule | Message | Fix |\n");
    out.append("|---|---|---|---|---|\n");
    for (Finding finding : findings) {
      out.append("| ")
          .append(String.valueOf(finding.line()))
          .append(':')
          .append(String.valueOf(finding.column()))
          .append(" | ")
          .append(finding.severity().toString())
          .append(" | `")
          .append(finding.ruleId())
          .append("` | ")
          .append(escape(finding.message()))
          .append(" | ")
          .append(escape(finding.fixHint()))
          .append(" |\n");
    }
    out.append('\n');
  }

  private static void writeProblems(List<ParseProblem> problems, Appendable out)
      throws IOException {
    out.append("### Files that could not be parsed\n\n");
    for (ParseProblem problem : problems) {
      out.append("- `").append(problem.file().toString()).append('`');
      if (problem.line() > 0) {
        out.append(':').append(String.valueOf(problem.line()));
      }
      out.append(": ").append(escape(problem.message())).append('\n');
    }
    out.append('\n');
  }

  private static String escape(String text) {
    return text.replace("|", "\\|").replace("\n", " ").replace("\r", "");
  }
}
