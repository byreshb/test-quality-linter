package io.github.byreshb.tql.output;

import io.github.byreshb.tql.model.LintResult;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Renders a {@link LintResult} in one output format. */
public interface Reporter {

  /**
   * Writes the result.
   *
   * @param result the result to render
   * @param out where to write
   * @throws IOException when writing fails
   */
  void write(LintResult result, Appendable out) throws IOException;

  /**
   * Renders the result to a string.
   *
   * @param result the result to render
   * @return the rendered text
   */
  default String render(LintResult result) {
    StringBuilder out = new StringBuilder();
    try {
      write(result, out);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return out.toString();
  }
}
