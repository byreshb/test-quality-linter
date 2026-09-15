package io.github.byreshb.tql.rules;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.Severity;
import io.github.byreshb.tql.rule.AbstractRule;
import io.github.byreshb.tql.rule.GitBlame;
import io.github.byreshb.tql.rule.RuleContext;
import io.github.byreshb.tql.rule.ShellGitBlame;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * TQL009: a test or test class is disabled with no reason given, or (when the {@code maxAgeDays}
 * option is set and the file is on disk inside a git repository) has stayed disabled for longer
 * than that, reason or not.
 *
 * <p>Recognises JUnit 5's {@code @Disabled} and JUnit 4's {@code @Ignore}, on a method or a class,
 * in marker form, with a single string argument, or with a named {@code value}/{@code reason}
 * argument.
 */
public final class DisabledWithoutReason extends AbstractRule {

  /** The rule id. */
  public static final String ID = "TQL009";

  private static final Set<String> DISABLING_ANNOTATIONS = Set.of("Disabled", "Ignore");
  private static final Set<String> REASON_ATTRIBUTE_NAMES = Set.of("value", "reason");

  private final GitBlame gitBlame;

  /** Creates the rule, using the real {@code git} command line for age checks. */
  public DisabledWithoutReason() {
    this(new ShellGitBlame());
  }

  /**
   * Creates the rule with an explicit {@link GitBlame}, for tests.
   *
   * @param gitBlame how to look up when a line was last changed
   */
  DisabledWithoutReason(GitBlame gitBlame) {
    super(
        ID,
        "DisabledWithoutReason",
        "@Disabled or @Ignore has no reason, or has stood disabled past the configured age.",
        Severity.WARN);
    this.gitBlame = gitBlame;
  }

  @Override
  public List<Finding> check(CompilationUnit unit, RuleContext context) {
    int maxAgeDays = context.config().intOption(ID, "maxAgeDays", 0);
    List<Finding> findings = new ArrayList<>();
    for (BodyDeclaration<?> declaration : unit.findAll(BodyDeclaration.class)) {
      for (AnnotationExpr annotation : declaration.getAnnotations()) {
        if (!DISABLING_ANNOTATIONS.contains(annotation.getName().getIdentifier())) {
          continue;
        }
        checkAnnotation(annotation, context, maxAgeDays).forEach(findings::add);
      }
    }
    return findings;
  }

  private List<Finding> checkAnnotation(
      AnnotationExpr annotation, RuleContext context, int maxAgeDays) {
    List<Finding> findings = new ArrayList<>();
    Optional<String> reason = reasonOf(annotation);
    if (reason.isEmpty()) {
      findings.add(
          context.finding(
              this,
              annotation,
              "@" + annotation.getNameAsString() + " has no reason",
              "Say why the test is disabled, e.g. @"
                  + annotation.getNameAsString()
                  + "(\"blocked by ISSUE-123\")"));
    }
    if (maxAgeDays > 0) {
      int line = annotation.getBegin().map(p -> p.line).orElse(0);
      gitBlame
          .lastChanged(context.source().path(), line)
          .ifPresent(
              changed -> {
                long ageDays = Duration.between(changed, Instant.now()).toDays();
                if (ageDays > maxAgeDays) {
                  findings.add(
                      context.finding(
                          this,
                          annotation,
                          "@"
                              + annotation.getNameAsString()
                              + " has stood for "
                              + ageDays
                              + " days, past the configured "
                              + maxAgeDays
                              + "-day limit",
                          "Fix the test and re-enable it, or delete it if it no longer applies"));
                }
              });
    }
    return findings;
  }

  private static Optional<String> reasonOf(AnnotationExpr annotation) {
    if (annotation instanceof SingleMemberAnnotationExpr single) {
      return textOf(single.getMemberValue());
    }
    if (annotation instanceof NormalAnnotationExpr normal) {
      NodeList<com.github.javaparser.ast.expr.MemberValuePair> pairs = normal.getPairs();
      for (var pair : pairs) {
        if (REASON_ATTRIBUTE_NAMES.contains(pair.getNameAsString())) {
          return textOf(pair.getValue());
        }
      }
    }
    return Optional.empty();
  }

  private static Optional<String> textOf(Expression expression) {
    if (expression instanceof StringLiteralExpr string) {
      return string.getValue().isBlank() ? Optional.empty() : Optional.of(string.getValue());
    }
    return Optional.of(expression.toString());
  }
}
