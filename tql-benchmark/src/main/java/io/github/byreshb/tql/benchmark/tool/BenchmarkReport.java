package io.github.byreshb.tql.benchmark.tool;

import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.rule.Rule;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Computes the benchmark's metrics and renders them as {@code docs/benchmark.md}. */
final class BenchmarkReport {

  private BenchmarkReport() {}

  /**
   * Renders the full report.
   *
   * @param rules the rules the linter ran with, in id order
   * @param truth the planted ground truth, one entry per test method
   * @param findingsByMethod the linter's findings, keyed by {@code ClassName#methodName}
   * @param killedByMethod mutants killed per test method, from PIT; empty when PIT has not run
   * @return the Markdown text
   */
  static String render(
      List<Rule> rules,
      List<GroundTruth> truth,
      Map<String, List<Finding>> findingsByMethod,
      Map<String, Integer> killedByMethod) {
    boolean mutationDataAvailable = !killedByMethod.isEmpty();
    StringBuilder out = new StringBuilder();
    out.append("# Benchmark\n\n");
    out.append(intro(truth));
    out.append(method());
    out.append(findingsPerTest(truth, findingsByMethod));
    out.append(precisionAndRecall(rules, truth, findingsByMethod));
    out.append(mutationScore(truth, findingsByMethod, killedByMethod, mutationDataAvailable));
    return out.toString();
  }

  private static String intro(List<GroundTruth> truth) {
    long weak = truth.stream().filter(GroundTruth::isWeak).count();
    long clean = truth.size() - weak;
    return """
    `tql-benchmark` is a small sample project of hand-written tests: %d planted clean and
    %d planted weak, the weak ones written in the styles each rule targets and labelled with
    the rule id(s) they are meant to trigger (see `@Weak` on the test methods under
    `src/test/java`). This page reports what the linter actually finds against that known
    answer, and what mutation testing with [PIT](https://pitest.org/) says about the tests
    either way.

    Regenerated on %s from a real run of both tools; see "Method" below to reproduce it.

    """
        .formatted(clean, weak, LocalDate.now());
  }

  private static String method() {
    return """
    ## Method

    ```bash
    mvn -pl tql-benchmark -am install -DskipTests
    mvn -pl tql-benchmark org.pitest:pitest-maven:mutationCoverage
    mvn -pl tql-benchmark exec:java@benchmark-report
    ```

    The first command builds `tql-core` and the benchmark's classes. The second runs PIT
    over `io.github.byreshb.tql.benchmark`, writing `target/pit-reports/mutations.xml` (each
    mutant's `killingTest`, when detected, names the test method that caught it). The third
    runs `tql`'s own `Linter` over the benchmark's test sources (with the benchmark's own
    compiled classes on the classpath, so `TQL011` can resolve types), attributes each
    finding to the test method it falls inside, and rewrites this file from both reports and
    the planted `@Weak` ground truth.

    """;
  }

  private static String findingsPerTest(
      List<GroundTruth> truth, Map<String, List<Finding>> findingsByMethod) {
    StringBuilder out = new StringBuilder();
    out.append("## Findings per test\n\n");
    out.append("| Test | Planted as | tql found |\n");
    out.append("|---|---|---|\n");
    List<GroundTruth> sorted =
        truth.stream()
            .sorted(
                Comparator.comparing(GroundTruth::className).thenComparing(GroundTruth::methodName))
            .toList();
    for (GroundTruth t : sorted) {
      String key = t.className() + "#" + t.methodName();
      String planted = t.isWeak() ? String.join(", ", sortedIds(t.weakRuleIds())) : "-";
      List<Finding> findings = findingsByMethod.getOrDefault(key, List.of());
      String found =
          findings.isEmpty()
              ? "-"
              : findings.stream()
                  .map(Finding::ruleId)
                  .distinct()
                  .sorted()
                  .collect(Collectors.joining(", "));
      out.append("| `")
          .append(t.className())
          .append('#')
          .append(t.methodName())
          .append("` | ")
          .append(planted)
          .append(" | ")
          .append(found)
          .append(" |\n");
    }
    out.append('\n');
    return out.toString();
  }

  private static List<String> sortedIds(Set<String> ids) {
    List<String> sorted = new ArrayList<>(ids);
    sorted.sort(Comparator.naturalOrder());
    return sorted;
  }

