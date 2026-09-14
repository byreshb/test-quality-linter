package io.github.byreshb.tql.model;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * One problem found in a test source file.
 *
 * @param ruleId the id of the rule that produced the finding, for example {@code TQL001}
 * @param severity how serious the finding is
 * @param file the file the finding is in
 * @param line the 1-based line of the offending code
 * @param column the 1-based column of the offending code
 * @param message what is wrong, in one sentence
 * @param fixHint what to change, in one sentence
 * @param snippet the offending source line, trimmed, when available
 */
public record Finding(
    String ruleId,
    Severity severity,
    Path file,
    int line,
    int column,
    String message,
    String fixHint,
    Optional<String> snippet) {

  /** Validates the arguments. */
  public Finding {
    Objects.requireNonNull(ruleId, "ruleId");
    Objects.requireNonNull(severity, "severity");
    Objects.requireNonNull(file, "file");
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(fixHint, "fixHint");
    Objects.requireNonNull(snippet, "snippet");
    if (line < 1 || column < 1) {
      throw new IllegalArgumentException("line and column are 1-based: " + line + ":" + column);
    }
  }

  /**
   * Returns a copy of this finding with another severity, used to apply configured overrides.
   *
   * @param newSeverity the severity of the copy
   * @return the copy
   */
  public Finding withSeverity(Severity newSeverity) {
    return new Finding(ruleId, newSeverity, file, line, column, message, fixHint, snippet);
  }

  /**
   * Formats the finding on one line as {@code file:line:column: SEVERITY TQL001 message}.
   *
   * @return the one-line form
   */
  public String toLine() {
    return file + ":" + line + ":" + column + ": " + severity + " " + ruleId + " " + message;
  }
}
