package io.github.byreshb.tql.output;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.LintResult;
import io.github.byreshb.tql.model.Severity;
import io.github.byreshb.tql.rules.NoAssertion;
import io.github.byreshb.tql.rules.TautologicalAssertion;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class SarifReporterTest {

  @SuppressWarnings("unchecked")
  private static Map<String, Object> parse(String json) {
    return (Map<String, Object>) new Yaml().load(json);
  }

  private static final LintResult RESULT =
      new LintResult(
          1,
          List.of(
              new Finding(
                  "TQL001",
                  Severity.ERROR,
                  Path.of("src/test/java/FooTest.java"),
                  12,
                  5,
                  "assertEquals compares x with itself",
                  "fix it",
                  Optional.empty()),
              new Finding(
                  "TQL010",
                  Severity.INFO,
                  Path.of("src/test/java/FooTest.java"),
                  20,
                  3,
                  "contradictory message",
                  "reword it",
                  Optional.empty())),
          List.of());

  @Test
  void producesValidSarifWithSchemaVersionAndToolInfo() {
    SarifReporter reporter =
        new SarifReporter(List.of(new TautologicalAssertion(), new NoAssertion()), "1.2.3");
    Map<String, Object> parsed = parse(reporter.render(RESULT));

    assertThat(parsed.get("version")).isEqualTo("2.1.0");
    assertThat(parsed.get("$schema").toString()).contains("sarif-schema-2.1.0.json");

    List<Map<String, Object>> runs = (List<Map<String, Object>>) parsed.get("runs");
    assertThat(runs).hasSize(1);
    Map<String, Object> driver =
        (Map<String, Object>) ((Map<String, Object>) runs.get(0).get("tool")).get("driver");
    assertThat(driver.get("name")).isEqualTo("tql");
    assertThat(driver.get("version")).isEqualTo("1.2.3");
    assertThat(driver.get("informationUri").toString()).contains("test-quality-linter");

    List<Map<String, Object>> rules = (List<Map<String, Object>>) driver.get("rules");
    assertThat(rules).extracting(r -> r.get("id")).containsExactlyInAnyOrder("TQL001", "TQL003");
    Map<String, Object> tql001 =
        rules.stream().filter(r -> r.get("id").equals("TQL001")).findFirst().orElseThrow();
    assertThat(tql001.get("name")).isEqualTo("TautologicalAssertion");
    Map<String, Object> shortDescription = (Map<String, Object>) tql001.get("shortDescription");
    assertThat(shortDescription.get("text").toString()).contains("never fail");
    Map<String, Object> defaultConfig = (Map<String, Object>) tql001.get("defaultConfiguration");
    assertThat(defaultConfig.get("level")).isEqualTo("error");
  }

  @Test
  void mapsSeveritiesToSarifLevelsAndFillsLocations() {
    SarifReporter reporter = new SarifReporter(List.of());
    Map<String, Object> parsed = parse(reporter.render(RESULT));
    List<Map<String, Object>> runs = (List<Map<String, Object>>) parsed.get("runs");
    List<Map<String, Object>> results = (List<Map<String, Object>>) runs.get(0).get("results");
    assertThat(results).hasSize(2);

    Map<String, Object> errorResult = results.get(0);
    assertThat(errorResult.get("ruleId")).isEqualTo("TQL001");
    assertThat(errorResult.get("level")).isEqualTo("error");
    Map<String, Object> message = (Map<String, Object>) errorResult.get("message");
    assertThat(message.get("text")).isEqualTo("assertEquals compares x with itself");
    List<Map<String, Object>> locations = (List<Map<String, Object>>) errorResult.get("locations");
    Map<String, Object> physical = (Map<String, Object>) locations.get(0).get("physicalLocation");
    Map<String, Object> artifact = (Map<String, Object>) physical.get("artifactLocation");
    assertThat(artifact.get("uri")).isEqualTo("src/test/java/FooTest.java");
    Map<String, Object> region = (Map<String, Object>) physical.get("region");
    assertThat(region.get("startLine")).isEqualTo(12);
    assertThat(region.get("startColumn")).isEqualTo(5);

    assertThat(results.get(1).get("level")).isEqualTo("note");
  }

  @Test
  void rendersEmptyRulesAndResultsAsValidJson() {
    SarifReporter reporter = new SarifReporter(List.of());
    Map<String, Object> parsed = parse(reporter.render(LintResult.empty(0)));
    List<Map<String, Object>> runs = (List<Map<String, Object>>) parsed.get("runs");
    List<Map<String, Object>> results = (List<Map<String, Object>>) runs.get(0).get("results");
    assertThat(results).isEmpty();
    assertThat(new SarifReporter(List.of()).render(RESULT)).contains("\"version\": \"0.0.0\"");
  }
}
