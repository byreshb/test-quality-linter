package io.github.byreshb.tql.output;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.LintResult;
import io.github.byreshb.tql.model.ParseProblem;
import io.github.byreshb.tql.model.Severity;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class JsonReporterTest {

  private final JsonReporter reporter = new JsonReporter();

  /** JSON is a subset of YAML, so snakeyaml (already a project dependency) parses it back. */
  @SuppressWarnings("unchecked")
  private static Map<String, Object> parse(String json) {
    return (Map<String, Object>) new Yaml().load(json);
  }

  @Test
  void rendersFilesScannedFindingsAndProblems() {
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
                    "assertEquals compares \"x\" with itself",
                    "fix it",
                    Optional.of("assertEquals(x, x);"))),
            List.of(new ParseProblem(Path.of("B.java"), 7, "unexpected token")));

    Map<String, Object> parsed = parse(reporter.render(result));

    assertThat(parsed.get("filesScanned")).isEqualTo(2);
    List<Map<String, Object>> findings = (List<Map<String, Object>>) parsed.get("findings");
    assertThat(findings).hasSize(1);
    Map<String, Object> finding = findings.get(0);
    assertThat(finding.get("ruleId")).isEqualTo("TQL001");
    assertThat(finding.get("severity")).isEqualTo("ERROR");
    assertThat(finding.get("file")).isEqualTo("A.java");
    assertThat(finding.get("line")).isEqualTo(3);
    assertThat(finding.get("column")).isEqualTo(5);
    assertThat(finding.get("message")).isEqualTo("assertEquals compares \"x\" with itself");
    assertThat(finding.get("fixHint")).isEqualTo("fix it");
    assertThat(finding.get("snippet")).isEqualTo("assertEquals(x, x);");

    List<Map<String, Object>> problems = (List<Map<String, Object>>) parsed.get("problems");
    assertThat(problems).hasSize(1);
    assertThat(problems.get(0).get("file")).isEqualTo("B.java");
    assertThat(problems.get(0).get("line")).isEqualTo(7);
    assertThat(problems.get(0).get("message")).isEqualTo("unexpected token");
  }

  @Test
  void rendersEmptyArraysAndNullSnippetAsValidJson() {
    LintResult result = LintResult.empty(0);
    Map<String, Object> parsed = parse(reporter.render(result));
    assertThat(parsed.get("filesScanned")).isEqualTo(0);
    assertThat((List<?>) parsed.get("findings")).isEmpty();
    assertThat((List<?>) parsed.get("problems")).isEmpty();

    LintResult withoutSnippet =
        new LintResult(
            1,
            List.of(
                new Finding(
                    "TQL003", Severity.WARN, Path.of("A.java"), 1, 1, "m", "f", Optional.empty())),
            List.of());
    Map<String, Object> parsedWithout = parse(reporter.render(withoutSnippet));
    List<Map<String, Object>> findings = (List<Map<String, Object>>) parsedWithout.get("findings");
    assertThat(findings.get(0)).containsEntry("snippet", null);
  }
}
