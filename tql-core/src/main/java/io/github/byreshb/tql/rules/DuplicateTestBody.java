package io.github.byreshb.tql.rules;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.stmt.BlockStmt;
import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.Severity;
import io.github.byreshb.tql.rule.AbstractRule;
import io.github.byreshb.tql.rule.RuleContext;
import io.github.byreshb.tql.rule.TestMethods;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TQL008: two test methods in the same class have identical bodies once formatting and comments are
 * normalised away. A common way this happens is copying a test to cover a new case and forgetting
 * to change anything, so the "new" test proves nothing the original did not already prove.
 *
 * <p>Bodies of a single statement or fewer are not compared, since two trivially empty or one-line
 * tests are common and rarely a meaningful duplicate.
 */
public final class DuplicateTestBody extends AbstractRule {

  /** The rule id. */
  public static final String ID = "TQL008";

  private static final int MIN_STATEMENTS = 2;

  /** Creates the rule. */
  public DuplicateTestBody() {
    super(
        ID,
        "DuplicateTestBody",
        "Two test methods in the same class have identical bodies.",
        Severity.WARN);
  }

  @Override
  public List<Finding> check(CompilationUnit unit, RuleContext context) {
    List<Finding> findings = new ArrayList<>();
    for (ClassOrInterfaceDeclaration type : unit.findAll(ClassOrInterfaceDeclaration.class)) {
      Map<String, List<MethodDeclaration>> byBody = new LinkedHashMap<>();
      for (MethodDeclaration method : type.getMethods()) {
        if (!TestMethods.isTest(method) || method.getBody().isEmpty()) {
          continue;
        }
        BlockStmt body = method.getBody().get();
        if (body.getStatements().size() < MIN_STATEMENTS) {
          continue;
        }
        byBody.computeIfAbsent(normalise(body), key -> new ArrayList<>()).add(method);
      }
      for (List<MethodDeclaration> group : byBody.values()) {
        if (group.size() < 2) {
          continue;
        }
        String original = group.get(0).getNameAsString();
        for (int i = 1; i < group.size(); i++) {
          MethodDeclaration duplicate = group.get(i);
          findings.add(
              context.finding(
                  this,
                  duplicate.getName(),
                  "Test '"
                      + duplicate.getNameAsString()
                      + "' has the same body as '"
                      + original
                      + "'",
                  "Give the test a different case to cover, or delete it if it is a leftover"
                      + " copy"));
        }
      }
    }
    return findings;
  }

  /** Renders a body with comments stripped, so only the executable structure is compared. */
  private static String normalise(BlockStmt body) {
    BlockStmt clone = body.clone();
    new ArrayList<>(clone.getAllContainedComments()).forEach(Comment::remove);
    return clone.toString();
  }
}
