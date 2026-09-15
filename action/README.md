# Test Quality Linter Action

Running [`tql`](https://github.com/byreshb/test-quality-linter) by hand in every pull request
review does not scale, and asking a human reviewer to spot a tautological assertion in a diff of
forty AI-generated tests does not work either. This action runs `tql` for you: it downloads the
matching release jar, lints your test sources, uploads the findings to GitHub code scanning as
SARIF (so they show up inline on the pull request diff, the same way CodeQL's do), writes a job
summary with counts per rule, and optionally fails the job when a finding reaches a severity you
choose.

## Usage

```yaml
name: Lint tests

on:
  pull_request:
  push:
    branches: [main]

permissions:
  contents: read
  security-events: write

jobs:
  tql:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - uses: byreshb/test-quality-linter/action@v1
        with:
          paths: src/test/java
          fail-on: error
```

Requires Java on the runner (`actions/setup-java` above); the action checks with `java -version`
and fails with a clear message if it is missing.

## Inputs

| Input     | Default                   | Meaning                                                                                                                                |
| --------- | ------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| `paths`   | `src/test/java`           | Space-separated files or directories to lint, relative to the workspace.                                                               |
| `config`  | (none)                    | Path to a `.tql.yaml` configuration file.                                                                                              |
| `fail-on` | (none)                    | `info`, `warn` or `error`: fail the job when a finding reaches this severity. Leave unset to never fail the job, whatever `tql` finds. |
| `version` | this action's own version | Which `tql` release to download, e.g. `1.0.0`.                                                                                         |
| `token`   | `${{ github.token }}`     | Used to download the release jar and to upload the SARIF report (needs `security-events: write` to upload).                            |

## Outputs

| Output     | Meaning                                                                                         |
| ---------- | ----------------------------------------------------------------------------------------------- |
| `findings` | Total number of findings.                                                                       |
| `sarif-id` | The id GitHub's code scanning API assigned to the uploaded analysis, when the upload succeeded. |

## How it works

The action calls the same `code-scanning/sarifs` REST endpoint
[`github/codeql-action/upload-sarif`](https://github.com/github/codeql-action/tree/main/upload-sarif)
does, gzip-compressed and base64-encoded as that API requires, rather than running that action as
a second step — so a workflow needs only this one action. The counts in the job summary and the
`fail-on` decision are both computed from the same SARIF the upload sends, not from `tql`'s own
process exit code, so the job fails only when `fail-on` is set and only at the threshold you
chose.

## Development

```bash
npm install
npm run check   # tsc --noEmit, eslint, prettier --check
npm test        # vitest, with an 85% line coverage threshold
npm run build   # bundles src/index.ts into dist/index.js with @vercel/ncc
```

`dist/` is committed, because consumers run the action straight from this repository's ref
without a build step; CI fails if `npm run build` would change it (`git diff --exit-code dist/`).
Publishing to the GitHub Marketplace is planned but not done yet.
