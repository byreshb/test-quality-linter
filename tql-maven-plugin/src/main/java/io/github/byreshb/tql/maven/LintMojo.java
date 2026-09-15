package io.github.byreshb.tql.maven;

import io.github.byreshb.tql.engine.Linter;
import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.LintResult;
import io.github.byreshb.tql.model.Severity;
import io.github.byreshb.tql.output.ConsoleReporter;
import io.github.byreshb.tql.output.SarifReporter;
import io.github.byreshb.tql.rule.RuleConfig;
import io.github.byreshb.tql.rule.RuleConfigLoader;
import io.github.byreshb.tql.rule.RuleRegistry;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Lints the project's test sources and fails the build when a finding reaches {@link
 * #failOnSeverity} or higher, or a file could not be parsed. Every finding is logged through the
 * Maven build log at a level matching its severity, and a SARIF report is always written to {@code
 * target/tql/tql.sarif}, pass or fail, so CI can upload it either way.
 */
@Mojo(name = "lint", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public final class LintMojo extends AbstractMojo {

  /** Directory to scan for {@code .java} test sources. */
  @Parameter(
      property = "tql.testSourceDirectory",
      defaultValue = "${project.build.testSourceDirectory}",
      required = true)
  File testSourceDirectory;

  /** A {@code .tql.yaml} configuration file; defaults apply when it does not exist. */
  @Parameter(property = "tql.configFile", defaultValue = "${project.basedir}/.tql.yaml")
  File configFile;

  /** The minimum severity that fails the build: {@code INFO}, {@code WARN} or {@code ERROR}. */
  @Parameter(property = "tql.failOnSeverity", defaultValue = "ERROR")
  String failOnSeverity;

  /** Directory the SARIF report is written into, as {@code tql.sarif}. */
  @Parameter(
      property = "tql.outputDirectory",
      defaultValue = "${project.build.directory}/tql",
      required = true)
  File outputDirectory;

  /** Skips this execution entirely when {@code true}. */
  @Parameter(property = "tql.skip", defaultValue = "false")
  boolean skip;

  /** Runs the lint, per the Mojo contract. */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      getLog().info("tql:lint skipped");
      return;
    }
    Path testSources = testSourceDirectory.toPath();
    if (!Files.isDirectory(testSources)) {
      getLog().info("No test sources at " + testSources + "; skipping tql:lint");
      return;
    }

    Linter linter = new Linter(RuleRegistry.discover(), loadConfig());
    LintResult result = linter.lintPaths(List.of(testSources));

    logFindings(result);
    writeSarif(linter, result);

    Severity threshold = Severity.parse(failOnSeverity);
    if (result.hasFindingsAtOrAbove(threshold) || !result.problems().isEmpty()) {
      throw new MojoFailureException(
          "tql found "
              + result.findings().size()
              + " finding(s) at or above "
              + threshold
              + (result.problems().isEmpty()
                  ? ""
                  : " and " + result.problems().size() + " file(s) that could not be parsed")
              + "; see "
              + outputDirectory.toPath().resolve("tql.sarif"));
    }
  }

  private RuleConfig loadConfig() {
    if (configFile != null && configFile.isFile()) {
      return RuleConfigLoader.load(configFile.toPath());
    }
    return RuleConfig.defaults();
  }

  private void logFindings(LintResult result) {
    for (Finding finding : result.findings()) {
      String line = finding.toLine();
      switch (finding.severity()) {
        case ERROR -> getLog().error(line);
        case WARN -> getLog().warn(line);
        case INFO -> getLog().info(line);
      }
    }
    result
        .problems()
        .forEach(p -> getLog().warn("Could not parse " + p.file() + ": " + p.message()));
    getLog().info(ConsoleReporter.summary(result));
  }

  private void writeSarif(Linter linter, LintResult result) throws MojoExecutionException {
    try {
      Files.createDirectories(outputDirectory.toPath());
      Path sarifFile = outputDirectory.toPath().resolve("tql.sarif");
      String version = getClass().getPackage().getImplementationVersion();
      SarifReporter reporter =
          new SarifReporter(linter.enabledRules(), version != null ? version : "development");
      Files.writeString(sarifFile, reporter.render(result), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new MojoExecutionException("Cannot write SARIF report", e);
    }
  }
}
