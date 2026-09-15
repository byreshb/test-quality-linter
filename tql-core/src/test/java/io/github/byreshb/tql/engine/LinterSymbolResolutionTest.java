package io.github.byreshb.tql.engine;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.byreshb.tql.model.SourceFile;
import io.github.byreshb.tql.rule.RuleConfig;
import io.github.byreshb.tql.rule.RuleRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;

class LinterSymbolResolutionTest {

  @Test
  void twoArgConstructorDisablesSymbolResolution() {
    Linter linter = new Linter(RuleRegistry.discover(), RuleConfig.defaults());
    assertThat(linter.symbolsResolved()).isFalse();
    assertThat(Linter.withDefaults().symbolsResolved()).isFalse();
  }

  @Test
  void threeArgConstructorEnablesSymbolResolutionEvenWithAnEmptyClasspath() {
    Linter linter = new Linter(RuleRegistry.discover(), RuleConfig.defaults(), List.of());
    assertThat(linter.symbolsResolved()).isTrue();
    assertThat(Linter.withClasspath(List.of()).symbolsResolved()).isTrue();
  }

  @Test
  void resolvesJdkTypesThroughReflectionWithoutAnyClasspathEntries() {
    String source =
        """
        import org.junit.jupiter.api.Test;

        class SampleTest {
          @Test
          void t() {
            java.util.List<Integer> numbers = new java.util.ArrayList<>();
            numbers.add(1);
          }
        }
        """;
    Linter linter = Linter.withClasspath(List.of());
    // No crash while resolving java.util.ArrayList/List through reflection; parsing still
    // succeeds and produces no problems.
    var result = linter.lint(List.of(SourceFile.of("SampleTest.java", source)));
    assertThat(result.problems()).isEmpty();
  }
}
