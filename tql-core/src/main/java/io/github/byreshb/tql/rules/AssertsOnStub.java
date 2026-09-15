package io.github.byreshb.tql.rules;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.Severity;
import io.github.byreshb.tql.rule.AbstractRule;
import io.github.byreshb.tql.rule.Assertions;
import io.github.byreshb.tql.rule.ConstantExpressions;
import io.github.byreshb.tql.rule.RuleContext;
import io.github.byreshb.tql.rule.TestMethods;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TQL004: an assertion compares the value a mock returns back to the literal the mock was stubbed
 * with, for example {@code when(repo.count()).thenReturn(5);} followed later by {@code
 * assertEquals(5, repo.count());}. The assertion cannot fail: it re-checks the mock's
 * configuration, not anything the code under test computed.
 *
 * <p>Recognises Mockito's {@code when(...).thenReturn(...)} and {@code
 * doReturn(...).when(mock).method(...)}, Mockito-BDD's {@code given(...).willReturn(...)} and
 * EasyMock's {@code expect(...).andReturn(...)}. Stubs are collected from the whole class, since
 * they are often set up in {@code @BeforeEach}; only test methods are checked for the circular
 * assertion.
 */
public final class AssertsOnStub extends AbstractRule {

  /** The rule id. */
  public static final String ID = "TQL004";

  private static final String FIX =
      "Assert on the outcome the code under test produces, not on the value a mock was told to"
          + " return";

  private static final Set<String> STUB_ROOTS = Set.of("when", "given", "expect");
  private static final Set<String> STUB_TERMINALS = Set.of("thenReturn", "willReturn", "andReturn");
  private static final Set<String> EQUALITY_ASSERTIONS = Set.of("assertEquals", "assertSame");
  private static final Set<String> FLUENT_EQUALITY = Set.of("isEqualTo", "isSameAs");
  private static final Set<String> HAMCREST_EQUALITY = Set.of("is", "equalTo");

  /** Creates the rule. */
  public AssertsOnStub() {
    super(
        ID,
        "AssertsOnStub",
        "An assertion compares a mock's return value to the literal it was stubbed with.",
        Severity.ERROR);
  }

  @Override
  public List<Finding> check(CompilationUnit unit, RuleContext context) {
    List<MethodDeclaration> tests = TestMethods.in(unit);
    Set<MethodDeclaration> testSet = new HashSet<>(tests);
    List<MethodCallExpr> sharedCalls =
        unit.findAll(MethodCallExpr.class).stream()
            .filter(
                call ->
                    call.findAncestor(MethodDeclaration.class)
                        .map(m -> !testSet.contains(m))
                        .orElse(true))
            .toList();
    Map<String, Expression> sharedStubs = collectStubs(sharedCalls);
    List<Finding> findings = new ArrayList<>();
    for (MethodDeclaration test : tests) {
      if (test.getBody().isEmpty()) {
        continue;
      }
      Map<String, Expression> stubs = new HashMap<>(sharedStubs);
      List<MethodCallExpr> bodyCalls = test.getBody().get().findAll(MethodCallExpr.class);
      stubs.putAll(collectStubs(bodyCalls));
      if (stubs.isEmpty()) {
        continue;
      }
      for (MethodCallExpr call : bodyCalls) {
        checkStatic(call, stubs, context).ifPresent(findings::add);
        checkFluent(call, stubs, context).ifPresent(findings::add);
      }
    }
    return findings;
  }

  /**
   * Collects stubs set up by the given calls: {@code when(...).thenReturn(...)}, {@code
   * given(...).willReturn(...)}, {@code expect(...).andReturn(...)} and {@code
   * doReturn(...).when(mock).method(...)}.
   *
   * @param calls the calls to scan, in any order
   * @return the stub target's rendered form mapped to the stubbed return value
   */
  private static Map<String, Expression> collectStubs(List<MethodCallExpr> calls) {
    Map<String, Expression> stubs = new HashMap<>();
    for (MethodCallExpr call : calls) {
      String name = call.getNameAsString();
      if (STUB_TERMINALS.contains(name)
          && call.getArguments().size() == 1
          && call.getScope().isPresent()
          && call.getScope().get() instanceof MethodCallExpr root
          && STUB_ROOTS.contains(root.getNameAsString())
          && root.getArguments().size() == 1
          && root.getArgument(0) instanceof MethodCallExpr target) {
        stubs.put(target.toString(), call.getArgument(0));
      }
      if (name.equals("when")
          && call.getArguments().size() == 1
          && call.getScope().isPresent()
          && call.getScope().get() instanceof MethodCallExpr doReturnCall
          && doReturnCall.getNameAsString().equals("doReturn")
          && doReturnCall.getArguments().size() == 1) {
        recordDoReturnUsages(call, doReturnCall.getArgument(0), stubs);
      }
    }
    return stubs;
  }

