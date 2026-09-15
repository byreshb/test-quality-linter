package io.github.byreshb.tql.cli;

import io.github.byreshb.tql.rule.Rule;
import io.github.byreshb.tql.rule.RuleRegistry;
import java.io.PrintWriter;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/** {@code tql rules}: lists every discovered rule with its id, name, severity and description. */
@Command(name = "rules", description = "List the available rules")
final class RulesCommand implements Callable<Integer> {

  @Spec private CommandSpec spec;

  @Override
  public Integer call() {
    PrintWriter out = spec.commandLine().getOut();
    for (Rule rule : RuleRegistry.discover().rules()) {
      out.printf("%-7s %-24s [%s]%n", rule.id(), rule.name(), rule.defaultSeverity());
      out.println("        " + rule.description());
    }
    out.flush();
    return 0;
  }
}
