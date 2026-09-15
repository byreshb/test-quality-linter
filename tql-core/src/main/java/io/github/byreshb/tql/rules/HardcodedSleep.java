package io.github.byreshb.tql.rules;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.Severity;
import io.github.byreshb.tql.rule.AbstractRule;
import io.github.byreshb.tql.rule.ConstantExpressions;
import io.github.byreshb.tql.rule.RuleContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * TQL006: a fixed delay where a wait for a condition belongs: {@code Thread.sleep}, {@code
 * TimeUnit.SECONDS.sleep}, Playwright's {@code page.waitForTimeout}, Guava's {@code
 * sleepUninterruptibly}, and Awaitility chains that only set a {@code pollDelay} and never wait for
 * anything.
 *
 * <p>Option {@code maxMillis}: sleeps of at most this many milliseconds are tolerated (default 0).
 */
public final class HardcodedSleep extends AbstractRule {

  /** The rule id. */
  public static final String ID = "TQL006";

  private static final String FIX =
      "Wait for the condition instead: Awaitility's await().until(...), a CountDownLatch, a"
          + " CompletableFuture, or an auto-waiting assertion";

  private static final Set<String> TIME_UNITS =
      Set.of(
          "NANOSECONDS",
          "MICROSECONDS",
          "MILLISECONDS",
          "SECONDS",
          "MINUTES",
          "HOURS",
          "DAYS",
          "TimeUnit");

  /** Creates the rule. */
  public HardcodedSleep() {
    super(
        ID,
        "HardcodedSleep",
        "A fixed sleep or timeout is used instead of waiting for a condition.",
        Severity.WARN);
  }

  @Override
  public List<Finding> check(CompilationUnit unit, RuleContext context) {
    int maxMillis = context.config().intOption(ID, "maxMillis", 0);
    List<Finding> findings = new ArrayList<>();
    for (MethodCallExpr call : unit.findAll(MethodCallExpr.class)) {
      String name = call.getNameAsString();
      Optional<Expression> scope = call.getScope();
      if (name.equals("sleep") && scope.isPresent() && isThread(scope.get())) {
        if (millis(call).orElse(Long.MAX_VALUE) > maxMillis) {
          findings.add(context.finding(this, call, "Thread.sleep delays the test", FIX));
        }
      } else if (name.equals("sleep") && scope.isPresent() && isTimeUnit(scope.get())) {
        findings.add(context.finding(this, call, "TimeUnit.sleep delays the test", FIX));
      } else if (name.equals("sleepUninterruptibly")) {
        findings.add(context.finding(this, call, "sleepUninterruptibly delays the test", FIX));
      } else if (name.equals("waitForTimeout")) {
        findings.add(
            context.finding(
                this,
                call,
                "waitForTimeout is a fixed delay; Playwright locators and assertions wait on"
                    + " their own",
                "Wait for the element or state with an auto-waiting locator action or"
                    + " assertThat(locator)"));
      } else if (name.equals("pollDelay") && isAwaitilityUsedAsSleep(call, unit)) {
        findings.add(
            context.finding(
                this,
                call,
                "Awaitility chain with pollDelay never waits for a real condition",
                "Replace pollDelay with until(...) or untilAsserted(...) on the condition the"
                    + " test needs"));
      }
    }
    return findings;
  }

  private static boolean isThread(Expression scope) {
    return scope instanceof NameExpr n && n.getNameAsString().equals("Thread")
        || scope instanceof FieldAccessExpr f && f.getNameAsString().equals("Thread");
  }

  private static boolean isTimeUnit(Expression scope) {
    if (scope instanceof NameExpr n) {
      return TIME_UNITS.contains(n.getNameAsString());
    }
    if (scope instanceof FieldAccessExpr f) {
      return TIME_UNITS.contains(f.getNameAsString());
    }
    if (scope instanceof MethodCallExpr m) {
      return m.getNameAsString().equals("of") && isTimeUnit(m.getScope().orElse(null));
    }
    return false;
  }

  private static Optional<Long> millis(MethodCallExpr call) {
    if (call.getArguments().size() != 1) {
      return Optional.empty();
    }
    Expression argument = ConstantExpressions.unwrap(call.getArgument(0));
    if (argument instanceof IntegerLiteralExpr i) {
      return Optional.of(i.asNumber().longValue());
    }
    if (argument instanceof LongLiteralExpr l) {
      return Optional.of(l.asNumber().longValue());
    }
    return Optional.empty();
  }

  /**
   * An Awaitility chain is a disguised sleep when it sets a pollDelay and its terminal {@code
   * until*} call is missing or waits for a condition that is trivially true.
   */
  private static boolean isAwaitilityUsedAsSleep(MethodCallExpr pollDelay, CompilationUnit unit) {
    MethodCallExpr outermost = pollDelay;
    boolean rootedAtAwait = false;
    Expression current = pollDelay;
    while (current instanceof MethodCallExpr m) {
      if (m.getNameAsString().equals("await")) {
        rootedAtAwait = true;
      }
      current = m.getScope().orElse(null);
    }
    if (!rootedAtAwait) {
      return false;
    }
    while (outermost.getParentNode().isPresent()
        && outermost.getParentNode().get() instanceof MethodCallExpr parent
        && parent.getScope().isPresent()
        && parent.getScope().get() == outermost) {
      outermost = parent;
    }
    String terminal = outermost.getNameAsString();
    if (!terminal.startsWith("until")) {
      return true;
    }
    if (outermost.getArguments().size() != 1) {
      return false;
    }
    Expression condition = outermost.getArgument(0);
    if (condition instanceof LambdaExpr lambda) {
      if (lambda.getBody() instanceof BlockStmt block) {
        return block.getStatements().isEmpty();
      }
      if (lambda.getBody() instanceof ExpressionStmt stmt) {
        return ConstantExpressions.isConstant(stmt.getExpression(), unit);
      }
    }
    return false;
  }
}
