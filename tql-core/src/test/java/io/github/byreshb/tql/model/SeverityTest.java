package io.github.byreshb.tql.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SeverityTest {

  @Test
  void ordersInfoBelowWarnBelowError() {
    assertThat(Severity.ERROR.atLeast(Severity.WARN)).isTrue();
    assertThat(Severity.WARN.atLeast(Severity.WARN)).isTrue();
    assertThat(Severity.INFO.atLeast(Severity.WARN)).isFalse();
  }

  @Test
  void parsesNamesAndAliasesCaseInsensitively() {
    assertThat(Severity.parse("warn")).isEqualTo(Severity.WARN);
    assertThat(Severity.parse(" Warning ")).isEqualTo(Severity.WARN);
    assertThat(Severity.parse("information")).isEqualTo(Severity.INFO);
    assertThat(Severity.parse("ERROR")).isEqualTo(Severity.ERROR);
    assertThatThrownBy(() -> Severity.parse("fatal")).isInstanceOf(IllegalArgumentException.class);
  }
}
