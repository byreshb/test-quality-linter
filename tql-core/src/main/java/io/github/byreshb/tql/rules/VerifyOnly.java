package io.github.byreshb.tql.rules;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.Severity;
import io.github.byreshb.tql.rule.AbstractRule;
import io.github.byreshb.tql.rule.Assertions;
import io.github.byreshb.tql.rule.RuleContext;
import io.github.byreshb.tql.rule.TestMethods;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * TQL005: a test that verifies mock interactions and nothing else. Verifying a call happened is
 * useful, but it does not check what that call was supposed to produce; a test with only
 * verifications passes even when the collaborator's contract is met by accident.
 *
 * <p>A test that also asserts on state or a return value, expects an exception, or has no assertion
 * at all (covered instead by {@code NoAssertion}, which counts a bare verification as "asserting
 * something") does not trigger this rule.
 */
public final class VerifyOnly extends AbstractRule {

  /** The rule id. */
  public static final String ID = "TQL005";

  private static final Set<String> VERIFICATIONS =
      Set.of(
          "verify",
          "verifyNoInteractions",
          "verifyNoMoreInteractions",
          "verifyZeroInteractions",
          "verifyAll");

  /** Creates the rule. */
  public VerifyOnly() {
    super(
        ID,
        "VerifyOnly",
        "A test contains only mock verifications and no assertion on an observable outcome.",
        Severity.WARN);
  }

  @Override
  public List<Finding> check(CompilationUnit unit, RuleContext context) {
    List<Finding> findings = new ArrayList<>();
    for (MethodDeclaration test : TestMethods.in(unit)) {
      if (TestMethods.expectsException(test) || test.getBody().isEmpty()) {
        continue;
      }
      List<MethodCallExpr> calls = test.getBody().get().findAll(MethodCallExpr.class);
      boolean hasVerification =
          calls.stream().anyMatch(call -> VERIFICATIONS.contains(call.getNameAsString()));
      boolean hasAssertion = calls.stream().anyMatch(Assertions::isAssertion);
      if (hasVerification && !hasAssertion) {
        findings.add(
            context.finding(
                this,
                test.getName(),
                "Test '"
                    + test.getNameAsString()
                    + "' only verifies mock interactions; nothing asserts an observable outcome",
                "Assert on the state or return value the interaction is supposed to produce, in"
                    + " addition to verifying it happened"));
      }
    }
    return findings;
  }
}
