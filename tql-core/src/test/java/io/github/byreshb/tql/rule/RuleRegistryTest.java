package io.github.byreshb.tql.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.javaparser.ast.CompilationUnit;
import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.Severity;
import io.github.byreshb.tql.rules.TautologicalAssertion;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleRegistryTest {

  private static final class Stub extends AbstractRule {
    Stub(String id) {
      super(id, "Stub", "A stub rule.", Severity.INFO);
    }

    @Override
    public List<Finding> check(CompilationUnit unit, RuleContext context) {
      return List.of();
    }
  }

  @Test
  void discoversBuiltInRulesThroughServiceLoader() {
    RuleRegistry registry = RuleRegistry.discover();
    assertThat(registry.rules()).extracting(Rule::id).contains(TautologicalAssertion.ID);
    assertThat(registry.find("tql001")).isPresent();
    assertThat(registry.find("TQL999")).isEmpty();
  }

  @Test
  void sortsByIdAndRejectsDuplicates() {
    RuleRegistry registry = RuleRegistry.of(new Stub("TQL900"), new Stub("TQL100"));
    assertThat(registry.rules()).extracting(Rule::id).containsExactly("TQL100", "TQL900");
    assertThat(registry.rules().get(0).toString()).isEqualTo("TQL100 Stub");
    assertThat(registry.rules().get(0).description()).isEqualTo("A stub rule.");
    assertThatThrownBy(() -> RuleRegistry.of(new Stub("TQL100"), new Stub("TQL100")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("TQL100");
  }

  @Test
  void filtersDisabledRules() {
    RuleRegistry registry = RuleRegistry.of(new Stub("TQL100"), new Stub("TQL200"));
    RuleConfig config = RuleConfig.builder().disable("TQL100").build();
    assertThat(registry.enabled(config)).extracting(Rule::id).containsExactly("TQL200");
  }
}
