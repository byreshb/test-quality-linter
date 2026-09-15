# Test Quality Linter (tql)

[![CI](https://github.com/byreshb/test-quality-linter/actions/workflows/ci.yml/badge.svg)](https://github.com/byreshb/test-quality-linter/actions/workflows/ci.yml)

A static linter for Java test code that finds tests which pass but prove nothing.

## The problem

Code generators now write tests in bulk, and a lot of those tests are green for the wrong
reason. They compile, they run, they pass, and they do not check anything that could break:
a value compared to itself, a mock's stubbed return value asserted against the very literal
that was stubbed, a `Thread.sleep` where a wait belongs, an assertion that can never fail, a
test body with no assertion at all. Coverage tools count every one of them as covered lines.
Reviewers, faced with forty near-identical methods, skim.

`tql` reads test sources and flags those patterns. Each rule has an id, a severity, a message
and a fix hint, so the finding tells you what to change, not only that something is wrong.
Findings come out on the console, as JSON, as SARIF for GitHub code scanning, or as Markdown
for a pull request comment. Mutation testing on a benchmark project shows that the flagged
tests kill fewer mutants than the clean ones, so the flags are worth acting on rather than
noise to suppress.

## Status

Every rule (TQL001-TQL012), console/JSON/SARIF/Markdown output, the `tql` command line, the
Maven plugin, and a mutation-testing benchmark that validates the rules with real numbers (see
below). See [CHANGELOG.md](CHANGELOG.md) for what shipped in each increment.

## Requirements

- Java 17 or newer
- Maven 3.9 or newer

## Install

Not on Maven Central yet (planned, see [docs/releasing.md](docs/releasing.md)). Two ways to get
`tql` today:

**Download the CLI jar** from a [GitHub Release](https://github.com/byreshb/test-quality-linter/releases)
once one exists, or build it from a checkout:

```bash
git clone https://github.com/byreshb/test-quality-linter.git
cd test-quality-linter
mvn package -pl tql-cli -am -DskipTests
java -jar tql-cli/target/tql-cli-*.jar --help
```

**Depend on `tql-core`** to call the linter from Java: `mvn install` from a checkout, then

```xml
<dependency>
  <groupId>io.github.byreshb</groupId>
  <artifactId>tql-core</artifactId>
  <version>1.1.0</version>
</dependency>
```

## Quick start

```bash
java -jar tql-cli-1.1.0.jar lint src/test/java
```

```text
src/test/java/com/acme/OrderServiceTest.java
  27:5   ERROR  TQL001  assertEquals compares order with itself and cannot fail
         | assertEquals(order, order);
         fix: Assert against an expected value that is computed independently of the code under test

1 finding in 12 files (1 error, 0 warnings, 0 info)
```

`tql` exits non-zero when a finding reaches `--fail-on` (default `ERROR`) or a file could not be
parsed, so `tql lint src/test/java` works as a CI gate on its own. For GitHub code scanning:

```bash
tql lint src/test/java --format sarif > target/tql.sarif
```

See [docs/ci-integration.md](docs/ci-integration.md) for the full GitHub Actions workflow, or use
the [`action/`](action) package directly:

```yaml
- uses: byreshb/test-quality-linter/action@v1
  with:
    paths: src/test/java
    fail-on: error
```

### VS Code

[`vscode-extension/`](vscode-extension) lints Java test files on open and save, right in the
editor: inline diagnostics with the rule id and fix hint, a quick fix that inserts
`// tql:ignore <id>`, and a **TQL: Explain rule** command. Install the `.vsix` from a
[GitHub Release](https://github.com/byreshb/test-quality-linter/releases) (Marketplace publishing
is planned, not done yet) with **Extensions → … → Install from VSIX…**.

### Maven plugin

Binds `tql:lint` to the `verify` phase:

```xml
<plugin>
  <groupId>io.github.byreshb</groupId>
  <artifactId>tql-maven-plugin</artifactId>
  <version>1.1.0</version>
  <executions>
    <execution>
      <goals>
        <goal>lint</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

Scans `${project.build.testSourceDirectory}` (`src/test/java` by default), always writes a SARIF
report to `target/tql/tql.sarif`, and fails the build (`MojoFailureException`) on a finding at or
above `failOnSeverity` (default `ERROR`) or a file that could not be parsed. Parameters, each
also settable as a `-Dtql.<name>=...` system property: `testSourceDirectory`, `configFile`
(defaults to `.tql.yaml` in the project's base directory), `failOnSeverity`, `outputDirectory`
(default `target/tql`), `skip`.

### Commands

- `tql lint <paths...> [--config FILE] [--format console|json|sarif|md] [--classpath PATH] [--fail-on info|warn|error]`
  Lints files and directories (recursively, for `.java` files). `--config` defaults to
  `.tql.yaml` in the working directory when present. `--classpath` (entries separated like the
  platform path separator) enables symbol resolution for the rules that use it.
- `tql rules` lists every rule with its id, name, default severity and description.
- `tql explain <RULE_ID>` prints a rule's description, default severity and a link to its docs
  page.

### Using the library directly

```java
import io.github.byreshb.tql.engine.Linter;
import io.github.byreshb.tql.model.LintResult;
import io.github.byreshb.tql.model.Severity;
import io.github.byreshb.tql.output.ConsoleReporter;
import java.nio.file.Path;
import java.util.List;

public class LintTests {
  public static void main(String[] args) {
    Linter linter = Linter.withDefaults();
    LintResult result = linter.lintPaths(List.of(Path.of("src/test/java")));
    System.out.print(ConsoleReporter.forTerminal().render(result));
    System.exit(result.hasFindingsAtOrAbove(Severity.ERROR) ? 1 : 0);
  }
}
```

## Rules

| Id | Name | Severity | What it catches |
|----|------|----------|-----------------|
| [TQL001](docs/rules/TQL001.md) | TautologicalAssertion | ERROR | An assertion compares an expression with itself or can never fail. |
| [TQL002](docs/rules/TQL002.md) | ConstantAssertion | ERROR | Both sides of an assertion are literals or compile-time constants. |
| [TQL003](docs/rules/TQL003.md) | NoAssertion | WARN | A test method has no assertion, verification or expected exception. |
| [TQL004](docs/rules/TQL004.md) | AssertsOnStub | ERROR | An assertion compares a mock's return value to the literal it was stubbed with. |
| [TQL005](docs/rules/TQL005.md) | VerifyOnly | WARN | A test contains only mock verifications and no assertion on an observable outcome. |
| [TQL006](docs/rules/TQL006.md) | HardcodedSleep | WARN | A fixed sleep or timeout is used instead of waiting for a condition. |
| [TQL007](docs/rules/TQL007.md) | SwallowedException | WARN | A catch block in a test is empty or only prints or logs the exception. |
| [TQL008](docs/rules/TQL008.md) | DuplicateTestBody | WARN | Two test methods in the same class have identical bodies. |
| [TQL009](docs/rules/TQL009.md) | DisabledWithoutReason | WARN | @Disabled or @Ignore has no reason, or has stood disabled past the configured age. |
| [TQL010](docs/rules/TQL010.md) | ContradictoryMessage | INFO | An assertion's message says the opposite of what the assertion checks. |
| [TQL011](docs/rules/TQL011.md) | UnusedTestResult | WARN | A call's return value is discarded as a bare statement and never checked. |
| [TQL012](docs/rules/TQL012.md) | MockOfTypeUnderTest | WARN | The class named in the test class name is mocked instead of exercised. |

The full list with bad and fixed examples is in [docs/rules](docs/rules/README.md).
Configuration (`.tql.yaml`, severity overrides, options, suppressions) is documented in
[docs/configuration.md](docs/configuration.md).

## Reference

- `Linter`: `withDefaults()`, or `new Linter(RuleRegistry, RuleConfig)`; `lintPaths(paths)`
  walks directories for `.java` files, `lint(sources)` takes in-memory `SourceFile`s. Pass a
  (possibly empty) classpath as a third constructor argument, or use `withClasspath(...)`, to
  enable symbol resolution.
- `RuleConfig.builder()`: `disable(id)`, `severity(id, Severity)`, `option(id, key, value)`,
  `exclude(glob)`; `RuleConfigLoader.load(path)` builds one from a `.tql.yaml` file (see
  [docs/configuration.md](docs/configuration.md)).
- `RuleRegistry.discover()` finds every rule registered as a `ServiceLoader` service, so a rule
  in another jar is picked up by putting the jar on the classpath.
- `LintResult`: `findings()` sorted by file and position, `problems()` for files that did not
  parse, `countByRule()`, `countBySeverity()`, `hasFindingsAtOrAbove(Severity)`.
- Reporters: `ConsoleReporter` (`forTerminal()` colours when standard output is a terminal and
  `NO_COLOR` is unset), `JsonReporter`, `SarifReporter(rules[, toolVersion])`,
  `MarkdownReporter`; every reporter has a `render(LintResult)` returning a `String`.

How the pieces fit together, and how to add a rule or a reporter, is in
[docs/design.md](docs/design.md).

## Building and testing

```bash
mvn test                # unit tests of every module
mvn verify              # tests + coverage report in */target/site/jacoco (fails under 85% lines)
mvn spotless:check      # verify formatting without changing anything (for CI)
mvn javadoc:javadoc     # API docs in */target/site/apidocs
```

Formatting is automatic: every build runs [Spotless](https://github.com/diffplug/spotless) with
google-java-format (Google style, annotations on their own line) over the sources before
compiling, so you never need to format by hand.

## Continuous integration

Every push and pull request runs the GitHub Actions workflow in `.github/workflows/ci.yml`:
formatting check, full test suite with the coverage gate, and upload of the Surefire reports.

## Releasing

1. Move the `Unreleased` notes in `CHANGELOG.md` under a new version heading and set that
   version in every `pom.xml`.
2. Commit, then tag and push: `git tag -a v1.2.3 -m "Release 1.2.3" && git push origin v1.2.3`.
3. The release workflow in `.github/workflows/release.yml` checks the tag matches the pom,
   builds the jars, and publishes a GitHub Release with the changelog section as its notes.

Full steps, including the planned but not yet configured Maven Central publishing, are in
[docs/releasing.md](docs/releasing.md).

## Benchmark

[`tql-benchmark`](tql-benchmark) is a sample project of 47 hand-written JUnit tests, 23 of them
deliberately weak in the exact style each rule targets and labelled with the rule id they should
trigger, the other 24 ordinary clean tests. Running `tql` against it and mutation testing the
result with [PIT](https://pitest.org/) gives two real numbers rather than an assertion that the
rules matter:

- Every rule's precision and recall against the planted labels: **100%** across all twelve.
- Mutants killed per test, averaged: **0.30** for the tests `tql` flags, **1.38** for the ones it
  leaves clean, a tests `tql` flags kill roughly a fifth as many mutants as the ones it does not.

Full breakdown, including a line-by-line findings table, in [docs/benchmark.md](docs/benchmark.md).
Regenerate it with:

```bash
mvn -pl tql-benchmark -am install -DskipTests
mvn -pl tql-benchmark org.pitest:pitest-maven:mutationCoverage
mvn -pl tql-benchmark exec:java@benchmark-report
```

## License

Apache License 2.0, see [LICENSE](LICENSE).
