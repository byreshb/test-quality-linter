package io.github.byreshb.tql.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.byreshb.tql.model.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleConfigTest {

  @Test
  void defaultsEnableEverythingWithoutOverrides() {
    RuleConfig config = RuleConfig.defaults();
    assertThat(config.isEnabled("TQL001")).isTrue();
    assertThat(config.severityOf("TQL001", Severity.WARN)).isEqualTo(Severity.WARN);
    assertThat(config.option("TQL001", "x")).isEmpty();
    assertThat(config.excludes()).isEmpty();
    assertThat(config).isEqualTo(RuleConfig.builder().build());
    assertThat(config.hashCode()).isEqualTo(RuleConfig.builder().build().hashCode());
  }

  @Test
  void builderRecordsDisabledSeveritiesOptionsAndExcludes() {
    RuleConfig config =
        RuleConfig.builder()
            .disable("TQL003")
            .disable("TQL004")
            .enable("TQL004")
            .severity("TQL001", Severity.INFO)
            .option("TQL006", "maxMillis", 100)
            .option("TQL003", "assertions", List.of("check", "expect"))
            .option("TQL009", "strict", "true")
            .exclude("**/generated/**")
            .build();
    assertThat(config.isEnabled("TQL003")).isFalse();
    assertThat(config.isEnabled("TQL004")).isTrue();
    assertThat(config.disabled()).containsExactly("TQL003");
    assertThat(config.severities()).containsEntry("TQL001", Severity.INFO);
    assertThat(config.severityOf("TQL001", Severity.ERROR)).isEqualTo(Severity.INFO);
    assertThat(config.intOption("TQL006", "maxMillis", 5)).isEqualTo(100);
    assertThat(config.intOption("TQL006", "other", 5)).isEqualTo(5);
    assertThat(config.listOption("TQL003", "assertions", List.of()))
        .containsExactly("check", "expect");
    assertThat(config.listOption("TQL009", "strict", List.of())).containsExactly("true");
    assertThat(config.listOption("TQL009", "none", List.of("d"))).containsExactly("d");
    assertThat(config.booleanOption("TQL009", "strict", false)).isTrue();
    assertThat(config.booleanOption("TQL009", "none", false)).isFalse();
    assertThat(config.stringOption("TQL006", "maxMillis", "")).isEqualTo("100");
    assertThat(config.stringOption("TQL006", "none", "x")).isEqualTo("x");
    assertThat(config.excludes()).containsExactly("**/generated/**");
    assertThat(config.toString()).contains("TQL003").contains("generated");
    assertThat(config).isNotEqualTo(RuleConfig.defaults());
  }

  @Test
  void parsesIntegerOptionsGivenAsStringsAndRejectsGarbage() {
    RuleConfig config =
        RuleConfig.builder()
            .option("TQL006", "maxMillis", " 42 ")
            .option("TQL006", "bad", "many")
            .option("TQL009", "flag", true)
            .build();
    assertThat(config.intOption("TQL006", "maxMillis", 0)).isEqualTo(42);
    assertThat(config.booleanOption("TQL009", "flag", false)).isTrue();
    assertThatThrownBy(() -> config.intOption("TQL006", "bad", 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("bad");
  }
}
