package io.github.byreshb.tql.rule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * {@link GitBlame} backed by the {@code git} command line, run with the file's parent directory as
 * the working directory so git discovers the repository upward from there. Any failure (no {@code
 * git} on the path, the file not in a repository, an uncommitted file, a timeout) is treated as
 * "unknown" rather than propagated, since this is a best-effort secondary signal.
 */
public final class ShellGitBlame implements GitBlame {

  private static final java.util.regex.Pattern AUTHOR_TIME =
      java.util.regex.Pattern.compile("^author-time (\\d+)$");

  @Override
  public Optional<Instant> lastChanged(Path file, int line) {
    if (line < 1 || !Files.isRegularFile(file)) {
      return Optional.empty();
    }
    try {
      Process process =
          new ProcessBuilder(
                  "git",
                  "blame",
                  "-L",
                  line + "," + line,
                  "--porcelain",
                  "--",
                  file.getFileName().toString())
              .directory(file.toAbsolutePath().getParent().toFile())
              .redirectErrorStream(false)
              .start();
      Optional<Instant> result = readAuthorTime(process);
      boolean finished = process.waitFor(5, TimeUnit.SECONDS);
      if (!finished || process.exitValue() != 0) {
        return Optional.empty();
      }
      return result;
    } catch (IOException | InterruptedException | UncheckedIOException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      return Optional.empty();
    }
  }

  private static Optional<Instant> readAuthorTime(Process process) throws IOException {
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        var matcher = AUTHOR_TIME.matcher(line);
        if (matcher.matches()) {
          return Optional.of(Instant.ofEpochSecond(Long.parseLong(matcher.group(1))));
        }
      }
    }
    return Optional.empty();
  }
}
