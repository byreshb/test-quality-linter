package io.github.byreshb.tql.rules;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.Severity;
import io.github.byreshb.tql.rule.AbstractRule;
import io.github.byreshb.tql.rule.RuleContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * TQL007: a {@code catch} block in a test that is empty or only prints or logs the exception, so a
 * failure in the code under test turns into a green test. The classic {@code try { ...; fail(); }
 * catch (Expected e) {}} pattern is recognised and left alone.
 */
public final class SwallowedException extends AbstractRule {

  /** The rule id. */
  public static final String ID = "TQL007";

  private static final String FIX =
      "Let the exception propagate (declare 'throws Exception' on the test) or assert it with"
          + " assertThrows";

  private static final Set<String> PRINTING =
      Set.of("printStackTrace", "println", "print", "printf");

  private static final Set<String> LOGGING =
      Set.of("trace", "debug", "info", "warn", "warning", "error", "severe", "fine", "log");

  private static final Pattern LOGGER_NAME = Pattern.compile("(?i)^(log|logger|logging)$");

  /** Creates the rule. */
  public SwallowedException() {
    super(
        ID,
        "SwallowedException",
        "A catch block in a test is empty or only prints or logs the exception.",
        Severity.WARN);
  }

  @Override
  public List<Finding> check(CompilationUnit unit, RuleContext context) {
    List<Finding> findings = new ArrayList<>();
    for (TryStmt tryStmt : unit.findAll(TryStmt.class)) {
      if (endsWithFail(tryStmt)) {
        continue;
      }
      for (CatchClause clause : tryStmt.getCatchClauses()) {
        List<Statement> statements = clause.getBody().getStatements();
        if (statements.isEmpty()) {
          findings.add(
              context.finding(
                  this,
                  clause,
                  "Empty catch block swallows " + clause.getParameter().getTypeAsString(),
                  FIX));
        } else if (statements.stream().allMatch(SwallowedException::onlyPrintsOrLogs)) {
          findings.add(
              context.finding(
                  this,
                  clause,
                  "Catch block only prints or logs " + clause.getParameter().getTypeAsString(),
                  FIX));
        }
      }
    }
    return findings;
  }

  private static boolean endsWithFail(TryStmt tryStmt) {
    return tryStmt.getTryBlock().findAll(MethodCallExpr.class).stream()
        .anyMatch(call -> call.getNameAsString().equals("fail"));
  }

  private static boolean onlyPrintsOrLogs(Statement statement) {
    if (!(statement instanceof ExpressionStmt expressionStmt)
        || !(expressionStmt.getExpression() instanceof MethodCallExpr call)) {
      return false;
    }
    String name = call.getNameAsString();
    if (PRINTING.contains(name)) {
      return true;
    }
    if (!LOGGING.contains(name) || call.getScope().isEmpty()) {
      return false;
    }
    Expression scope = call.getScope().get();
    return scope instanceof NameExpr n && LOGGER_NAME.matcher(n.getNameAsString()).matches()
        || scope.toString().toLowerCase(java.util.Locale.ROOT).contains("log");
  }
}
