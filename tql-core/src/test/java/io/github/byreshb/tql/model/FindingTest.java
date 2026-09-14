package io.github.byreshb.tql.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FindingTest {

  private static Finding sample() {
    return new Finding(
        "TQL001",
        Severity.ERROR,
        Path.of("src/test/java/FooTest.java"),
        12,
        5,
        "assertEquals compares x with itself",
        "Compare against an expected value",
        Optional.of("assertEquals(x, x);"));
  }

  @Test
  void rendersOneLineForm() {
    assertThat(sample().toLine())
        .isEqualTo(
            "src/test/java/FooTest.java:12:5: ERROR TQL001 assertEquals compares x with itself");
  }

  @Test
  void copiesWithAnotherSeverity() {
    Finding copy = sample().withSeverity(Severity.INFO);
    assertThat(copy.severity()).isEqualTo(Severity.INFO);
    assertThat(copy.ruleId()).isEqualTo("TQL001");
    assertThat(copy.snippet()).contains("assertEquals(x, x);");
    assertThat(copy).isNotEqualTo(sample());
  }

  @Test
  void rejectsZeroBasedPositions() {
    assertThatThrownBy(
            () ->
                new Finding(
                    "TQL001", Severity.WARN, Path.of("A.java"), 0, 1, "m", "f", Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