  /** Handles {@code doReturn(value).when(mock).method(args)}: the key is built from the chain. */
  private static void recordDoReturnUsages(
      MethodCallExpr whenCall, Expression value, Map<String, Expression> stubs) {
    whenCall
        .getParentNode()
        .filter(parent -> parent instanceof MethodCallExpr)
        .map(parent -> (MethodCallExpr) parent)
        .filter(terminal -> terminal.getScope().filter(s -> s == whenCall).isPresent())
        .ifPresent(
            terminal -> {
              String args =
                  terminal.getArguments().stream()
                      .map(Expression::toString)
                      .collect(Collectors.joining(", "));
              String key =
                  whenCall.getArgument(0) + "." + terminal.getNameAsString() + "(" + args + ")";
              stubs.put(key, value);
            });
  }

  private Optional<Finding> checkStatic(
      MethodCallExpr call, Map<String, Expression> stubs, RuleContext context) {
    String name = call.getNameAsString();
    List<Expression> args = call.getArguments();
    if (EQUALITY_ASSERTIONS.contains(name) && args.size() >= 2) {
      Expression left = args.get(0);
      Expression right = args.get(1);
      if (args.size() >= 3 && left instanceof StringLiteralExpr) {
        left = args.get(1);
        right = args.get(2);
      }
      Optional<Finding> found = matchStub(call, left, right, stubs, context);
      return found.isPresent() ? found : matchStub(call, right, left, stubs, context);
    }
    if (name.equals("assertThat") && args.size() >= 2) {
      Expression actual = args.get(args.size() - 2);
      Expression matcher = args.get(args.size() - 1);
      if (matcher instanceof MethodCallExpr matcherCall
          && HAMCREST_EQUALITY.contains(matcherCall.getNameAsString())
          && matcherCall.getArguments().size() == 1) {
        return matchStub(call, actual, matcherOperand(matcherCall), stubs, context);
      }
    }
    return Optional.empty();
  }

  private Optional<Finding> checkFluent(
      MethodCallExpr call, Map<String, Expression> stubs, RuleContext context) {
    if (!FLUENT_EQUALITY.contains(call.getNameAsString()) || call.getArguments().size() != 1) {
      return Optional.empty();
    }
    Optional<MethodCallExpr> root = Assertions.fluentRoot(call);
    if (root.isEmpty()
        || !root.get().getNameAsString().equals("assertThat")
        || root.get().getArguments().size() != 1) {
      return Optional.empty();
    }
    return matchStub(call, root.get().getArgument(0), call.getArgument(0), stubs, context);
  }

  private Optional<Finding> matchStub(
      MethodCallExpr call,
      Expression callSide,
      Expression valueSide,
      Map<String, Expression> stubs,
      RuleContext context) {
    Expression stubbedValue = stubs.get(callSide.toString());
    if (stubbedValue == null || !same(valueSide, stubbedValue)) {
      return Optional.empty();
    }
    return Optional.of(
        context.finding(
            this,
            call,
            callSide
                + " is asserted against "
                + valueSide
                + ", the exact value it was stubbed"
                + " to return",
            FIX));
  }

  private static Expression matcherOperand(MethodCallExpr matcher) {
    Expression operand = matcher.getArgument(0);
    if (operand instanceof MethodCallExpr nested
        && HAMCREST_EQUALITY.contains(nested.getNameAsString())
        && nested.getArguments().size() == 1) {
      return matcherOperand(nested);
    }
    return operand;
  }

  private static boolean same(Expression a, Expression b) {
    return ConstantExpressions.unwrap(a).equals(ConstantExpressions.unwrap(b));
  }
}
