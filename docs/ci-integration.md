# CI integration

`tql lint --format sarif` produces SARIF 2.1.0, which GitHub reads natively as code scanning
results: findings show up as inline annotations on the diff of a pull request, and accumulate in
the repository's Security tab.

## GitHub Actions: the `action/` package (recommended)

```yaml
permissions:
  contents: read
  security-events: write

steps:
  - uses: actions/checkout@v4
  - uses: actions/setup-java@v4
    with: { distribution: temurin, java-version: '17' }
  - uses: byreshb/test-quality-linter/action@v1
    with:
      paths: src/test/java
      fail-on: error
```

One step: it downloads the matching `tql-cli` release jar, runs `tql lint --format sarif`,
uploads the SARIF straight to code scanning's REST API (the same endpoint
`github/codeql-action/upload-sarif` calls), writes a job summary with counts per rule, and fails
the job only when `fail-on` is set and a finding reaches it. See [action/README.md](../action/README.md)
for every input and output.

## GitHub Actions: two manual steps

Without the action above: run `tql`, then upload the SARIF file with
[`github/codeql-action/upload-sarif`](https://github.com/github/codeql-action/tree/main/upload-sarif).

```yaml
name: Lint tests

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

permissions:
  contents: read
  security-events: write   # required by upload-sarif

jobs:
  tql:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Download tql
        run: |
          version=1.0.0
          curl -fsSL -o tql.jar \
            "https://github.com/byreshb/test-quality-linter/releases/download/v${version}/tql-cli-${version}.jar"

      - name: Lint tests
        run: java -jar tql.jar lint src/test/java --format sarif > tql.sarif
        continue-on-error: true   # let the upload step run, and see step 8 for a fail-the-job option

      - uses: github/codeql-action/upload-sarif@v3
        with:
          sarif_file: tql.sarif
```

`continue-on-error: true` on the lint step means a finding does not fail the job by itself; the
SARIF file is uploaded either way, and GitHub still shows the findings on the pull request. Drop
it (and keep `tql`'s own `--fail-on` at its default of `ERROR`) to fail the job on an ERROR
finding, or pass `--fail-on WARN` to be stricter.

Once the TypeScript GitHub Action described in `CLAUDE.md`'s delivery plan exists (`uses:
byreshb/test-quality-linter/action@v1`), it wraps exactly this: download the matching jar, run
`tql lint --format sarif`, and call the upload API, in one step.

## Other CI systems

Any CI that can run a jar can run `tql`: download the release jar (or build it with `mvn
package -pl tql-cli -am -DskipTests`), run `tql lint <paths> --format json` or `--format md`, and
fail the build on a non-zero exit code (`tql`'s own default: non-zero when an ERROR finding is
present, or a file could not be parsed). `--format md` output is suited to posting as a merge
request comment on GitLab or Bitbucket, the same way `--format sarif` suits GitHub's own review
UI.
