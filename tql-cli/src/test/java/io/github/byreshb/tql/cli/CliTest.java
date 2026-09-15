package io.github.byreshb.tql.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

class CliTest {

  private record Result(int exitCode, String out, String err) {}

  private static Result run(String... args) {
    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    int exitCode = Cli.execute(args, new PrintWriter(out), new PrintWriter(err));
    return new Result(exitCode, out.toString(), err.toString());
  }

  private static Path writeTest(Path dir, String name, String content) throws IOException {
    Path file = dir.resolve(name);
    Files.writeString(file, content);
    return file;
  }

  private static final String BAD_TEST =
      """
      import static org.junit.jupiter.api.Assertions.assertEquals;
      import org.junit.jupiter.api.Test;

      class BadTest {
        @Test
        void same() {
          int x = 1;
          assertEquals(x, x);
        }
      }
      """;

  private static final String GOOD_TEST =
      """
      import static org.junit.jupiter.api.Assertions.assertEquals;
      import org.junit.jupiter.api.Test;

      class GoodTest {
        @Test
        void sum() {
          assertEquals(2, Math.addExact(1, 1));
        }
      }
      """;

  @Test
  void noArgumentsPrintsUsage() {
    Result result = run();
    assertThat(result.exitCode()).isZero();
    assertThat(result.out()).contains("Usage:").contains("tql").contains("lint");
  }

  @Test
  void versionReportsAVersionString() {
    Result result = run("--version");
    assertThat(result.exitCode()).isZero();
    assertThat(result.out()).contains("tql");
  }

  @Test
  void helpListsSubcommands() {
    Result result = run("--help");
    assertThat(result.exitCode()).isZero();
    assertThat(result.out()).contains("lint").contains("rules").contains("explain");
  }

  @Test
  void rulesListsEveryDiscoveredRule() {
    Result result = run("rules");
    assertThat(result.exitCode()).isZero();
    assertThat(result.out())
        .contains("TQL001")
        .contains("TautologicalAssertion")
        .contains("[ERROR]")
        .contains("TQL010")
        .contains("ContradictoryMessage")
        .contains("[INFO]");
  }

  @Test
  void explainPrintsDescriptionSeverityAndDocsLink() {
    Result result = run("explain", "TQL001");
    assertThat(result.exitCode()).isZero();
    assertThat(result.out())
        .contains("TQL001 TautologicalAssertion")
        .contains("Default severity: ERROR")
        .contains("docs/rules/TQL001.md");
  }

  @Test
  void explainAnUnknownRuleFailsWithAMessage() {
    Result result = run("explain", "TQL999");
    assertThat(result.exitCode()).isEqualTo(1);
    assertThat(result.err()).contains("Unknown rule: TQL999");
  }

  @Test
  void lintFailsOnAnErrorFindingByDefault(@TempDir Path dir) throws IOException {
    Path bad = writeTest(dir, "BadTest.java", BAD_TEST);
    Result result = run("lint", bad.toString());
    assertThat(result.exitCode()).isEqualTo(1);
    assertThat(result.out()).contains("TQL001").contains("ERROR");
  }

  @Test
  void lintPassesOnCleanSources(@TempDir Path dir) throws IOException {
    Path good = writeTest(dir, "GoodTest.java", GOOD_TEST);
    Result result = run("lint", good.toString());
    assertThat(result.exitCode()).isZero();
    assertThat(result.out()).contains("No findings");
  }

  @Test
  void lintFailOnRaisesTheThreshold(@TempDir Path dir) throws IOException {
    Path bad = writeTest(dir, "BadTest.java", BAD_TEST);
    // TQL003 (no assertion) is WARN; raise the bar so only ERROR findings fail the build, then
    // lower it so the same run fails.
    Result quiet =
        run(
            "lint",
            "--fail-on",
            "warn",
            "--config",
            writeConfigDisablingTql001(dir).toString(),
            bad.toString());
    assertThat(quiet.exitCode()).isZero();

    Result strict = run("lint", "--fail-on", "info", bad.toString());
    assertThat(strict.exitCode()).isEqualTo(1);
  }

