package io.github.byreshb.tql.rules;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.Severity;
import io.github.byreshb.tql.model.SourceFile;
import io.github.byreshb.tql.rule.RuleConfig;
import io.github.byreshb.tql.rule.RuleRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MockOfTypeUnderTestTest {

  private final MockOfTypeUnderTest rule = new MockOfTypeUnderTest();

  private List<Finding> lint(String className, String members) {
    String source =
        """
        import org.mockito.Mock;
        import static org.mockito.Mockito.mock;
        import org.junit.jupiter.api.Test;

        class %s {
        %s
        }
        """
            .formatted(className, members);
    return new io.github.byreshb.tql.engine.Linter(RuleRegistry.of(rule), RuleConfig.defaults())
        .lint(List.of(SourceFile.of(className + ".java", source)))
        .findings();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"OrderServiceTest", "OrderServiceTests", "OrderServiceIT", "OrderServiceTestCase"})
  void flagsAMockFieldOfTheClassUnderTest(String className) {
    List<Finding> findings = lint(className, "  @Mock\n  OrderService orderService;\n");
    assertThat(findings).hasSize(1);
    Finding finding = findings.get(0);
    assertThat(finding.ruleId()).isEqualTo("TQL012");
    assertThat(finding.severity()).isEqualTo(Severity.WARN);
    assertThat(finding.message()).contains("OrderService");
  }

  @Test
  void flagsAnExplicitMockCallOfTheClassUnderTest() {
    List<Finding> findings =
        lint("OrderServiceTest", "  OrderService orderService = mock(OrderService.class);\n");
    assertThat(findings).hasSize(1);
    assertThat(findings.get(0).message()).contains("mock(OrderService.class)");
  }

  @Test
  void acceptsMocksOfCollaborators() {
    assertThat(
            lint(
                "OrderServiceTest",
                "  @Mock\n"
                    + "  OrderRepository repository;\n\n"
                    + "  OrderService service = mock(OrderRepository.class);\n"))
        .isEmpty();
  }

  @Test
  void acceptsAClassNameWithoutARecognisedTestSuffix() {
    assertThat(lint("OrderServiceChecks", "  @Mock\n  OrderServiceChecks self;\n")).isEmpty();
  }

  @Test
  void describesItself() {
    assertThat(rule.id()).isEqualTo("TQL012");
    assertThat(rule.name()).isEqualTo("MockOfTypeUnderTest");
    assertThat(rule.description()).contains("mocked");
  }
}
