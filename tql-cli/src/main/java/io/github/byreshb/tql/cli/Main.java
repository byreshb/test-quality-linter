package io.github.byreshb.tql.cli;

import java.io.PrintWriter;

/**
 * Executable entry point of the {@code tql} command line. Delegates to {@link Cli#execute}, which
 * holds the actual logic and is what tests call directly.
 */
public final class Main {

  private Main() {}

  /**
   * Runs the CLI and exits the JVM with its result.
   *
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    int exitCode =
        Cli.execute(args, new PrintWriter(System.out, true), new PrintWriter(System.err, true));
    System.exit(exitCode);
  }
}
