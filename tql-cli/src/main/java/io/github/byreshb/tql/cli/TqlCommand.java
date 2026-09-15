package io.github.byreshb.tql.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/** The root {@code tql} command: shows usage when run without a subcommand. */
@Command(
    name = "tql",
    mixinStandardHelpOptions = true,
    versionProvider = ManifestVersionProvider.class,
    description = "Static linter for Java test code that finds tests which pass but prove nothing.",
    subcommands = {LintCommand.class, RulesCommand.class, ExplainCommand.class})
final class TqlCommand implements Runnable {

  @Spec private CommandSpec spec;

  @Override
  public void run() {
    spec.commandLine().usage(spec.commandLine().getOut());
  }
}
