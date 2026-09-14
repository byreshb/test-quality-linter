package io.github.byreshb.tql;

import io.github.byreshb.tql.engine.Linter;
import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.LintResult;
import io.github.byreshb.tql.model.SourceFile;
import io.github.byreshb.tql.rule.Rule;
import io.github.byreshb.tql.rule.RuleConfig;
import io.github.byreshb.tql.rule.RuleRegistry;
import java.util.List;

/** Runs the linter over inline snippets in tests. */
public final class LintSupport {

  private LintSupport() {}

  /** Lints one in-memory file named {@code SampleTest.java} with a single rule. */
  public static List<Finding> lint(Rule rule, String source) {
    return lint(rule, RuleConfig.defaults(), source);
  }

  /** Lints one in-memory file named {@code SampleTest.java} with a single rule and config. */
  public static List<Finding> lint(Rule rule, RuleConfig config, String source) {
    return result(RuleRegistry.of(rule), config, source).findings();
  }

  /** Lints one in-memory file with every discovered rule. */
  public static LintResult lintAll(String source) {
    return result(RuleRegistry.discover(), RuleConfig.defaults(), source);
  }

  /** Lints one in-memory file with the given registry and configuration. */
  public static LintResult result(RuleRegistry registry, RuleConfig config, String source) {
    return new Linter(registry, config).lint(List.of(SourceFile.of("SampleTest.java", source)));
  }

  /** Wraps method bodies in a JUnit 5 test class with the usual static imports. */
  public static String testClass(String members) {
    return """
    import static org.junit.jupiter.api.Assertions.*;
    import static org.assertj.core.api.Assertions.assertThat;
    import static org.hamcrest.MatcherAssert.assertThat;
    import static org.hamcrest.Matchers.*;
    import static org.mockito.Mockito.*;

    import org.junit.jupiter.api.Test;

    class SampleTest {
    """
        + members
        + "\n}\n";
  }
}
