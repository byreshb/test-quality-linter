package io.github.byreshb.tql.rules;

import static io.github.byreshb.tql.LintSupport.lint;
import static io.github.byreshb.tql.LintSupport.testClass;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SwallowedExceptionTest {

  private final SwallowedException rule = new SwallowedException();

  private List<Finding> lintCatch(String body) {
    return lint(
        rule,
        testClass(
            "  @Test\n  void t() {\n    try {\n      service.run();\n    } catch (Exception e) {\n"
                + body
                + "\n    }\n  }"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "// nothing to do",
        "e.printStackTrace();",
        "System.out.println(e.getMessage());",
        "System.err.println(e);",
        "System.out.printf(\"%s\", e);",
        "log.error(\"failed\", e);",
        "LOG.warn(\"failed\", e);",
        "logger.info(e.getMessage());",
        "LOGGER.log(Level.SEVERE, \"x\", e);",
        "Logger.getLogger(\"x\").warning(e.getMessage());",
        "log.error(\"failed\", e); e.printStackTrace();",
      })
  void flagsCatchBlocksThatHideTheFailure(String body) {
    List<Finding> findings = lintCatch(body);
    assertThat(findings).hasSize(1);
    Finding finding = findings.get(0);
    assertThat(finding.ruleId()).isEqualTo("TQL007");
    assertThat(finding.severity()).isEqualTo(Severity.WARN);
    assertThat(finding.line()).isEqualTo(14);
    assertThat(finding.message()).contains("Exception");
    assertThat(finding.fixHint()).contains("assertThrows");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "throw new IllegalStateException(e);",
        "fail(\"unexpected: \" + e);",
        "assertEquals(\"boom\", e.getMessage());",
        "assertThat(e).hasMessage(\"boom\");",
        "failure = e;",
        "log.error(\"failed\", e); throw e;",
        "caught.add(e);",
        "handler.error(e);",
      })
  void acceptsCatchBlocksThatReactToTheException(String body) {
    assertThat(lintCatch(body)).isEmpty();
  }

  @Test
  void acceptsTheTryFailCatchPattern() {
    String members =
        """
          @Test
          void t() {
            try {
              service.run();
              fail("expected an exception");
            } catch (IllegalStateException expected) {
            }
          }
        """;
    assertThat(lint(rule, testClass(members))).isEmpty();
  }

  @Test
  void flagsEachOffendingClauseOfMultiCatch() {
    String members =
        """
          @Test
          void t() {
            try {
              service.run();
            } catch (IllegalStateException e) {
              assertNotNull(e);
            } catch (RuntimeException e) {
            } catch (Exception e) {
              e.printStackTrace();
            } finally {
              service.close();
            }
          }
        """;
    assertThat(lint(rule, testClass(members)))
        .extracting(Finding::message)
        .containsExactly(
            "Empty catch block swallows RuntimeException",
            "Catch block only prints or logs Exception");
    assertThat(rule.description()).contains("catch");
  }
}
