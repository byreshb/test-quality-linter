package io.github.byreshb.tql.model;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * A Java source file to lint: a path and its content. The content is kept so that rules can quote
 * the offending line and so that in-memory sources (tests, editors) can be linted without touching
 * the file system.
 *
 * @param path the path of the file; for in-memory sources any descriptive relative path
 * @param content the full source text
 */
public record SourceFile(Path path, String content) {

  /** Validates the arguments. */
  public SourceFile {
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(content, "content");
  }

  /**
   * Creates an in-memory source file.
   *
   * @param path a descriptive path such as {@code FooTest.java}
   * @param content the source text
   * @return the source file
   */
  public static SourceFile of(String path, String content) {
    return new SourceFile(Path.of(path), content);
  }

  /**
   * Reads a file from disk as UTF-8.
   *
   * @param path the file to read
   * @return the source file
   * @throws UncheckedIOException when the file cannot be read
   */
  public static SourceFile read(Path path) {
    try {
      return new SourceFile(path, Files.readString(path, StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read " + path, e);
    }
  }

  /**
   * Returns the 1-based line of the content, trimmed, or an empty string when out of range.
   *
   * @param line the 1-based line number
   * @return the trimmed line
   */
  public String line(int line) {
    List<String> lines = content.lines().toList();
    if (line < 1 || line > lines.size()) {
      return "";
    }
    return lines.get(line - 1).trim();
  }
}
