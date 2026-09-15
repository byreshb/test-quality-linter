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

class NoAssertionTest {

  private final NoAssertion rule = new NoAssertion();

  private List<Finding> lintBody(String body) {
    return lint(rule, testClass("  @Test\n  void t() {\n" + body + "\n  }"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "service.save(order);",
        "Object result = service.find(1);",
        "service.save(order); System.out.println(\"saved\");",
        "try { service.save(order); } catch (Exception e) { }",
        "helperWithoutAssertions();",
        "mock.doSomething();",
        "when(mock.get()).thenReturn(1);",
        "log.info(\"done\");"
      })
  void flagsTestsWithoutAssertions(String body) {
    List<Finding> findings = lintBody(body);
    assertThat(findings).hasSize(1);
    Finding finding = findings.get(0);
    assertThat(finding.ruleId()).isEqualTo("TQL003");
    assertThat(finding.severity()).isEqualTo(Severity.WARN);
    assertThat(finding.message()).isEqualTo("Test 't' has no assertion");
    assertThat(finding.line()).isEqualTo(11);
    assertThat(finding.fixHint()).contains("assertThrows");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "assertEquals(1, service.count());",
        "Assertions.assertTrue(service.isEmpty());",
        "assertThat(service.count()).isEqualTo(1);",
        "assertThat(service.count(), is(1));",
        "assertThrows(IllegalStateException.class, () -> service.fail());",
        "assertDoesNotThrow(() -> service.run());",
        "assertAll(() -> assertEquals(1, 1));",
        "verify(mock).save(order);",
        "verifyNoInteractions(mock);",
        "Mockito.verify(mock, times(2)).save(order);",
        "inOrder(mock).verify(mock).save(order);",
        "assertThatThrownBy(() -> service.fail()).isInstanceOf(Exception.class);",
        "try { service.fail(); fail(\"expected\"); } catch (Exception e) { }",
        "assertOrderSaved(order);",
        "verifySaved(order);",
        "expectSaved(order);",
        "shouldBeSaved(order);",
        "checkSaved(order);",
        "thrown.expect(IllegalStateException.class); service.fail();",
        "assertThat(page.getByRole(BUTTON)).isVisible();",
        "helperWithAssertion();",
        "outerHelper();",
        "custom(order);",
      })
  void acceptsAssertionsVerificationsAndAssertingHelpers(String body) {
    String members =
        """
          @Test
          void t() {
        """
            + body
            + """

              }

              private void helperWithAssertion() {
                assertTrue(true);
              }

              private void outerHelper() {
                helperWithAssertion();
              }

              private void helperWithoutAssertions() {
                service.save(order);
              }
            """;
    RuleConfig config = RuleConfig.builder().option("TQL003", "methods", List.of("custom")).build();
    assertThat(lint(rule, config, testClass(members))).isEmpty();
  }

  @Test
  void skipsTestsThatDeclareAnExpectedException() {
    String members =
        """
          @Test(expected = IllegalStateException.class)
          public void junit4() {
            service.fail();
          }

          @org.testng.annotations.Test(expectedExceptions = RuntimeException.class)
          public void testng() {
            service.fail();
          }

          @Test
          void plain() {
            service.fail();
          }
        """;
    assertThat(lint(rule, testClass(members)))
        .extracting(Finding::message)
        .containsExactly("Test 'plain' has no assertion");
  }

  @Test
  void countsStaticallyImportedMethodsOfConfiguredPackagesAsAssertions() {
    String source =
        """
        import static com.acme.testing.Checks.ensureSaved;
        import static com.acme.testing.Checks.*;
        import static org.assertj.core.api.Assertions.assertThat;
        import org.junit.jupiter.api.Test;

        class SampleTest {
          @Test
          void imported() {
            ensureSaved(order);
          }

          @Test
          void wildcardOnly() {
            ensureLoaded(order);
          }
        }
        """;
    assertThat(lint(rule, source))
        .extracting(Finding::message)
        .containsExactly(
            "Test 'imported' has no assertion", "Test 'wildcardOnly' has no assertion");
    RuleConfig config =
        RuleConfig.builder().option("TQL003", "packages", List.of("com.acme.testing")).build();
    assertThat(lint(rule, config, source))
        .extracting(Finding::message)
        .containsExactly("Test 'wildcardOnly' has no assertion");
  }

  @Test
  void handlesMutuallyRecursiveHelpersAndAbstractTests() {
    String members =
        """
          @Test
          void t() {
            a();
          }

          private void a() {
            b();
          }

          private void b() {
            a();
          }

          @Test
          abstract void template();
        """;
    assertThat(lint(rule, testClass(members)))
        .extracting(Finding::message)
        .containsExactly("Test 't' has no assertion", "Test 'template' has no assertion");
    assertThat(rule.description()).contains("no assertion");
  }
}
