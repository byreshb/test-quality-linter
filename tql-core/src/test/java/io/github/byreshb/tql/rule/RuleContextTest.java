package io.github.byreshb.tql.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.Severity;
import io.github.byreshb.tql.model.SourceFile;
import io.github.byreshb.tql.rules.TautologicalAssertion;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RuleContextTest {

  private static final String SOURCE = "class A {\n  void m() {\n    foo(1);\n  }\n}\n";

  @Test
  void buildsFindingsFromNodesWithSnippetAndConfiguredSeverity() {
    SourceFile source = SourceFile.of("A.java", SOURCE);
    RuleConfig config =
        RuleConfig.builder().severity(TautologicalAssertion.ID, Severity.INFO).build();
    RuleContext context = new RuleContext(source, config, false);
    CompilationUnit unit = StaticJavaParser.parse(SOURCE);
    MethodCallExpr call = unit.findFirst(MethodCallExpr.class).orElseThrow();

    Finding finding = context.finding(new TautologicalAssertion(), call, "msg", "fix");

    assertThat(finding.file()).isEqualTo(Path.of("A.java"));
    assertThat(finding.line()).isEqualTo(3);
    assertThat(finding.column()).isEqualTo(5);
    assertThat(finding.severity()).isEqualTo(Severity.INFO);
    assertThat(finding.snippet()).contains("foo(1);");
    assertThat(context.source()).isSameAs(source);
    assertThat(context.config()).isSameAs(config);
    assertThat(context.symbolsResolved()).isFalse();
  }

  @Test
  void omitsSnippetForUnknownLinesAndClampsPositions() {
    RuleContext context =
        new RuleContext(SourceFile.of("A.java", SOURCE), RuleConfig.defaults(), true);
    Finding finding = context.finding(new TautologicalAssertion(), 99, 0, "msg", "fix");
    assertThat(finding.snippet()).isEmpty();
    assertThat(finding.column()).isEqualTo(1);
    assertThat(finding.severity()).isEqualTo(Severity.ERROR);
    assertThat(context.symbolsResolved()).isTrue();
  }
}
