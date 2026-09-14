package io.github.byreshb.tql.model;

import java.nio.file.Path;
import java.util.Objects;

/**
 * A file the linter could not parse. Such files produce no findings; the problem is reported so
 * that a syntax error is not mistaken for a clean file.
 *
 * @param file the file that failed to parse
 * @param line the 1-based line of the first syntax error, or 0 when unknown
 * @param message the parser's description of the error
 */
public record ParseProblem(Path file, int line, String message) {

  /** Validates the arguments. */
  public ParseProblem {
    Objects.requireNonNull(file, "file");
    Objects.requireNonNull(message, "message");
  }
}
