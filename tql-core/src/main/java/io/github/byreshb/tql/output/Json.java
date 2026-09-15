package io.github.byreshb.tql.output;

import java.io.IOException;

/** A minimal JSON string writer shared by {@link JsonReporter} and {@link SarifReporter}. */
final class Json {

  private Json() {}

  /**
   * Writes a JSON string literal, quoted and escaped.
   *
   * @param out where to write
   * @param value the raw text
   * @throws IOException when writing fails
   */
  static void writeString(Appendable out, String value) throws IOException {
    out.append('"');
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"':
          out.append("\\\"");
          break;
        case '\\':
          out.append("\\\\");
          break;
        case '\n':
          out.append("\\n");
          break;
        case '\r':
          out.append("\\r");
          break;
        case '\t':
          out.append("\\t");
          break;
        default:
          if (c < 0x20) {
            out.append(String.format("\\u%04x", (int) c));
          } else {
            out.append(c);
          }
      }
    }
    out.append('"');
  }
}
