package io.github.byreshb.tql.engine;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.SourceFile;
import io.github.byreshb.tql.rule.RuleConfig;
import io.github.byreshb.tql.rule.RuleRegistry;
import io.github.byreshb.tql.rules.ConstantAssertion;
import io.github.byreshb.tql.rules.TautologicalAssertion;
import java.util.List;
import org.junit.jupiter.api.Test;

class SuppressionsTest {

  private static final Linter LINTER =
      new Linter(
          RuleRegistry.of(new TautologicalAssertion(), new ConstantAssertion()),
          RuleConfig.defaults());

  private static List<Finding> lint(String source) {
    return LINTER.lint(List.of(SourceFile.of("SampleTest.java", source))).findings();
  }

  @Test
  void bareTqlIgnoreSuppressesEveryFindingOnTheLine() {
    String source =
        """
        import static org.junit.jupiter.api.Assertions.*;
        import org.junit.jupiter.api.Test;

        class SampleTest {
          @Test
          void t() {
            int x = 1;
            assertEquals(x, x); // tql:ignore
          }
        }
        """;
    assertThat(lint(source)).isEmpty();
  }

  @Test
  void tqlIgnoreWithAnIdOnlySuppressesThatRule() {
    String source =
        """
        import static org.junit.jupiter.api.Assertions.*;
        import org.junit.jupiter.api.Test;

        class SampleTest {
          @Test
          void t() {
            int x = 1;
            assertEquals(x, x); // tql:ignore TQL001
          }
        }
        """;
    assertThat(lint(source)).isEmpty();

    String otherRuleOnly =
        """
        import static org.junit.jupiter.api.Assertions.*;
        import org.junit.jupiter.api.Test;

        class SampleTest {
          @Test
          void t() {
            assertEquals(2, 1 + 1); // tql:ignore TQL001
          }
        }
        """;
    assertThat(lint(otherRuleOnly)).extracting(Finding::ruleId).containsExactly("TQL002");
  }

  @Test
  void tqlIgnoreAcceptsMultipleCommaOrSpaceSeparatedIds() {
    String source =
        """
        import static org.junit.jupiter.api.Assertions.*;
        import org.junit.jupiter.api.Test;

        class SampleTest {
          @Test
          void t() {
            int x = 1;
            assertEquals(x, x); // tql:ignore TQL001, TQL002
          }
        }
        """;
    assertThat(lint(source)).isEmpty();
  }

  @Test
  void suppressWarningsOnAMethodSuppressesFindingsInsideItsRange() {
    String source =
        """
        import static org.junit.jupiter.api.Assertions.*;
        import org.junit.jupiter.api.Test;

        class SampleTest {
          @SuppressWarnings("tql:TQL001")
          @Test
          void t() {
            int x = 1;
            assertEquals(x, x);
          }
        }
        """;
    assertThat(lint(source)).isEmpty();
  }

  @Test
  void suppressWarningsWithAnArrayOfValuesSuppressesEachListedRule() {
    String source =
        """
        import static org.junit.jupiter.api.Assertions.*;
        import org.junit.jupiter.api.Test;

        class SampleTest {
          @SuppressWarnings({"unchecked", "tql:TQL001", "tql:TQL002"})
          @Test
          void t() {
            int x = 1;
            assertEquals(x, x);
            assertEquals(2, 1 + 1);
          }
        }
        """;
    assertThat(lint(source)).isEmpty();
  }

  @Test
  void suppressWarningsWithAStarSuppressesEveryRule() {
    String source =
        """
        import static org.junit.jupiter.api.Assertions.*;
        import org.junit.jupiter.api.Test;

        class SampleTest {
          @SuppressWarnings("tql:*")
          @Test
          void t() {
            int x = 1;
            assertEquals(x, x);
            assertEquals(2, 1 + 1);
          }
        }
        """;
    assertThat(lint(source)).isEmpty();
  }

  @Test
  void suppressWarningsWithAnOrdinaryValueDoesNothing() {
    String source =
        """
        import static org.junit.jupiter.api.Assertions.*;
        import org.junit.jupiter.api.Test;

        class SampleTest {
          @SuppressWarnings("unchecked")
          @Test
          void t() {
            int x = 1;
            assertEquals(x, x);
          }
        }
        """;
    assertThat(lint(source)).extracting(Finding::ruleId).containsExactly("TQL001");
  }

  @Test
  void suppressWarningsOnAnotherMethodDoesNotLeak() {
    String source =
        """
        import static org.junit.jupiter.api.Assertions.*;
        import org.junit.jupiter.api.Test;

        class SampleTest {
          @SuppressWarnings("tql:TQL001")
          @Test
          void suppressed() {
            int x = 1;
            assertEquals(x, x);
          }

          @Test
          void notSuppressed() {
            int y = 1;
            assertEquals(y, y);
          }
        }
        """;
    assertThat(lint(source)).extracting(Finding::message).hasSize(1);
  }

  @Test
  void findsWithNoIgnoreCommentOrAnnotationAreUnaffected() {
    String source =
        """
        import static org.junit.jupiter.api.Assertions.*;
        import org.junit.jupiter.api.Test;

        class SampleTest {
          @Test
          void t() {
            int x = 1;
            assertEquals(x, x);
          }
        }
        """;
    assertThat(lint(source)).hasSize(1);
  }
}
