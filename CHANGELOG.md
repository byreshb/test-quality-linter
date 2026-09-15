# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- `tql-benchmark`: 47 hand-written tests (23 deliberately weak, labelled with the rule id each
  should trigger; 24 clean), a `BenchmarkRunner` that runs the linter and reads a PIT mutation
  report to regenerate `docs/benchmark.md` with precision/recall per rule and mutation score of
  flagged vs clean tests. Excluded from the coverage gate; not bound to any build phase (PIT is
  slow), run on demand.

## [1.0.0] - 2026-09-14

### Added
- Multi-module Maven build (`tql-core`, `tql-cli`, `tql-maven-plugin`) with automatic formatting
  (Spotless, google-java-format), a JaCoCo coverage gate at 85% lines, GitHub Actions CI and
  release workflows.
- Core model (`Finding`, `SourceFile`, `LintResult`), rule API (`Rule`, `RuleContext`,
  `RuleConfig`, `RuleRegistry` with `ServiceLoader` discovery), the JavaParser-based `Linter`,
  and suppressions: a trailing `// tql:ignore [rule-id...]` line comment and
  `@SuppressWarnings("tql:rule-id")` (or `"tql:*"`) on a method, constructor, class, interface,
  enum or field.
- Optional symbol resolution: `Linter` accepts a classpath and resolves types with JavaParser's
  `JavaSymbolSolver` (JDK types resolve through reflection even with an empty classpath).
- `.tql.yaml` configuration loading (`RuleConfigLoader`): enable/disable, severity overrides,
  per-rule options and exclude globs.
- The full set of rules:
  - TQL001 TautologicalAssertion: an assertion compares an expression with itself or can never
    fail.
  - TQL002 ConstantAssertion: both sides of an assertion are literals or compile-time constants.
  - TQL003 NoAssertion (options `methods`, `packages`): a test has no assertion, verification or
    expected exception.
  - TQL004 AssertsOnStub: an assertion compares a mock's return value to the literal it was
    stubbed with.
  - TQL005 VerifyOnly: a test contains only mock verifications and no assertion on an
    observable outcome.
  - TQL006 HardcodedSleep (option `maxMillis`): a fixed sleep or timeout instead of waiting for
    a condition.
  - TQL007 SwallowedException: a catch block is empty or only prints or logs the exception.
  - TQL008 DuplicateTestBody: two test methods in the same class have identical bodies.
  - TQL009 DisabledWithoutReason (option `maxAgeDays`, using `git blame` when available):
    `@Disabled`/`@Ignore` has no reason, or has stood disabled past a configured age.
  - TQL010 ContradictoryMessage: an assertion's message says the opposite of what it checks.
  - TQL011 UnusedTestResult (needs symbol resolution): a call's return value is discarded and
    never checked.
  - TQL012 MockOfTypeUnderTest: the class named in the test class name is mocked.
- Reporters: `ConsoleReporter`, `JsonReporter`, `SarifReporter` (SARIF 2.1.0, with a
  `tool.driver.rules` array), `MarkdownReporter`.
- `tql-cli`: the `tql` command (`lint`, `rules`, `explain`), packaged as a shaded executable jar.
- `tql-maven-plugin`: `tql:lint`, bound to the `verify` phase by default, scanning
  `src/test/java`, writing a SARIF report to `target/tql/tql.sarif` and failing the build on a
  finding at or above `failOnSeverity` (default `ERROR`) or an unparseable file.
- Documentation: README, `docs/design.md`, `docs/configuration.md`, `docs/ci-integration.md`,
  `docs/releasing.md`, and a docs page per rule under `docs/rules`.

[Unreleased]: https://github.com/byreshb/test-quality-linter/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/byreshb/test-quality-linter/releases/tag/v1.0.0