  private static Path writeConfigDisablingTql001(Path dir) throws IOException {
    Path config = dir.resolve(".tql.yaml");
    Files.writeString(config, "rules:\n  TQL001:\n    enabled: false\n");
    return config;
  }

  @Test
  void lintHonoursAnExplicitConfigFile(@TempDir Path dir) throws IOException {
    Path bad = writeTest(dir, "BadTest.java", BAD_TEST);
    Path config = writeConfigDisablingTql001(dir);
    Result result = run("lint", "--config", config.toString(), bad.toString());
    assertThat(result.exitCode()).isZero();
    assertThat(result.out()).contains("No findings");
  }

  @Test
  void lintFailsWhenAFileCannotBeParsedEvenWithNoFindings(@TempDir Path dir) throws IOException {
    Path broken = writeTest(dir, "Broken.java", "class { oops");
    Result result = run("lint", "--fail-on", "error", broken.toString());
    assertThat(result.exitCode()).isEqualTo(1);
    assertThat(result.out()).contains("Could not parse");
  }

  @Test
  @SuppressWarnings("unchecked")
  void lintFormatJsonProducesParseableJson(@TempDir Path dir) throws IOException {
    Path bad = writeTest(dir, "BadTest.java", BAD_TEST);
    Result result = run("lint", "--format", "json", bad.toString());
    assertThat(result.exitCode()).isEqualTo(1);
    var parsed = (java.util.Map<String, Object>) new Yaml().load(result.out());
    assertThat(parsed.get("filesScanned")).isEqualTo(1);
    var findings = (java.util.List<java.util.Map<String, Object>>) parsed.get("findings");
    assertThat(findings).extracting(f -> f.get("ruleId")).containsExactly("TQL001");
  }

  @Test
  @SuppressWarnings("unchecked")
  void lintFormatSarifProducesParseableSarifWithRuleMetadata(@TempDir Path dir) throws IOException {
    Path bad = writeTest(dir, "BadTest.java", BAD_TEST);
    Result result = run("lint", "--format", "sarif", bad.toString());
    var parsed = (java.util.Map<String, Object>) new Yaml().load(result.out());
    assertThat(parsed.get("version")).isEqualTo("2.1.0");
    var runs = (java.util.List<java.util.Map<String, Object>>) parsed.get("runs");
    var driver =
        (java.util.Map<String, Object>)
            ((java.util.Map<String, Object>) runs.get(0).get("tool")).get("driver");
    var rules = (java.util.List<java.util.Map<String, Object>>) driver.get("rules");
    assertThat(rules).extracting(r -> r.get("id")).contains("TQL001");
  }

  @Test
  void lintFormatMdProducesAMarkdownReport(@TempDir Path dir) throws IOException {
    Path bad = writeTest(dir, "BadTest.java", BAD_TEST);
    Result result = run("lint", "--format", "md", bad.toString());
    assertThat(result.out()).contains("## Test Quality Linter").contains("| `TQL001` |");
  }

  @Test
  void lintAcceptsAClasspathOptionWithoutFailing(@TempDir Path dir) throws IOException {
    Path good = writeTest(dir, "GoodTest.java", GOOD_TEST);
    Result result = run("lint", "--classpath", dir.toString(), good.toString());
    assertThat(result.exitCode()).isZero();
  }

  @Test
  void lintWalksADirectory(@TempDir Path dir) throws IOException {
    writeTest(dir, "BadTest.java", BAD_TEST);
    writeTest(dir, "GoodTest.java", GOOD_TEST);
    Result result = run("lint", dir.toString());
    assertThat(result.exitCode()).isEqualTo(1);
    assertThat(result.out()).contains("2 files").contains("BadTest.java");
  }
}
