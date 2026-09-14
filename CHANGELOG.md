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
- Multi-module Maven build (`tql-core`) with automatic formatting (Spotless,
  google-java-format), JaCoCo coverage gate at 85%, GitHub Actions CI and release workflows.

[Unreleased]: https://github.com/byreshb/test-quality-linter/compare/main...HEAD
