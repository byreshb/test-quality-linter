package io.github.byreshb.tql.rules;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.Severity;
import io.github.byreshb.tql.rule.AbstractRule;
import io.github.byreshb.tql.rule.Assertions;
import io.github.byreshb.tql.rule.RuleContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * TQL001: an assertion that compares an expression with itself or otherwise cannot fail, such as
 * {@code assertEquals(x, x)}, {@code assertThat(x).isEqualTo(x)}, {@code assertTrue(true)}, {@code
 * assertFalse(false)} or {@code assertNotNull(new Foo())}.
 */
public final class TautologicalAssertion extends AbstractRule {

  /** The rule id. */
  public static final String ID = "TQL001";

  private static final String FIX =
      "Assert against an expected value that is computed independently of the code under test";

  private static final String FIX_NOT_NULL =
      "Assert on the object returned by the code under test, not on one created in the test";

  private static final Set<String> EQUALITY_ASSERTIONS =
      Set.of("assertEquals", "assertSame", "assertArrayEquals", "assertIterableEquals");

  private static final Set<String> FLUENT_EQUALITY =
      Set.of(
          "isEqualTo",
          "isSameAs",
          "isEqualToComparingFieldByField",
          "containsExactlyElementsOf",
          "containsExactlyInAnyOrderElementsOf",
          "hasSameElementsAs",
          "isEqualToIgnoringCase");

  private static final Set<String> HAMCREST_EQUALITY =
      Set.of("is", "equalTo", "sameInstance", "theInstance");

  /** Creates the rule. */
  public TautologicalAssertion() {
    super(
        ID,
        "TautologicalAssertion",
        "An assertion compares an expression with itself or can never fail.",
        Severity.ERROR);
  }

  @Override
  public List<Finding> check(CompilationUnit unit, RuleContext context) {
    List<Finding> findings = new ArrayList<>();
    for (MethodCallExpr call : unit.findAll(MethodCallExpr.class)) {
      checkStatic(call, context).ifPresent(findings::add);
      checkFluent(call, context).ifPresent(findings::add);
    }
    return findings;
  }

  private Optional<Finding> checkStatic(MethodCallExpr call, RuleContext context) {
    String name = call.getNameAsString();
    List<Expression> args = call.getArguments();
    if (EQUALITY_ASSERTIONS.contains(name) && args.size() >= 2) {
      Expression[] pair = comparedPair(args);
      if (same(pair[0], pair[1])) {
        return Optional.of(
            context.finding(
                this,
                call,
                name + " compares " + describe(pair[0]) + " with itself and cannot fail",
                FIX));
      }
    }
    if (name.equals("assertTrue") && !args.isEmpty() && isAlwaysTrue(args.get(0))) {
      return Optional.of(
          context.finding(this, call, "assertTrue(" + args.get(0) + ") cannot fail", FIX));
    }
    if (name.equals("assertFalse") && !args.isEmpty() && isAlwaysFalse(args.get(0))) {
      return Optional.of(
          context.finding(this, call, "assertFalse(" + args.get(0) + ") cannot fail", FIX));
    }
    if (name.equals("assertNotNull") && !args.isEmpty() && isNeverNull(args.get(0))) {
      return Optional.of(
          context.finding(
              this,
              call,
              "assertNotNull on " + describe(args.get(0)) + " cannot fail; it is never null",
              FIX_NOT_NULL));
    }
    if (name.equals("assertThat") && args.size() >= 2) {
      // Hamcrest: assertThat(actual, matcher) or assertThat(reason, actual, matcher)
      Expression actual = args.get(args.size() - 2);
      Expression matcher = args.get(args.size() - 1);
      if (matcher instanceof MethodCallExpr matcherCall) {
        Optional<Expression> matched = hamcrestOperand(matcherCall);
        if (matched.isPresent() && same(actual, matched.get())) {
          return Optional.of(
              context.finding(
                  this,
                  call,
                  "assertThat compares " + describe(actual) + " with itself and cannot fail",
                  FIX));
        }
        if (matcherCall.getNameAsString().equals("notNullValue") && isNeverNull(actual)) {
          return Optional.of(
              context.finding(
                  this,
                  call,
                  "assertThat(" + describe(actual) + ", notNullValue()) cannot fail",
                  FIX_NOT_NULL));
        }
      }
    }
    return Optional.empty();
  }

