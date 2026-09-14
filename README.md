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

Work in progress. This first commit contains the build, the CI and release workflows and the
`tql-core` module skeleton. The rules, engine, reporters, CLI and Maven plugin follow, one
increment at a time; see [CHANGELOG.md](CHANGELOG.md).

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
