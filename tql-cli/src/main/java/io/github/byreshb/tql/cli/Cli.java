package io.github.byreshb.tql.cli;

import java.io.PrintWriter;
import picocli.CommandLine;

/**
 * Programmatic entry point of the {@code tql} command line, kept separate from {@link Main} so
 * tests can capture output and an exit code without the process actually exiting.
 */
public final class Cli {

  private Cli() {}

  /**
   * Runs the CLI.
   *
   * @param args the command line arguments
   * @param out where normal output goes
   * @param err where error output goes
   * @return the process exit code
   */
  public static int execute(String[] args, PrintWriter out, PrintWriter err) {
    CommandLine commandLine = new CommandLine(new TqlCommand());
    commandLine.setCaseInsensitiveEnumValuesAllowed(true);
    commandLine.setOut(out);
    commandLine.setErr(err);
    return commandLine.execute(args);
  }

  /**
   * The linter's own version, read from the jar manifest.
   *
   * @return the version, or {@code "development"} when run from classes rather than a built jar
   */
  static String version() {
    String version = Cli.class.getPackage().getImplementationVersion();
    return version != null ? version : "development";
  }
}
