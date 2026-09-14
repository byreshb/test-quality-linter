package io.github.byreshb.tql.rule;

import com.github.javaparser.Position;
import com.github.javaparser.ast.Node;
import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.SourceFile;
import java.util.Objects;
import java.util.Optional;

/**
 * Everything a rule needs besides the parsed file: the source text (to quote lines), the
 * configuration (for options and severity overrides), whether symbol resolution is available, and a
 * factory for findings that fills in file, position, snippet and severity.
 */
public final class RuleContext {

  private final SourceFile source;
  private final RuleConfig config;
  private final boolean symbolsResolved;

  /**
   * Creates a context for one file.
   *
   * @param source the file being linted
   * @param config the configuration
   * @param symbolsResolved whether the compilation unit was parsed with a symbol solver, so that
   *     {@code resolve()} calls on its nodes can be expected to work for classpath types
   */
  public RuleContext(SourceFile source, RuleConfig config, boolean symbolsResolved) {
    this.source = Objects.requireNonNull(source, "source");
    this.config = Objects.requireNonNull(config, "config");
    this.symbolsResolved = symbolsResolved;
  }

  /**
   * The file being linted.
   *
   * @return the source file
   */
  public SourceFile source() {
    return source;
  }

  /**
   * The configuration.
   *
   * @return the rule configuration
   */
  public RuleConfig config() {
    return config;
  }

  /**
   * Whether a symbol solver was attached when parsing. Rules that need types should degrade to
   * syntax-only heuristics when this is false.
   *
   * @return true when symbol resolution is available
   */
  public boolean symbolsResolved() {
    return symbolsResolved;
  }

  /**
   * Creates a finding for a node, at the node's start position, with the rule's configured severity
   * and the source line as snippet.
   *
   * @param rule the rule reporting the finding
   * @param node the offending node
   * @param message what is wrong
   * @param fixHint what to change
   * @return the finding
   */
  public Finding finding(Rule rule, Node node, String message, String fixHint) {
    Position begin = node.getBegin().orElse(new Position(1, 1));
    return finding(rule, begin.line, begin.column, message, fixHint);
  }

  /**
   * Creates a finding at an explicit position, with the rule's configured severity and the source
   * line as snippet.
   *
   * @param rule the rule reporting the finding
   * @param line the 1-based line
   * @param column the 1-based column
   * @param message what is wrong
   * @param fixHint what to change
   * @return the finding
   */
  public Finding finding(Rule rule, int line, int column, String message, String fixHint) {
    String snippet = source.line(line);
    return new Finding(
        rule.id(),
        config.severityOf(rule.id(), rule.defaultSeverity()),
        source.path(),
        Math.max(1, line),
        Math.max(1, column),
        message,
        fixHint,
        snippet.isEmpty() ? Optional.empty() : Optional.of(snippet));
  }
}
