package io.github.byreshb.tql.rule;

import com.github.javaparser.ast.CompilationUnit;
import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.Severity;
import java.util.List;

/**
 * One check over a parsed test source file. Rules are stateless: the same instance is used for
 * every file, and everything file-specific arrives through the {@link RuleContext}.
 *
 * <p>Built-in rules are discovered through {@link java.util.ServiceLoader}; to add a rule from
 * another jar, implement this interface and list the class in {@code
 * META-INF/services/io.github.byreshb.tql.rule.Rule}.
 */
public interface Rule {

  /**
   * The stable id of the rule, for example {@code TQL001}. Used in configuration, suppressions and
   * reports.
   *
   * @return the id
   */
  String id();

  /**
   * A short CamelCase name, for example {@code TautologicalAssertion}.
   *
   * @return the name
   */
  String name();

  /**
   * One sentence saying what the rule catches.
   *
   * @return the description
   */
  String description();

  /**
   * The severity of the rule's findings unless overridden in the configuration.
   *
   * @return the default severity
   */
  Severity defaultSeverity();

  /**
   * Checks one file.
   *
   * @param unit the parsed file
   * @param context the file, configuration and finding factory
   * @return the findings, possibly empty, never null
   */
  List<Finding> check(CompilationUnit unit, RuleContext context);
}
