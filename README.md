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

Work in progress, delivered in increments (see [CHANGELOG.md](CHANGELOG.md)). Today the core
module parses test sources, runs the rules over them and prints findings on the console. The
remaining rules, the JSON, SARIF and Markdown reporters, the `tql` command line, the Maven
plugin and the mutation-testing benchmark follow.

## Requirements

- Java 17 or newer
- Maven 3.9 or newer

## Install

The linter is not on Maven Central yet (planned, see [docs/releasing.md](docs/releasing.md)).
Build it once on your machine and install it into your local Maven repository (`~/.m2`):

```bash
git clone https://github.com/byreshb/test-quality-linter.git
cd test-quality-linter
mvn install
```

Then depend on the core module from your own build:

```xml
<dependency>
  <groupId>io.github.byreshb</groupId>
  <artifactId>tql-core</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Quick start

Until the command line and Maven plugin exist, run the linter from Java:

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

The console output groups findings by file, one line each with position, severity, rule id and
message, followed by the offending line and a fix hint:

```text
src/test/java/com/acme/OrderServiceTest.java
  27:5   ERROR  TQL001  assertEquals compares order with itself and cannot fail
         | assertEquals(order, order);
         fix: Assert against an expected value that is computed independently of the code under test

1 finding in 12 files (1 error, 0 warnings, 0 info)
```

## Rules

| Id | Name | Severity | What it catches |
|----|------|----------|-----------------|
| [TQL001](docs/rules/TQL001.md) | TautologicalAssertion | ERROR | An assertion compares an expression with itself or can never fail. |
| [TQL002](docs/rules/TQL002.md) | ConstantAssertion | ERROR | Both sides of an assertion are literals or compile-time constants. |
| [TQL003](docs/rules/TQL003.md) | NoAssertion | WARN | A test method has no assertion, verification or expected exception. |
| [TQL006](docs/rules/TQL006.md) | HardcodedSleep | WARN | A fixed sleep or timeout is used instead of waiting for a condition. |
| [TQL007](docs/rules/TQL007.md) | SwallowedException | WARN | A catch block in a test is empty or only prints or logs the exception. |

The full list with bad and fixed examples is in [docs/rules](docs/rules/README.md).

## Reference

- `Linter`: `withDefaults()`, or `new Linter(RuleRegistry, RuleConfig)`; `lintPaths(paths)`
  walks directories for `.java` files, `lint(sources)` takes in-memory `SourceFile`s.
- `RuleConfig.builder()`: `disable(id)`, `severity(id, Severity)`, `option(id, key, value)`,
  `exclude(glob)`.
- `RuleRegistry.discover()` finds every rule registered as a `ServiceLoader` service, so a rule
  in another jar is picked up by putting the jar on the classpath.
- `LintResult`: `findings()` sorted by file and position, `problems()` for files that did not
  parse, `countByRule()`, `countBySeverity()`, `hasFindingsAtOrAbove(Severity)`.
- `ConsoleReporter`: `forTerminal()` colours when standard output is a terminal and `NO_COLOR`
  is unset; `new ConsoleReporter(false)` never colours.

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

## License

Apache License 2.0, see [LICENSE](LICENSE).
