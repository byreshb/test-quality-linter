package io.github.byreshb.tql.rules;

import static io.github.byreshb.tql.LintSupport.lint;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;

class AssertsOnStubTest {

  private final AssertsOnStub rule = new AssertsOnStub();

  private static String testClass(String members) {
    return """
    import static org.junit.jupiter.api.Assertions.*;
    import static org.assertj.core.api.Assertions.assertThat;
    import static org.hamcrest.MatcherAssert.assertThat;
    import static org.hamcrest.Matchers.*;
    import static org.mockito.Mockito.*;
    import static org.mockito.BDDMockito.*;
    import static org.easymock.EasyMock.*;

    import org.junit.jupiter.api.Test;

    class SampleTest {
    """
        + members
        + "\n}\n";
  }

  @Test
  void flagsMockitoWhenThenReturnAssertedBackAgainstTheSameLiteral() {
    String members =
        """
          Repository repo = mock(Repository.class);

          @Test
          void t() {
            when(repo.count()).thenReturn(5);
            assertEquals(5, repo.count());
          }
        """;
    List<Finding> findings = lint(rule, testClass(members));
    assertThat(findings).hasSize(1);
    Finding finding = findings.get(0);
    assertThat(finding.ruleId()).isEqualTo("TQL004");
    assertThat(finding.severity()).isEqualTo(Severity.ERROR);
    assertThat(finding.message()).contains("repo.count()").contains("5");
    assertThat(finding.fixHint()).contains("code under test");
  }

  @Test
  void flagsTheReverseOperandOrderAndFluentAssertJ() {
    String members =
        """
          Repository repo = mock(Repository.class);

          @Test
          void reversed() {
            when(repo.count()).thenReturn(5);
            assertEquals(repo.count(), 5);
          }

          @Test
          void fluent() {
            when(repo.count()).thenReturn(5);
            assertThat(repo.count()).isEqualTo(5);
          }

          @Test
          void hamcrest() {
            when(repo.count()).thenReturn(5);
            assertThat(repo.count(), is(5));
          }
        """;
    assertThat(lint(rule, testClass(members))).hasSize(3);
  }

  @Test
  void flagsBddMockitoGivenAndEasyMockExpect() {
    String members =
        """
          Repository repo = mock(Repository.class);

          @Test
          void bdd() {
            given(repo.count()).willReturn(5);
            assertEquals(5, repo.count());
          }

          @Test
          void easyMock() {
            expect(repo.count()).andReturn(5);
            assertEquals(5, repo.count());
          }
        """;
    assertThat(lint(rule, testClass(members))).hasSize(2);
  }

  @Test
  void flagsDoReturnWhenStyleStubs() {
    String members =
        """
          Repository repo = mock(Repository.class);

          @Test
          void t() {
            doReturn(5).when(repo).count();
            assertEquals(5, repo.count());
          }
        """;
    assertThat(lint(rule, testClass(members))).hasSize(1);
  }

  @Test
  void picksUpStubsSetUpInBeforeEach() {
    String members =
        """
          Repository repo = mock(Repository.class);

          @org.junit.jupiter.api.BeforeEach
          void setUp() {
            when(repo.count()).thenReturn(5);
          }

          @Test
          void t() {
            assertEquals(5, repo.count());
          }
        """;
    assertThat(lint(rule, testClass(members))).hasSize(1);
  }

  @Test
  void leavesGenuineAssertionsAndUnrelatedValuesAlone() {
    String members =
        """
          Repository repo = mock(Repository.class);

          @Test
          void differentValue() {
            when(repo.count()).thenReturn(5);
            assertEquals(6, repo.count());
          }

          @Test
          void differentCall() {
            when(repo.count()).thenReturn(5);
            assertEquals(5, repo.total());
          }

          @Test
          void noStub() {
            assertEquals(5, repo.count());
          }

          @Test
          void assertsOnRealComputation() {
            when(repo.count()).thenReturn(5);
            int doubled = repo.count() * 2;
            assertEquals(10, doubled);
          }
        """;
    assertThat(lint(rule, testClass(members))).isEmpty();
  }

  @Test
  void describesItself() {
    assertThat(rule.id()).isEqualTo("TQL004");
    assertThat(rule.name()).isEqualTo("AssertsOnStub");
    assertThat(rule.description()).contains("stubbed");
  }

  interface Repository {
    int count();

    int total();
  }
}