  private static String precisionAndRecall(
      List<Rule> rules, List<GroundTruth> truth, Map<String, List<Finding>> findingsByMethod) {
    StringBuilder out = new StringBuilder();
    out.append("## Precision and recall, per rule\n\n");
    out.append(
        "Against the planted ground truth: a true positive is a rule firing on a test planted\n"
            + "for that rule; a false positive is a rule firing on a test not planted for it\n"
            + "(clean, or planted for a different rule); a false negative is a planted test the\n"
            + "rule missed.\n\n");
    out.append(
        "| Rule | Planted | True positives | False positives | False negatives | Precision | Recall"
            + " |\n");
    out.append("|---|---|---|---|---|---|---|\n");
    for (Rule rule : rules) {
      int tp = 0;
      int fp = 0;
      int fn = 0;
      int planted = 0;
      for (GroundTruth t : truth) {
        boolean expected = t.weakRuleIds().contains(rule.id());
        if (expected) {
          planted++;
        }
        boolean flagged =
            findingsByMethod.getOrDefault(t.className() + "#" + t.methodName(), List.of()).stream()
                .anyMatch(f -> f.ruleId().equals(rule.id()));
        if (expected && flagged) {
          tp++;
        } else if (expected) {
          fn++;
        } else if (flagged) {
          fp++;
        }
      }
      out.append("| ")
          .append(rule.id())
          .append(" | ")
          .append(planted)
          .append(" | ")
          .append(tp)
          .append(" | ")
          .append(fp)
          .append(" | ")
          .append(fn)
          .append(" | ")
          .append(percent(tp, tp + fp))
          .append(" | ")
          .append(percent(tp, tp + fn))
          .append(" |\n");
    }
    out.append('\n');
    return out.toString();
  }

  private static String percent(int numerator, int denominator) {
    return denominator == 0 ? "n/a" : Math.round(numerator * 100.0 / denominator) + "%";
  }

  private static String mutationScore(
      List<GroundTruth> truth,
      Map<String, List<Finding>> findingsByMethod,
      Map<String, Integer> killedByMethod,
      boolean mutationDataAvailable) {
    StringBuilder out = new StringBuilder();
    out.append("## Mutation score: flagged vs clean\n\n");
    if (!mutationDataAvailable) {
      out.append(
          "PIT has not been run yet (`target/pit-reports/mutations.xml` is missing); run the"
              + " second command under \"Method\" and regenerate this page.\n");
      return out.toString();
    }
    List<GroundTruth> flaggedByTql = new ArrayList<>();
    List<GroundTruth> notFlaggedByTql = new ArrayList<>();
    for (GroundTruth t : truth) {
      boolean flagged =
          !findingsByMethod.getOrDefault(t.className() + "#" + t.methodName(), List.of()).isEmpty();
      (flagged ? flaggedByTql : notFlaggedByTql).add(t);
    }
    List<GroundTruth> plantedWeak = truth.stream().filter(GroundTruth::isWeak).toList();
    List<GroundTruth> plantedClean = truth.stream().filter(t -> !t.isWeak()).toList();

    out.append("Mutants killed per test, averaged, using PIT's `killingTest` on each detected");
    out.append(" mutant:\n\n");
    out.append("| Group | Tests | Avg. mutants killed |\n");
    out.append("|---|---|---|\n");
    appendRow(out, "Tests tql flagged (at least one finding)", flaggedByTql, killedByMethod);
    appendRow(out, "Tests tql left clean", notFlaggedByTql, killedByMethod);
    appendRow(out, "Planted weak examples", plantedWeak, killedByMethod);
    appendRow(out, "Planted clean examples", plantedClean, killedByMethod);
    out.append('\n');
    out.append(
        "The flagged and planted-weak rows are expected to sit well below the clean rows: a"
            + " test tql flags contributes fewer surviving-mutant kills than one it leaves"
            + " alone, which is the headline this benchmark exists to demonstrate with a"
            + " number rather than an assertion.\n");
    return out.toString();
  }

  private static void appendRow(
      StringBuilder out,
      String label,
      List<GroundTruth> group,
      Map<String, Integer> killedByMethod) {
    int totalKilled =
        group.stream()
            .mapToInt(t -> killedByMethod.getOrDefault(t.className() + "#" + t.methodName(), 0))
            .sum();
    double average = group.isEmpty() ? 0 : totalKilled / (double) group.size();
    out.append("| ")
        .append(label)
        .append(" | ")
        .append(group.size())
        .append(" | ")
        .append(String.format("%.2f", average))
        .append(" |\n");
  }
}
