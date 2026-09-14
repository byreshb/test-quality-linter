package io.github.byreshb.tql.model;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The outcome of one linter run: the findings of every rule over every file, plus the files that
 * could not be parsed.
 *
 * @param filesScanned how many files were linted, including unparseable ones
 * @param findings all findings, sorted by file, line and column
 * @param problems the files that could not be parsed
 */
public record LintResult(int filesScanned, List<Finding> findings, List<ParseProblem> problems) {

  /** Validates the arguments and sorts the findings. */
  public LintResult {
    Objects.requireNonNull(findings, "findings");
    Objects.requireNonNull(problems, "problems");
    findings =
        findings.stream()
            .sorted(
                Comparator.comparing((Finding f) -> f.file().toString())
                    .thenComparingInt(Finding::line)
                    .thenComparingInt(Finding::column)
                    .thenComparing(Finding::ruleId))
            .toList();
    problems = List.copyOf(problems);
  }

  /**
   * Returns an empty result for the given number of files.
   *
   * @param filesScanned how many files were linted
   * @return a result without findings or problems
   */
  public static LintResult empty(int filesScanned) {
    return new LintResult(filesScanned, List.of(), List.of());
  }

  /**
   * Groups the findings by file, preserving file order.
   *
   * @return the findings per file
   */
  public Map<Path, List<Finding>> byFile() {
    Map<Path, List<Finding>> grouped = new LinkedHashMap<>();
    for (Finding finding : findings) {
      grouped.computeIfAbsent(finding.file(), unused -> new java.util.ArrayList<>()).add(finding);
    }
    return grouped;
  }

  /**
   * Counts the findings per rule id, in rule id order.
   *
   * @return the counts per rule
   */
  public Map<String, Long> countByRule() {
    Map<String, Long> counts = new java.util.TreeMap<>();
    for (Finding finding : findings) {
      counts.merge(finding.ruleId(), 1L, Long::sum);
    }
    return counts;
  }

  /**
   * Counts the findings per severity.
   *
   * @return the counts per severity, with every severity present
   */
  public Map<Severity, Long> countBySeverity() {
    Map<Severity, Long> counts = new EnumMap<>(Severity.class);
    for (Severity severity : Severity.values()) {
      counts.put(severity, 0L);
    }
    for (Finding finding : findings) {
      counts.merge(finding.severity(), 1L, Long::sum);
    }
    return counts;
  }

  /**
   * Returns the most serious severity among the findings.
   *
   * @return the maximum severity, or empty when there are no findings
   */
  public Optional<Severity> maxSeverity() {
    return findings.stream().map(Finding::severity).max(Comparator.naturalOrder());
  }

  /**
   * Whether any finding has at least the given severity, used to decide whether a build fails.
   *
   * @param threshold the severity to compare against
   * @return true when at least one finding reaches the threshold
   */
  public boolean hasFindingsAtOrAbove(Severity threshold) {
    return findings.stream().anyMatch(f -> f.severity().atLeast(threshold));
  }

  /**
   * Whether the run found nothing and parsed every file.
   *
   * @return true when there are no findings and no parse problems
   */
  public boolean isClean() {
    return findings.isEmpty() && problems.isEmpty();
  }
}
