package io.github.byreshb.tql.rules;

import static io.github.byreshb.tql.LintSupport.lint;
import static io.github.byreshb.tql.LintSupport.testClass;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.Severity;
import io.github.byreshb.tql.rule.RuleConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HardcodedSleepTest {

  private final HardcodedSleep rule = new HardcodedSleep();

  private List<Finding> lintStatement(String statement) {
    return lint(
        rule, testClass("  @Test\n  void t() throws Exception {\n    " + statement + "\n  }"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Thread.sleep(1000);",
        "java.lang.Thread.sleep(1000L);",
        "Thread.sleep(delay);",
        "Thread.sleep(1_000, 0);",
        "TimeUnit.SECONDS.sleep(1);",
        "SECONDS.sleep(1);",
        "java.util.concurrent.TimeUnit.MILLISECONDS.sleep(50);",
        "TimeUnit.of(ChronoUnit.SECONDS).sleep(1);",
        "Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);",
        "sleepUninterruptibly(Duration.ofSeconds(1));",
        "page.waitForTimeout(500);",
        "frame.waitForTimeout(500);",
        "await().pollDelay(2, SECONDS).until(() -> true);",
        "await().pollDelay(Duration.ofSeconds(2)).untilAsserted(() -> { });",
        "Awaitility.await().atMost(5, SECONDS).pollDelay(2, SECONDS).until(() -> true);",
        "await().pollDelay(2, SECONDS);",
      })
  void flagsFixedDelays(String statement) {
    List<Finding> findings = lintStatement(statement);
    assertThat(findings).hasSize(1);
    Finding finding = findings.get(0);
    assertThat(finding.ruleId()).isEqualTo("TQL006");
    assertThat(finding.severity()).isEqualTo(Severity.WARN);
    assertThat(finding.line()).isEqualTo(12);
    assertThat(finding.fixHint()).isNotBlank();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "await().until(() -> service.isDone());",
        "await().atMost(5, SECONDS).untilAsserted(() -> assertTrue(service.isDone()));",
        "await().pollDelay(100, MILLISECONDS).until(() -> service.isDone());",
        "await().pollDelay(100, MILLISECONDS).untilAsserted(() -> { assertTrue(done); });",
        "await().pollDelay(1, SECONDS).until(service::isDone);",
        "latch.await(5, TimeUnit.SECONDS);",
        "future.get(5, TimeUnit.SECONDS);",
        "executor.sleep(5);",
        "page.waitForSelector(\"#done\");",
        "worker.sleep();",
        "config.pollDelay(2);",
      })
  void acceptsRealWaits(String statement) {
    assertThat(lintStatement(statement)).isEmpty();
  }

  @Test
  void toleratesShortSleepsUpToMaxMillis() {
    RuleConfig config = RuleConfig.builder().option("TQL006", "maxMillis", 100).build();
    String members =
        """
          @Test
          void t() throws Exception {
            Thread.sleep(50);
            Thread.sleep(100);
            Thread.sleep(101L);
            Thread.sleep(unknown);
          }
        """;
    assertThat(lint(rule, config, testClass(members)))
        .extracting(Finding::line)
        .containsExactly(14, 15);
    assertThat(rule.description()).contains("sleep");
  }
}
