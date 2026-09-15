# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- Core model (`Finding`, `SourceFile`, `LintResult`), rule API (`Rule`, `RuleContext`,
  `RuleConfig`, `RuleRegistry` with `ServiceLoader` discovery), the JavaParser-based `Linter`
  and the console reporter.
- Rule TQL001 TautologicalAssertion: assertions that compare an expression with itself or can
  never fail.
- Rules TQL002 ConstantAssertion, TQL003 NoAssertion (options `methods`, `packages`),
  TQL006 HardcodedSleep (option `maxMillis`) and TQL007 SwallowedException.
- Optional symbol resolution: `Linter` accepts a classpath and resolves types with JavaParser's
  `JavaSymbolSolver` (JDK types resolve through reflection even with an empty classpath).
- Rules TQL004 AssertsOnStub, TQL005 VerifyOnly, TQL011 UnusedTestResult (needs symbol
  resolution) and TQL012 MockOfTypeUnderTest.
- Rules TQL008 DuplicateTestBody, TQL009 DisabledWithoutReason (option `maxAgeDays`, using
  `git blame` when available) and TQL010 ContradictoryMessage; every planned rule now exists.
- Suppressions: a trailing `// tql:ignore [rule-id...]` line comment and
  `@SuppressWarnings("tql:rule-id")` (or `"tql:*"`) on a method, constructor, class, interface,
  enum or field.
- `.tql.yaml` configuration loading (`RuleConfigLoader`): enable/disable, severity overrides,
  per-rule options and exclude globs.
- `JsonReporter`, `SarifReporter` (SARIF 2.1.0, with a `tool.driver.rules` array) and
  `MarkdownReporter`, alongside the existing `ConsoleReporter`.
- `tql-cli`: the `tql` command (`lint`, `rules`, `explain`), packaged as a shaded executable jar
  by `maven-shade-plugin`. `docs/ci-integration.md` documents uploading SARIF to GitHub code
  scanning.
- `tql-maven-plugin`: `tql:lint`, bound to the `verify` phase by default, scanning
  `src/test/java`, writing a SARIF report to `target/tql/tql.sarif` and failing the build on a
  finding at or above `failOnSeverity` (default `ERROR`) or an unparseable file.
- Multi-module Maven build (`tql-core`) with automatic formatting (Spotless,
  google-java-format), JaCoCo coverage gate at 85%, GitHub Actions CI and release workflows.

[Unreleased]: https://github.com/byreshb/test-quality-linter/compare/main...HEAD
