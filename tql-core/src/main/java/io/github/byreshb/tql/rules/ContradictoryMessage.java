package io.github.byreshb.tql.rules;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.Severity;
import io.github.byreshb.tql.rule.AbstractRule;
import io.github.byreshb.tql.rule.RuleContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * TQL010: the assertion message contradicts what the assertion checks: {@code assertTrue} with a
 * message that says the condition should not hold, or {@code assertFalse} with a message that says
 * it should. This is a simple text heuristic over the message literal, not a semantic check, so it
 * is INFO rather than a stronger severity: it usually means the message was not updated when the
 * assertion was flipped, but it can also be a coincidence of wording.
 */
public final class ContradictoryMessage extends AbstractRule {

  /** The rule id. */
  public static final String ID = "TQL010";

  private static final Pattern NEGATIVE =
      Pattern.compile("(?i)\\b(should not|shouldn't|must not|mustn't|never)\\b");
  private static final Pattern POSITIVE = Pattern.compile("(?i)\\bshould\\b");
  private static final Pattern ANY_NOT = Pattern.compile("(?i)\\bnot\\b");

  /** Creates the rule. */
  public ContradictoryMessage() {
    super(
        ID,
        "ContradictoryMessage",
        "An assertion's message says the opposite of what the assertion checks.",
        Severity.INFO);
  }

  @Override
  public List<Finding> check(CompilationUnit unit, RuleContext context) {
    List<Finding> findings = new ArrayList<>();
    for (MethodCallExpr call : unit.findAll(MethodCallExpr.class)) {
      String name = call.getNameAsString();
      if (!name.equals("assertTrue") && !name.equals("assertFalse")) {
        continue;
      }
      message(call)
          .ifPresent(
              message -> {
                boolean positive = name.equals("assertTrue");
                if (positive && NEGATIVE.matcher(message).find()) {
                  findings.add(contradiction(call, name, message, context));
                } else if (!positive
                    && POSITIVE.matcher(message).find()
                    && !ANY_NOT.matcher(message).find()) {
                  findings.add(contradiction(call, name, message, context));
                }
              });
    }
    return findings;
  }

  private Finding contradiction(
      MethodCallExpr call, String assertionName, String message, RuleContext context) {
    return context.finding(
        this,
        call,
        assertionName + "'s message \"" + message + "\" contradicts what it checks",
        "Reword the message to match the condition, or fix the condition if the message is"
            + " right");
  }

  /** Finds a string-literal message argument, JUnit 4 (message first) or 5 (message last). */
  private static Optional<String> message(MethodCallExpr call) {
    List<Expression> args = call.getArguments();
    if (args.size() != 2) {
      return Optional.empty();
    }
    if (args.get(0) instanceof StringLiteralExpr message
        && !(args.get(1) instanceof StringLiteralExpr)) {
      return Optional.of(message.getValue());
    }
    if (args.get(1) instanceof StringLiteralExpr message
        && !(args.get(0) instanceof StringLiteralExpr)) {
      return Optional.of(message.getValue());
    }
    return Optional.empty();
  }
}
