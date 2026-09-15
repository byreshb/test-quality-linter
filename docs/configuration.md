# Configuration

`tql` runs with sensible defaults: every built-in rule enabled at its own default severity, no
files excluded. A `.tql.yaml` file changes that.

## `.tql.yaml`

```yaml
rules:
  TQL001:
    enabled: false          # turn a rule off entirely

  TQL003:
    severity: ERROR         # override the severity of a rule's findings
    options:                # per-rule options; see the rule's own docs page
      methods: [ensureSaved, mustContain]
      packages: [com.acme.testing]

  TQL006:
    options:
      maxMillis: 50

  TQL009:
    options:
      maxAgeDays: 90

exclude:
  - "**/generated/**"
  - "**/legacy/**"
```

Every key is optional. A rule not mentioned under `rules` keeps its default severity and stays
enabled. `severity` accepts `INFO`, `WARN` or `ERROR` (case-insensitive; `WARNING` and
`INFORMATION` are accepted as aliases). `exclude` is a list of glob patterns matched against
each file's path as given to the linter, and against the file name alone, so both
`**/generated/**` (a path segment anywhere) and `GeneratedTest.java` (a file name) work.

An option's value type depends on the option: `methods` and `packages` are lists, `maxMillis`
and `maxAgeDays` are integers. The rule's own docs page under [docs/rules](rules/README.md)
lists its options and their defaults.

Loading is programmatic today, through `tql-core`:

```java
RuleConfig config = RuleConfigLoader.load(Path.of(".tql.yaml"));
Linter linter = new Linter(RuleRegistry.discover(), config);
```

The `tql-cli` and `tql-maven-plugin` modules (a later increment) both pass a `--config` /
`<config>` path through to `RuleConfigLoader.load`, defaulting to `.tql.yaml` in the working
directory when present.

A malformed file (the wrong shape, not YAML at all) raises an `IllegalArgumentException` rather
than being silently ignored, so a typo in the configuration is caught immediately instead of
quietly linting with the wrong rules turned on.

## Suppressing one finding

Two ways to suppress a finding without changing the configuration, both matched by rule id:

**A line comment**, for one line:

```java
assertEquals(x, x); // tql:ignore TQL001
assertEquals(x, x); // tql:ignore                  // suppresses every rule on this line
assertEquals(x, x); // tql:ignore TQL001, TQL002    // several ids, comma or space separated
```

**`@SuppressWarnings`**, for a whole method, constructor, class, interface, enum or field,
coexisting with ordinary Java suppress-warnings values:

```java
// not compiled
@SuppressWarnings("tql:TQL001")
@Test
void intentionallyTautological() { ... }

@SuppressWarnings({"unchecked", "tql:TQL001", "tql:TQL002"})
@Test
void suppressesTwoRules() { ... }

@SuppressWarnings("tql:*")   // suppresses every rule inside this declaration
class LegacyTest { ... }
```

A suppressed finding never appears in the result, in any output format.
