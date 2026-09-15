package io.github.byreshb.tql.rule;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Exercises {@link ShellGitBlame} against a throwaway git repository created for this test, so the
 * result does not depend on this project's own commit history.
 */
class ShellGitBlameTest {

  private static boolean gitAvailable;

  @BeforeAll
  static void checkGitAvailable() {
    try {
      Process process = new ProcessBuilder("git", "--version").start();
      gitAvailable = process.waitFor() == 0;
    } catch (IOException | InterruptedException e) {
      gitAvailable = false;
    }
  }

  private final ShellGitBlame gitBlame = new ShellGitBlame();

  @Test
  void returnsTheCommitTimeOfTheGivenLine(@TempDir Path repo) throws Exception {
    org.junit.jupiter.api.Assumptions.assumeTrue(gitAvailable, "git is not available");
    run(repo, "init", "-q", "-b", "main");
    run(repo, "config", "user.email", "test@example.com");
    run(repo, "config", "user.name", "Test");
    Path file = repo.resolve("Sample.java");
    Files.writeString(file, "class Sample {\n  int a = 1;\n  int b = 2;\n}\n");
    Instant commitTime = Instant.ofEpochSecond(1_700_000_000L);
    runWithDate(repo, commitTime, "add", "Sample.java");
    runWithDate(repo, commitTime, "commit", "-q", "-m", "initial");

    Optional<Instant> result = gitBlame.lastChanged(file, 2);

    assertThat(result).contains(commitTime);
  }

  @Test
  void returnsEmptyForAFileNotOnDisk(@TempDir Path repo) {
    assertThat(gitBlame.lastChanged(repo.resolve("missing.java"), 1)).isEmpty();
  }

  @Test
  void returnsEmptyForALineNumberBelowOne(@TempDir Path repo) throws IOException {
    Path file = repo.resolve("Sample.java");
    Files.writeString(file, "class Sample {}\n");
    assertThat(gitBlame.lastChanged(file, 0)).isEmpty();
  }

  @Test
  void returnsEmptyWhenTheFileIsNotInARepository(@TempDir Path repo) throws IOException {
    Path file = repo.resolve("Sample.java");
    Files.writeString(file, "class Sample {}\n");
    // No git init here: repo is a plain temp directory.
    assertThat(gitBlame.lastChanged(file, 1)).isEmpty();
  }

  private static void run(Path dir, String... args) throws IOException, InterruptedException {
    List<String> command = new java.util.ArrayList<>(List.of("git"));
    command.addAll(List.of(args));
    Process process = new ProcessBuilder(command).directory(dir.toFile()).start();
    process.waitFor();
  }

  private static void runWithDate(Path dir, Instant date, String... args)
      throws IOException, InterruptedException {
    List<String> command = new java.util.ArrayList<>(List.of("git"));
    command.addAll(List.of(args));
    ProcessBuilder builder = new ProcessBuilder(command).directory(dir.toFile());
    String iso = date.toString();
    builder.environment().put("GIT_AUTHOR_DATE", iso);
    builder.environment().put("GIT_COMMITTER_DATE", iso);
    builder.start().waitFor();
  }
}
