package io.github.byreshb.tql.rules;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.Severity;
import io.github.byreshb.tql.rule.AbstractRule;
import io.github.byreshb.tql.rule.Assertions;
import io.github.byreshb.tql.rule.ConstantExpressions;
import io.github.byreshb.tql.rule.RuleContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * TQL002: an assertion whose operands are all compile-time constants, such as {@code
 * assertEquals(2, 1 + 1)}, {@code assertTrue(1 < 2)} or {@code assertThat("a").isEqualTo("a")}. Its
 * outcome is decided when the test is written, not by the code under test.
 */
public final class ConstantAssertion extends AbstractRule {

  /** The rule id. */
  public static final String ID = "TQL002";

  private static final String FIX =
      "Make one side of the assertion a value produced by the code under test";

  private static final Set<String> EQUALITY_ASSERTIONS =
      Set.of("assertEquals", "assertNotEquals", "assertSame", "assertNotSame");

  private static final Set<String> BOOLEAN_ASSERTIONS = Set.of("assertTrue", "assertFalse");

  private static final Set<String> FLUENT_TERMINALS =
      Set.of(
          "isEqualTo",
          "isNotEqualTo",
          "isSameAs",
          "isGreaterThan",
          "isLessThan",
          "isTrue",
          "isFalse",
          "contains",
          "startsWith",
          "endsWith",
          "hasSize",
          "isPositive",
          "isNegative",
          "isZero",
          "isEmpty",
          "isNotEmpty",
          "isBlank",
          "isNotBlank");

  private static final Set<String> HAMCREST_MATCHERS =
      Set.of("is", "equalTo", "not", "greaterThan", "lessThan", "containsString", "startsWith");

  /** Creates the rule. */
  public ConstantAssertion() {
    super(
        ID,
        "ConstantAssertion",
        "Both sides of an assertion are literals or compile-time constants.",
        Severity.ERROR);
  }

  @Override
  public List<Finding> check(CompilationUnit unit, RuleContext context) {
    List<Finding> findings = new ArrayList<>();
    for (MethodCallExpr call : unit.findAll(MethodCallExpr.class)) {
      checkStatic(call, unit, context).ifPresent(findings::add);
      checkFluent(call, unit, context).ifPresent(findings::add);
    }
    return findings;
  }

  private Optional<Finding> checkStatic(
      MethodCallExpr call, CompilationUnit unit, RuleContext context) {
    String name = call.getNameAsString();
    List<Expression> args = call.getArguments();
    if (EQUALITY_ASSERTIONS.contains(name) && args.size() >= 2) {
      Expression left = args.get(0);
      Expression right = args.get(1);
      if (args.size() >= 3 && left instanceof StringLiteralExpr) {
        left = args.get(1);
        right = args.get(2);
      }
      if (!same(left, right)
          && ConstantExpressions.isConstant(left, unit)
          && ConstantExpressions.isConstant(right, unit)) {
        return Optional.of(
            context.finding(
                this, call, name + " compares two constants, " + left + " and " + right, FIX));
      }
    }
    if (BOOLEAN_ASSERTIONS.contains(name)
        && !args.isEmpty()
        && !(ConstantExpressions.unwrap(args.get(0)) instanceof BooleanLiteralExpr)
        && ConstantExpressions.isConstant(args.get(0), unit)) {
      return Optional.of(
          context.finding(
              this, call, name + " checks the constant expression " + args.get(0), FIX));
    }
    if (name.equals("assertThat") && args.size() >= 2) {
      Expression actual = args.get(args.size() - 2);
      Expression matcher = args.get(args.size() - 1);
      if (matcher instanceof MethodCallExpr matcherCall
          && HAMCREST_MATCHERS.contains(matcherCall.getNameAsString())
          && matcherCall.getArguments().size() == 1
          && !same(actual, matcherOperand(matcherCall))
          && ConstantExpressions.isConstant(actual, unit)
          && ConstantExpressions.isConstant(matcherOperand(matcherCall), unit)) {
        return Optional.of(
            context.finding(
                this,
                call,
                "assertThat compares the constant " + actual + " with a constant matcher",
                FIX));
      }
    }
    return Optional.empty();
  }

  private Optional<Finding> checkFluent(
      MethodCallExpr call, CompilationUnit unit, RuleContext context) {
    if (!FLUENT_TERMINALS.contains(call.getNameAsString())) {
      return Optional.empty();
    }
    Optional<MethodCallExpr> root = Assertions.fluentRoot(call);
    if (root.isEmpty()
        || !root.get().getNameAsString().equals("assertThat")
        || root.get().getArguments().size() != 1) {
      return Optional.empty();
    }
    Expression actual = root.get().getArgument(0);
    if (!ConstantExpressions.isConstant(actual, unit)) {
      return Optional.empty();
    }
    for (Expression argument : call.getArguments()) {
      if (!ConstantExpressions.isConstant(argument, unit)) {
        return Optional.empty();
      }
    }
    if (call.getArguments().size() == 1 && same(actual, call.getArgument(0))) {
      return Optional.empty();
    }
    if (ConstantExpressions.unwrap(actual) instanceof BooleanLiteralExpr
        && call.getArguments().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        context.finding(
            this,
            call,
            "assertThat(" + actual + ")." + call.getNameAsString() + " asserts on a constant",
            FIX));
  }

  private static Expression matcherOperand(MethodCallExpr matcher) {
    Expression operand = matcher.getArgument(0);
    if (operand instanceof MethodCallExpr nested
        && HAMCREST_MATCHERS.contains(nested.getNameAsString())
        && nested.getArguments().size() == 1) {
      return matcherOperand(nested);
    }
    return operand;
  }

  private static boolean same(Expression a, Expression b) {
    return ConstantExpressions.unwrap(a).equals(ConstantExpressions.unwrap(b));
  }
}