  private Optional<Finding> checkFluent(MethodCallExpr call, RuleContext context) {
    String name = call.getNameAsString();
    if (!FLUENT_EQUALITY.contains(name)
        && !name.equals("isNotNull")
        && !name.equals("isTrue")
        && !name.equals("isFalse")) {
      return Optional.empty();
    }
    Optional<MethodCallExpr> root = Assertions.fluentRoot(call);
    if (root.isEmpty()
        || !root.get().getNameAsString().equals("assertThat")
        || root.get().getArguments().size() != 1) {
      return Optional.empty();
    }
    Expression actual = root.get().getArgument(0);
    if (FLUENT_EQUALITY.contains(name)
        && call.getArguments().size() == 1
        && same(actual, call.getArgument(0))) {
      return Optional.of(
          context.finding(
              this,
              call,
              "assertThat(" + describe(actual) + ")." + name + " compares it with itself",
              FIX));
    }
    if (name.equals("isNotNull") && isNeverNull(actual)) {
      return Optional.of(
          context.finding(
              this,
              call,
              "assertThat(" + describe(actual) + ").isNotNull() cannot fail; it is never null",
              FIX_NOT_NULL));
    }
    if ((name.equals("isTrue") && isAlwaysTrue(actual))
        || (name.equals("isFalse") && isAlwaysFalse(actual))) {
      return Optional.of(
          context.finding(
              this, call, "assertThat(" + actual + ")." + name + "() cannot fail", FIX));
    }
    return Optional.empty();
  }

  /**
   * Picks the two compared arguments of a JUnit-style equality assertion, skipping a leading (JUnit
   * 4) message; a trailing (JUnit 5) message or delta is simply not part of the pair.
   */
  private static Expression[] comparedPair(List<Expression> args) {
    if (args.size() >= 3 && args.get(0) instanceof StringLiteralExpr) {
      return new Expression[] {args.get(1), args.get(2)};
    }
    return new Expression[] {args.get(0), args.get(1)};
  }

  private static Optional<Expression> hamcrestOperand(MethodCallExpr matcher) {
    if (!HAMCREST_EQUALITY.contains(matcher.getNameAsString())
        || matcher.getArguments().size() != 1) {
      return Optional.empty();
    }
    Expression operand = matcher.getArgument(0);
    if (operand instanceof MethodCallExpr nested
        && HAMCREST_EQUALITY.contains(nested.getNameAsString())) {
      return hamcrestOperand(nested);
    }
    return Optional.of(operand);
  }

  private static boolean same(Expression a, Expression b) {
    return unwrap(a).equals(unwrap(b));
  }

  private static Expression unwrap(Expression expression) {
    Expression current = expression;
    while (current instanceof EnclosedExpr enclosed) {
      current = enclosed.getInner();
    }
    return current;
  }

  private static boolean isAlwaysTrue(Expression expression) {
    Expression e = unwrap(expression);
    if (e instanceof BooleanLiteralExpr literal) {
      return literal.getValue();
    }
    if (e instanceof BinaryExpr binary && same(binary.getLeft(), binary.getRight())) {
      return binary.getOperator() == BinaryExpr.Operator.EQUALS
          || binary.getOperator() == BinaryExpr.Operator.LESS_EQUALS
          || binary.getOperator() == BinaryExpr.Operator.GREATER_EQUALS;
    }
    if (e instanceof MethodCallExpr call
        && call.getNameAsString().equals("equals")
        && call.getArguments().size() == 1
        && call.getScope().isPresent()) {
      return same(call.getScope().get(), call.getArgument(0));
    }
    return false;
  }

  private static boolean isAlwaysFalse(Expression expression) {
    Expression e = unwrap(expression);
    if (e instanceof BooleanLiteralExpr literal) {
      return !literal.getValue();
    }
    if (e instanceof BinaryExpr binary && same(binary.getLeft(), binary.getRight())) {
      return binary.getOperator() == BinaryExpr.Operator.NOT_EQUALS
          || binary.getOperator() == BinaryExpr.Operator.LESS
          || binary.getOperator() == BinaryExpr.Operator.GREATER;
    }
    return false;
  }

  private static boolean isNeverNull(Expression expression) {
    Expression e = unwrap(expression);
    return e instanceof ObjectCreationExpr
        || e instanceof ArrayCreationExpr
        || e instanceof LambdaExpr
        || (e instanceof LiteralExpr && !(e instanceof NullLiteralExpr));
  }

  private static String describe(Expression expression) {
    String text = unwrap(expression).toString();
    return text.length() > 40 ? text.substring(0, 37) + "..." : text;
  }
}
