package io.github.byreshb.tql.rules;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.Severity;
import io.github.byreshb.tql.rule.AbstractRule;
import io.github.byreshb.tql.rule.Assertions;
import io.github.byreshb.tql.rule.RuleContext;
import io.github.byreshb.tql.rule.TestMethods;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TQL003: a test method that asserts nothing: no assertion call, no {@code assertThrows}, no mock
 * verification, no expected exception, not even a call to a helper that asserts.
 *
 * <p>Options: {@code methods}, extra method names that count as assertions; {@code packages},
 * packages whose statically imported members count as assertions.
 */
public final class NoAssertion extends AbstractRule {

  /** The rule id. */
  public static final String ID = "TQL003";

  /** Packages whose statically imported methods count as assertions by default. */
  public static final List<String> DEFAULT_PACKAGES =
      List.of(
          "org.junit.jupiter.api.Assertions",
          "org.junit.Assert",
          "org.testng.Assert",
          "org.assertj.core.api",
          "org.hamcrest",
          "com.google.common.truth",
          "com.microsoft.playwright.assertions",
          "org.mockito",
          "org.easymock");

  private static final Set<String> VERIFICATIONS =
      Set.of(
          "verify",
          "verifyNoInteractions",
          "verifyNoMoreInteractions",
          "verifyZeroInteractions",
          "inOrder",
          "verifyAll",
          "expectThrows",
          "expect");

  private static final List<String> ASSERTING_PREFIXES =
      List.of("assert", "verify", "expect", "should", "check");

  /** Creates the rule. */
  public NoAssertion() {
    super(
        ID,
        "NoAssertion",
        "A test method has no assertion, verification or expected exception.",
        Severity.WARN);
  }

  @Override
  public List<Finding> check(CompilationUnit unit, RuleContext context) {
    Set<String> extraMethods = new HashSet<>(context.config().listOption(ID, "methods", List.of()));
    extraMethods.addAll(importedAssertions(unit, context));
    Map<String, MethodDeclaration> helpers = new HashMap<>();
    for (MethodDeclaration method : unit.findAll(MethodDeclaration.class)) {
      helpers.put(method.getNameAsString(), method);
    }
    List<Finding> findings = new ArrayList<>();
    for (MethodDeclaration test : TestMethods.in(unit)) {
      if (TestMethods.expectsException(test)) {
        continue;
      }
      if (!asserts(test, extraMethods, helpers, new HashSet<>())) {
        findings.add(
            context.finding(
                this,
                test.getName(),
                "Test '" + test.getNameAsString() + "' has no assertion",
                "Assert on the observable outcome, or state the intent with assertThrows or"
                    + " assertDoesNotThrow"));
      }
    }
    return findings;
  }

  private static boolean asserts(
      MethodDeclaration method,
      Set<String> extraMethods,
      Map<String, MethodDeclaration> helpers,
      Set<String> visiting) {
    if (!visiting.add(method.getNameAsString()) || method.getBody().isEmpty()) {
      return false;
    }
    for (MethodCallExpr call : method.getBody().get().findAll(MethodCallExpr.class)) {
      String name = call.getNameAsString();
      if (Assertions.isAssertion(call)
          || VERIFICATIONS.contains(name)
          || extraMethods.contains(name)
          || hasAssertingPrefix(name)) {
        return true;
      }
      MethodDeclaration helper = helpers.get(name);
      if (helper != null
          && call.getScope().isEmpty()
          && asserts(helper, extraMethods, helpers, visiting)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasAssertingPrefix(String name) {
    for (String prefix : ASSERTING_PREFIXES) {
      if (name.startsWith(prefix)
          && name.length() > prefix.length()
          && Character.isUpperCase(name.charAt(prefix.length()))) {
        return true;
      }
    }
    return false;
  }

  private static Set<String> importedAssertions(CompilationUnit unit, RuleContext context) {
    List<String> packages = context.config().listOption(ID, "packages", DEFAULT_PACKAGES);
    Set<String> names = new HashSet<>();
    for (ImportDeclaration imp : unit.getImports()) {
      if (!imp.isStatic() || imp.isAsterisk()) {
        continue;
      }
      String qualified = imp.getNameAsString();
      for (String pkg : packages) {
        if (qualified.startsWith(pkg + ".")) {
          names.add(imp.getName().getIdentifier());
        }
      }
    }
    return names;
  }
}
