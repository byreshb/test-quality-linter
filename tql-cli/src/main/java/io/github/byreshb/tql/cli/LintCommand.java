package io.github.byreshb.tql.cli;

import io.github.byreshb.tql.engine.Linter;
import io.github.byreshb.tql.model.LintResult;
import io.github.byreshb.tql.model.Severity;
import io.github.byreshb.tql.output.ConsoleReporter;
import io.github.byreshb.tql.output.JsonReporter;
import io.github.byreshb.tql.output.MarkdownReporter;
import io.github.byreshb.tql.output.Reporter;
import io.github.byreshb.tql.output.SarifReporter;
import io.github.byreshb.tql.rule.RuleConfig;
import io.github.byreshb.tql.rule.RuleConfigLoader;
import io.github.byreshb.tql.rule.RuleRegistry;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * {@code tql lint}: parses the given files and directories and reports the findings. Exits non-zero
 * when a finding reaches {@code --fail-on} or higher, or when a file could not be parsed at all.
 */
@Command(name = "lint", description = "Lint Java test sources for tests that prove nothing.")
final class LintCommand implements Callable<Integer> {

  /** Output formats {@code tql lint} can render. */
  enum Format {
    CONSOLE,
    JSON,
    SARIF,
    MD
  }

  @Parameters(paramLabel = "PATH", description = "Files or directories to lint", arity = "1..*")
  private List<Path> paths;

  @Option(
      names = "--config",
      paramLabel = "FILE",
      description =
          "Path to a .tql.yaml configuration file; defaults to .tql.yaml in the current"
              + " directory when present")
  private Path config;

  @Option(
      names = "--format",
      paramLabel = "FORMAT",
      defaultValue = "CONSOLE",
      description = "Output format: console, json, sarif or md")
  private Format format;

  @Option(
      names = "--classpath",
      paramLabel = "PATH",
      description =
          "Classpath entries (jars or class directories), separated like the platform path"
              + " separator; enables symbol resolution for rules that need it")
  private String classpath;

  @Option(
      names = "--fail-on",
      paramLabel = "SEVERITY",
      defaultValue = "ERROR",
      description =
          "Exit non-zero when a finding reaches this severity or higher: info, warn or error")
  private Severity failOn;

  @Spec private CommandSpec spec;

  @Override
  public Integer call() {
    RuleConfig ruleConfig = resolveConfig();
    Linter linter = buildLinter(ruleConfig);
    LintResult result = linter.lintPaths(paths);
    PrintWriter out = spec.commandLine().getOut();
    out.print(reporterFor(format, linter).render(result));
    out.flush();
    boolean failed = result.hasFindingsAtOrAbove(failOn) || !result.problems().isEmpty();
    return failed ? 1 : 0;
  }

  private RuleConfig resolveConfig() {
    if (config != null) {
      return RuleConfigLoader.load(config);
    }
    Path defaultConfig = Path.of(".tql.yaml");
    return Files.isRegularFile(defaultConfig)
        ? RuleConfigLoader.load(defaultConfig)
        : RuleConfig.defaults();
  }

  private Linter buildLinter(RuleConfig ruleConfig) {
    RuleRegistry registry = RuleRegistry.discover();
    if (classpath == null) {
      return new Linter(registry, ruleConfig);
    }
    List<Path> entries = new ArrayList<>();
    for (String entry : classpath.split(File.pathSeparator, -1)) {
      if (!entry.isBlank()) {
        entries.add(Path.of(entry));
      }
    }
    return new Linter(registry, ruleConfig, entries);
  }

  private static Reporter reporterFor(Format format, Linter linter) {
    return switch (format) {
      case CONSOLE -> ConsoleReporter.forTerminal();
      case JSON -> new JsonReporter();
      case SARIF -> new SarifReporter(linter.enabledRules(), Cli.version());
      case MD -> new MarkdownReporter();
    };
  }
}
