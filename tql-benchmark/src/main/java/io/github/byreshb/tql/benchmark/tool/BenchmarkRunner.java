package io.github.byreshb.tql.benchmark.tool;

import io.github.byreshb.tql.engine.Linter;
import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.LintResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Runs the linter over the benchmark's test sources, correlates its findings and PIT's mutation
 * report with the planted {@link io.github.byreshb.tql.benchmark.Weak} ground truth, and writes
 * {@code docs/benchmark.md} at the repository root.
 *
 * <p>Run via {@code mvn -pl tql-benchmark exec:java@benchmark-report}, after {@code mvn -pl
 * tql-benchmark org.pitest:pitest-maven:mutationCoverage} has produced {@code
 * target/pit-reports/mutations.xml}; see {@code docs/benchmark.md}'s own "Method" section.
 */
public final class BenchmarkRunner {

  private static final String PACKAGE = "io.github.byreshb.tql.benchmark";

  private BenchmarkRunner() {}

  /**
   * Entry point.
   *
   * @param args unused
   * @throws Exception when a source or report file cannot be read
   */
  public static void main(String[] args) throws Exception {
    Path moduleDir = moduleDirectory();
    Path testSources = moduleDir.resolve("src/test/java/io/github/byreshb/tql/benchmark");
    Path testClasses = moduleDir.resolve("target/test-classes");
    Path mainClasses = moduleDir.resolve("target/classes");
    Path mutationsXml = moduleDir.resolve("target/pit-reports/mutations.xml");
    Path docsFile = moduleDir.resolve("../docs/benchmark.md").normalize();

    List<GroundTruth> truth = GroundTruthReader.read(testClasses, PACKAGE);

    // Only the top-level *.java files directly in this directory: the planted examples, not the
    // benchmark tool's own tests under the tool/ subpackage.
    List<Path> plantedSources;
    try (var stream = Files.list(testSources)) {
      plantedSources = stream.filter(p -> p.toString().endsWith(".java")).sorted().toList();
    }

    Linter linter = Linter.withClasspath(List.of(mainClasses));
    LintResult result = linter.lintPaths(plantedSources);
    Map<String, List<Finding>> findingsByMethod = FindingAttributor.byTestMethod(result.findings());

    Map<String, Integer> killedByMethod =
        Files.isRegularFile(mutationsXml)
            ? MutationReport.killedMutantsByTestMethod(mutationsXml)
            : Map.of();

    String report =
        BenchmarkReport.render(linter.enabledRules(), truth, findingsByMethod, killedByMethod);
    Files.writeString(docsFile, report, StandardCharsets.UTF_8);
    System.out.println("Wrote " + docsFile);
    System.out.println(
        "Planted "
            + truth.size()
            + " tests ("
            + truth.stream().filter(GroundTruth::isWeak).count()
            + " weak); tql produced "
            + result.findings().size()
            + " findings; mutation data "
            + (killedByMethod.isEmpty() ? "not available" : "loaded"));
  }

  /**
   * The {@code tql-benchmark} module's own base directory, found from where this class's own {@code
   * .class} file was loaded from ({@code <moduleDir>/target/classes/...}) rather than the process's
   * working directory, which {@code exec:java} does not reliably set to the module.
   *
   * @return the module's base directory
   * @throws java.net.URISyntaxException when the code source location is not a valid URI
   */
  private static Path moduleDirectory() throws java.net.URISyntaxException {
    Path classesDir =
        Path.of(BenchmarkRunner.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    return classesDir.getParent().getParent();
  }
}
