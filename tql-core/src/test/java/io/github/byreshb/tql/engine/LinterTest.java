package io.github.byreshb.tql.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.LintResult;
import io.github.byreshb.tql.model.Severity;
import io.github.byreshb.tql.model.SourceFile;
import io.github.byreshb.tql.rule.RuleConfig;
import io.github.byreshb.tql.rule.RuleRegistry;
import io.github.byreshb.tql.rules.TautologicalAssertion;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LinterTest {

  private static final String BAD =
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

  private static final String GOOD =
      """
      import static org.junit.jupiter.api.Assertions.assertEquals;
      import org.junit.jupiter.api.Test;

      class GoodTest {
        @Test
        void sum() {
          assertEquals(2, 1 + 1);
        }
      }
      """;

  @Test
  void lintsInMemorySourcesWithDiscoveredRules() {
    Linter linter = Linter.withDefaults();
    assertThat(linter.enabledRules()).extracting(r -> r.id()).contains(TautologicalAssertion.ID);
    assertThat(linter.registry().rules()).isNotEmpty();
    assertThat(linter.config()).isEqualTo(RuleConfig.defaults());

    LintResult result =
        linter.lint(
            List.of(SourceFile.of("BadTest.java", BAD), SourceFile.of("GoodTest.java", GOOD)));

    assertThat(result.filesScanned()).isEqualTo(2);
    assertThat(result.problems()).isEmpty();
    assertThat(result.findings())
        .singleElement()
        .satisfies(
            f -> {
              assertThat(f.ruleId()).isEqualTo(TautologicalAssertion.ID);
              assertThat(f.file()).isEqualTo(Path.of("BadTest.java"));
              assertThat(f.line()).isEqualTo(8);
              assertThat(f.severity()).isEqualTo(Severity.ERROR);
            });
  }

  @Test
  void reportsFilesItCannotParse() {
    LintResult result =
        Linter.withDefaults().lint(List.of(SourceFile.of("Broken.java", "class { oops")));
    assertThat(result.findings()).isEmpty();
    assertThat(result.problems())
        .singleElement()
        .satisfies(
            p -> {
              assertThat(p.file()).isEqualTo(Path.of("Broken.java"));
              assertThat(p.line()).isEqualTo(1);
              assertThat(p.message()).isNotBlank();
            });
  }

  @Test
  void appliesDisabledRulesAndSeverityOverrides() {
    RuleRegistry registry = RuleRegistry.discover();
    List<SourceFile> sources = List.of(SourceFile.of("BadTest.java", BAD));

    RuleConfig disabled = RuleConfig.builder().disable(TautologicalAssertion.ID).build();
    assertThat(new Linter(registry, disabled).lint(sources).findings()).isEmpty();

    RuleConfig downgraded =
        RuleConfig.builder().severity(TautologicalAssertion.ID, Severity.INFO).build();
    assertThat(new Linter(registry, downgraded).lint(sources).findings())
        .extracting(Finding::severity)
        .containsExactly(Severity.INFO);
  }

  @Test
  void walksDirectoriesAndHonoursExcludes(@TempDir Path dir) throws Exception {
    Path tests = Files.createDirectories(dir.resolve("src/test/java"));
    Path generated = Files.createDirectories(tests.resolve("generated"));
    Files.writeString(tests.resolve("BadTest.java"), BAD);
    Files.writeString(tests.resolve("GoodTest.java"), GOOD);
    Files.writeString(generated.resolve("GenTest.java"), BAD);
    Files.writeString(tests.resolve("SkipMeTest.java"), BAD);
    Files.writeString(tests.resolve("notes.txt"), "not java");

    RuleConfig config =
        RuleConfig.builder().exclude("**/generated/**").exclude("SkipMe*.java").build();
    Linter linter = new Linter(RuleRegistry.discover(), config);
    LintResult result = linter.lintPaths(List.of(dir));

    assertThat(result.filesScanned()).isEqualTo(2);
    assertThat(result.findings())
        .extracting(f -> f.file().getFileName().toString())
        .containsExactly("BadTest.java");
    assertThat(linter.isExcluded(Path.of("a/generated/X.java"))).isTrue();
    assertThat(linter.isExcluded(Path.of("a/X.java"))).isFalse();

    LintResult single = linter.lintPaths(List.of(tests.resolve("GoodTest.java")));
    assertThat(single.filesScanned()).isEqualTo(1);
    assertThat(single.isClean()).isTrue();
  }

  @Test
  void failsLoudlyOnUnreadablePaths(@TempDir Path dir) {
    assertThatThrownBy(
            () -> Linter.withDefaults().lintPaths(List.of(dir.resolve("missing/File.java"))))
        .isInstanceOf(UncheckedIOException.class);
  }
}
