package io.github.byreshb.tql.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.byreshb.tql.model.Severity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RuleConfigLoaderTest {

  @Test
  void blankOrMissingYamlYieldsDefaults() {
    assertThat(RuleConfigLoader.load("")).isEqualTo(RuleConfig.defaults());
    assertThat(RuleConfigLoader.load((String) null)).isEqualTo(RuleConfig.defaults());
    assertThat(RuleConfigLoader.load("# just a comment\n")).isEqualTo(RuleConfig.defaults());
  }

  @Test
  void loadsDisableSeverityOptionsAndExcludes() {
    String yaml =
        """
        rules:
          TQL001:
            enabled: false
          TQL003:
            severity: ERROR
            options:
              methods: [ensureSaved, mustContain]
              packages: [com.acme.testing]
          TQL006:
            options:
              maxMillis: 50
        exclude:
          - "**/generated/**"
          - "SkipMe*.java"
        """;
    RuleConfig config = RuleConfigLoader.load(yaml);

    assertThat(config.isEnabled("TQL001")).isFalse();
    assertThat(config.severityOf("TQL003", Severity.WARN)).isEqualTo(Severity.ERROR);
    assertThat(config.listOption("TQL003", "methods", List.of()))
        .containsExactly("ensureSaved", "mustContain");
    assertThat(config.listOption("TQL003", "packages", List.of()))
        .containsExactly("com.acme.testing");
    assertThat(config.intOption("TQL006", "maxMillis", 0)).isEqualTo(50);
    assertThat(config.excludes()).containsExactly("**/generated/**", "SkipMe*.java");
  }

  @Test
  void aRuleMentionedWithoutSettingsKeepsItsDefaults() {
    RuleConfig config = RuleConfigLoader.load("rules:\n  TQL001: {}\n");
    assertThat(config.isEnabled("TQL001")).isTrue();
    assertThat(config.severityOf("TQL001", Severity.ERROR)).isEqualTo(Severity.ERROR);
  }

  @Test
  void rejectsANonMappingTopLevel() {
    assertThatThrownBy(() -> RuleConfigLoader.load("- a\n- b\n"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("mapping");
  }

  @Test
  void rejectsANonMappingRulesSection() {
    assertThatThrownBy(() -> RuleConfigLoader.load("rules: [a, b]\n"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsSettingsThatAreNotAMapping() {
    assertThatThrownBy(() -> RuleConfigLoader.load("rules:\n  TQL001: disabled\n"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("TQL001");
  }

  @Test
  void rejectsOptionsThatAreNotAMapping() {
    assertThatThrownBy(() -> RuleConfigLoader.load("rules:\n  TQL006:\n    options: [a]\n"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("options");
  }

  @Test
  void rejectsAnExcludeThatIsNotAList() {
    assertThatThrownBy(() -> RuleConfigLoader.load("exclude: not-a-list\n"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("exclude");
  }

  @Test
  void loadsFromAFileOnDisk(@TempDir Path dir) throws Exception {
    Path file = dir.resolve(".tql.yaml");
    Files.writeString(file, "rules:\n  TQL001:\n    enabled: false\n");
    RuleConfig config = RuleConfigLoader.load(file);
    assertThat(config.isEnabled("TQL001")).isFalse();
  }

  @Test
  void wrapsAMissingFileAsUncheckedIOException() {
    assertThatThrownBy(() -> RuleConfigLoader.load(Path.of("/no/such/.tql.yaml")))
        .isInstanceOf(java.io.UncheckedIOException.class);
  }
}
