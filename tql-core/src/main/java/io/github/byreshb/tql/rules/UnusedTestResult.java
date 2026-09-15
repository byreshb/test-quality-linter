package io.github.byreshb.tql.rules;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.resolution.types.ResolvedType;
import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.Severity;
import io.github.byreshb.tql.rule.AbstractRule;
import io.github.byreshb.tql.rule.Assertions;
import io.github.byreshb.tql.rule.RuleContext;
import io.github.byreshb.tql.rule.TestMethods;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * TQL011: the code under test is called, its return value is discarded as a bare statement, and
 * that value is never checked anywhere else in the method.
 *
 * <p>This rule needs symbol resolution (a {@code Linter} built with a classpath) to know whether a
 * call's return type is void; without it, {@link RuleContext#symbolsResolved()} is false and the
 * rule reports nothing rather than guess.
 *
 * <p>To keep the false-positive rate low, only calls that resolve to a small set of "value-like"
 * return types are considered (primitives, their wrapper classes, {@code String} and {@code
 * Optional}): a fluent setter returning its own type, or a mutator like {@code List.add} whose
 * boolean result is conventionally ignored, is not flagged unless it happens to be one of those
 * types too, in which case {@code // tql:ignore TQL011} suppresses the individual line.
 */
public final class UnusedTestResult extends AbstractRule {

  /** The rule id. */
  public static final String ID = "TQL011";

  private static final Set<String> VALUE_LIKE_TYPES =
      Set.of(
          "java.lang.String",
          "java.lang.Boolean",
          "java.lang.Integer",
          "java.lang.Long",
          "java.lang.Double",
          "java.lang.Float",
          "java.lang.Short",
          "java.lang.Byte",
          "java.lang.Character",
          "java.util.Optional",
          "java.math.BigDecimal",
          "java.math.BigInteger");

  private static final Set<String> MOCK_DSL =
      Set.of(
          "when",
          "given",
          "expect",
          "stub",
          "doReturn",
          "doThrow",
          "doAnswer",
          "doNothing",
          "thenReturn",
          "thenThrow",
          "willReturn",
          "willThrow",
          "andReturn",
          "andThrow",
          "replay",
          "reset",
          "mock",
          "spy",
          "createMock",
          "createNiceMock",
          "createStrictMock",
          "times",
          "any",
          "anyInt",
          "anyLong",
          "anyString",
          "anyBoolean",
          "eq",
          "argThat");

  /** Creates the rule. */
  public UnusedTestResult() {
    super(
        ID,
        "UnusedTestResult",
        "A call's return value is discarded as a bare statement and never checked.",
        Severity.WARN);
  }

  @Override
  public List<Finding> check(CompilationUnit unit, RuleContext context) {
    if (!context.symbolsResolved()) {
      return List.of();
    }
    List<Finding> findings = new ArrayList<>();
    for (MethodDeclaration test : TestMethods.in(unit)) {
      if (test.getBody().isEmpty()) {
        continue;
      }
      BlockStmt body = test.getBody().get();
      Set<String> referencedInAssertions = referencedCalls(body);
      for (ExpressionStmt stmt : body.findAll(ExpressionStmt.class)) {
        if (!(stmt.getExpression() instanceof MethodCallExpr call) || !isCandidate(call)) {
          continue;
        }
        if (referencedInAssertions.contains(call.toString())) {
          continue;
        }
        resolveValueLikeType(call)
            .ifPresent(
                type ->
                    findings.add(
                        context.finding(
                            this,
                            call,
                            "Result of "
                                + call
                                + " ("
                                + type
                                + ") is discarded and never"
                                + " asserted",
                            "Assert on the returned value, or assign it to a variable and use it"
                                + " in an assertion")));
      }
    }
    return findings;
  }

  private static boolean isCandidate(MethodCallExpr call) {
    String name = call.getNameAsString();
    return !Assertions.isAssertion(call) && !MOCK_DSL.contains(name) && !name.startsWith("verify");
  }

  private static Optional<String> resolveValueLikeType(MethodCallExpr call) {
    ResolvedType type;
    try {
      type = call.calculateResolvedType();
    } catch (RuntimeException unresolvable) {
      return Optional.empty();
    }
    if (type.isVoid()) {
      return Optional.empty();
    }
    if (type.isPrimitive()) {
      return Optional.of(type.describe());
    }
    if (type.isReferenceType()
        && VALUE_LIKE_TYPES.contains(type.asReferenceType().getQualifiedName())) {
      return Optional.of(type.asReferenceType().getQualifiedName());
    }
    return Optional.empty();
  }

  /**
   * The {@code toString()} of every call that appears, at any depth, inside the arguments of an
   * assertion call in the method: the only structural signal available that a bare call's result
   * was in fact checked, since it was never assigned to a variable.
   */
  private static Set<String> referencedCalls(BlockStmt body) {
    Set<String> referenced = new HashSet<>();
    for (MethodCallExpr call : body.findAll(MethodCallExpr.class)) {
      if (!Assertions.isAssertion(call)) {
        continue;
      }
      for (com.github.javaparser.ast.expr.Expression argument : call.getArguments()) {
        argument.findAll(MethodCallExpr.class).forEach(c -> referenced.add(c.toString()));
      }
    }
    return referenced;
  }
}
