package io.github.byreshb.tql.output;

import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.LintResult;
import io.github.byreshb.tql.model.ParseProblem;
import io.github.byreshb.tql.model.Severity;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Human-readable output grouped by file, with the finding's position, severity, rule id, message,
 * fix hint and the offending line. ANSI colours are used when enabled, which callers should do only
 * when writing to a terminal.
 */
public final class ConsoleReporter implements Reporter {

  private static final String ESC = "\u001b";
  private static final String RESET = ESC + "[0m";
  private static final String BOLD = ESC + "[1m";
  private static final String DIM = ESC + "[2m";
  private static final String RED = ESC + "[31m";
  private static final String YELLOW = ESC + "[33m";
  private static final String CYAN = ESC + "[36m";
  private static final String GREEN = ESC + "[32m";

  private final boolean colour;

  /**
   * Creates a reporter.
   *
   * @param colour whether to emit ANSI colour codes
   */
  public ConsoleReporter(boolean colour) {
    this.colour = colour;
  }

  /**
   * A reporter that colours its output when standard output is a terminal and {@code NO_COLOR} is
   * not set.
   *
   * @return the reporter
   */
  public static ConsoleReporter forTerminal() {
    return new ConsoleReporter(System.console() != null && System.getenv("NO_COLOR") == null);
  }

  @Override
  public void write(LintResult result, Appendable out) throws IOException {
    for (Map.Entry<Path, List<Finding>> entry : result.byFile().entrySet()) {
      out.append(paint(BOLD, entry.getKey().toString())).append('\n');
      for (Finding finding : entry.getValue()) {
        out.append("  ")
            .append(String.format("%-7s", finding.line() + ":" + finding.column()))
            .append(paintSeverity(finding.severity()))
            .append("  ")
            .append(paint(CYAN, finding.ruleId()))
            .append("  ")
            .append(finding.message())
            .append('\n');
        if (finding.snippet().isPresent()) {
          out.append("         ").append(paint(DIM, "| " + finding.snippet().get())).append('\n');
        }
        out.append("         ").append(paint(DIM, "fix: " + finding.fixHint())).append('\n');
      }
      out.append('\n');
    }
    for (ParseProblem problem : result.problems()) {
      out.append(paint(RED, "Could not parse " + problem.file()));
      if (problem.line() > 0) {
        out.append(":").append(String.valueOf(problem.line()));
      }
      out.append(": ").append(problem.message()).append('\n');
    }
    out.append(summary(result)).append('\n');
  }

  /**
   * The one-line summary printed last, for example {@code 3 findings in 2 files (1 error, 2
   * warnings, 0 info)}.
   *
   * @param result the result
   * @return the summary line, without colour
   */
  public static String summary(LintResult result) {
    String files = result.filesScanned() == 1 ? "1 file" : result.filesScanned() + " files";
    if (result.findings().isEmpty()) {
      return "No findings in " + files + ".";
    }
    Map<Severity, Long> counts = result.countBySeverity();
    int total = result.findings().size();
    return (total == 1 ? "1 finding" : total + " findings")
        + " in "
        + files
        + " ("
        + plural(counts.get(Severity.ERROR), "error")
        + ", "
        + plural(counts.get(Severity.WARN), "warning")
        + ", "
        + counts.get(Severity.INFO)
        + " info)";
  }

  private static String plural(long count, String noun) {
    return count + " " + noun + (count == 1 ? "" : "s");
  }

  private String paintSeverity(Severity severity) {
    String label = String.format("%-5s", severity);
    switch (severity) {
      case ERROR:
        return paint(RED, label);
      case WARN:
        return paint(YELLOW, label);
      default:
        return paint(GREEN, label);
    }
  }

  private String paint(String code, String text) {
    return colour ? code + text + RESET : text;
  }
}
