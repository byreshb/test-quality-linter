package io.github.byreshb.tql.engine;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.SourceFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Removes findings suppressed by a {@code // tql:ignore} line comment or a
 * {@code @SuppressWarnings("tql:...")} annotation.
 *
 * <p>{@code // tql:ignore} at the end of a line suppresses every finding on that line, or (with one
 * or more comma/space-separated rule ids after it, e.g. {@code // tql:ignore TQL001}) only those
 * rules. {@code @SuppressWarnings("tql:TQL001")} on a method, constructor, class, interface, enum
 * or field suppresses that rule for every finding inside the declaration's line range; the value
 * {@code "tql:*"} suppresses every rule. Both forms coexist with ordinary {@code @SuppressWarnings}
 * values (e.g. {@code "unchecked"}), which are ignored here.
 */
final class Suppressions {

  private static final Pattern LINE_IGNORE =
      Pattern.compile("//\\s*tql:ignore(?:[:\\s]+([A-Za-z0-9_,\\s]+))?\\s*$");
  private static final String TQL_PREFIX = "tql:";

  private Suppressions() {}

  /**
   * Filters out suppressed findings.
   *
   * @param unit the parsed file the findings came from
   * @param source the same file's source, for the line-comment scan
   * @param findings the candidate findings
   * @return the findings that are not suppressed
   */
  static List<Finding> filter(CompilationUnit unit, SourceFile source, List<Finding> findings) {
    if (findings.isEmpty()) {
      return findings;
    }
    Map<Integer, Optional<Set<String>>> lineSuppressions = lineSuppressions(source);
    List<AnnotationRange> annotationSuppressions = annotationSuppressions(unit);
    if (lineSuppressions.isEmpty() && annotationSuppressions.isEmpty()) {
      return findings;
    }
    List<Finding> kept = new ArrayList<>();
    for (Finding finding : findings) {
      if (!isSuppressed(finding, lineSuppressions, annotationSuppressions)) {
        kept.add(finding);
      }
    }
    return kept;
  }

  private static boolean isSuppressed(
      Finding finding,
      Map<Integer, Optional<Set<String>>> lineSuppressions,
      List<AnnotationRange> annotationSuppressions) {
    Optional<Set<String>> lineRule = lineSuppressions.get(finding.line());
    if (lineRule != null && (lineRule.isEmpty() || lineRule.get().contains(finding.ruleId()))) {
      return true;
    }
    for (AnnotationRange range : annotationSuppressions) {
      if (finding.line() >= range.startLine
          && finding.line() <= range.endLine
          && (range.ruleIds.contains(finding.ruleId()) || range.ruleIds.contains("*"))) {
        return true;
      }
    }
    return false;
  }

  private static Map<Integer, Optional<Set<String>>> lineSuppressions(SourceFile source) {
    Map<Integer, Optional<Set<String>>> result = new HashMap<>();
    List<String> lines = source.content().lines().toList();
    for (int i = 0; i < lines.size(); i++) {
      Matcher matcher = LINE_IGNORE.matcher(lines.get(i).stripTrailing());
      if (!matcher.find()) {
        continue;
      }
      String ids = matcher.group(1);
      if (ids == null || ids.isBlank()) {
        result.put(i + 1, Optional.empty());
      } else {
        Set<String> ruleIds = new HashSet<>();
        for (String id : ids.split("[,\\s]+")) {
          if (!id.isBlank()) {
            ruleIds.add(id.toUpperCase(Locale.ROOT));
          }
        }
        result.put(i + 1, Optional.of(ruleIds));
      }
    }
    return result;
  }

  private record AnnotationRange(int startLine, int endLine, Set<String> ruleIds) {}

  private static List<AnnotationRange> annotationSuppressions(CompilationUnit unit) {
    List<AnnotationRange> ranges = new ArrayList<>();
    for (BodyDeclaration<?> declaration : unit.findAll(BodyDeclaration.class)) {
      for (AnnotationExpr annotation : declaration.getAnnotations()) {
        if (!annotation.getNameAsString().equals("SuppressWarnings")) {
          continue;
        }
        Set<String> ruleIds = tqlIdsOf(annotation);
        if (ruleIds.isEmpty()
            || declaration.getBegin().isEmpty()
            || declaration.getEnd().isEmpty()) {
          continue;
        }
        ranges.add(
            new AnnotationRange(
                declaration.getBegin().get().line, declaration.getEnd().get().line, ruleIds));
      }
    }
    return ranges;
  }

  private static Set<String> tqlIdsOf(AnnotationExpr annotation) {
    List<Expression> values = new ArrayList<>();
    if (annotation instanceof SingleMemberAnnotationExpr single) {
      values.add(single.getMemberValue());
    } else if (annotation instanceof NormalAnnotationExpr normal) {
      normal.getPairs().stream()
          .filter(pair -> pair.getNameAsString().equals("value"))
          .forEach(pair -> values.add(pair.getValue()));
    }
    Set<String> ids = new HashSet<>();
    for (Expression value : values) {
      if (value instanceof ArrayInitializerExpr array) {
        array.getValues().forEach(v -> addTqlId(v, ids));
      } else {
        addTqlId(value, ids);
      }
    }
    return ids;
  }

  private static void addTqlId(Expression expression, Set<String> ids) {
    if (expression instanceof StringLiteralExpr literal
        && literal.getValue().startsWith(TQL_PREFIX)) {
      ids.add(literal.getValue().substring(TQL_PREFIX.length()).toUpperCase(Locale.ROOT));
    }
  }
}
