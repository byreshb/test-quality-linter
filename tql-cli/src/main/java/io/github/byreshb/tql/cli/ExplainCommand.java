package io.github.byreshb.tql.cli;

import io.github.byreshb.tql.rule.Rule;
import io.github.byreshb.tql.rule.RuleRegistry;
import java.io.PrintWriter;
import java.util.Optional;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** {@code tql explain <id>}: prints a rule's description, default severity and docs link. */
@Command(name = "explain", description = "Explain one rule")
final class ExplainCommand implements Callable<Integer> {

  @Parameters(paramLabel = "RULE_ID", description = "The rule id, e.g. TQL001")
  private String ruleId;

  @Spec private CommandSpec spec;

  @Override
  public Integer call() {
    Optional<Rule> found = RuleRegistry.discover().find(ruleId);
    if (found.isEmpty()) {
      spec.commandLine().getErr().println("Unknown rule: " + ruleId);
      return 1;
    }
    Rule rule = found.get();
    PrintWriter out = spec.commandLine().getOut();
    out.println(rule.id() + " " + rule.name());
    out.println(rule.description());
    out.println();
    out.println("Default severity: " + rule.defaultSeverity());
    out.println(
        "Docs: https://github.com/byreshb/test-quality-linter/blob/main/docs/rules/"
            + rule.id()
            + ".md");
    out.flush();
    return 0;
  }
}
