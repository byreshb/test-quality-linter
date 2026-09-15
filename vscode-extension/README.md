# Test Quality Linter (VS Code)

Catching a tautological assertion or a `Thread.sleep` standing in for a wait is easy to miss in
review, especially across dozens of near-identical AI-generated tests. This extension runs
[`tql`](https://github.com/byreshb/test-quality-linter) on every Java test file you open or save
and shows its findings right where you're already looking: as inline diagnostics, with the rule
id, the severity, and what to change.

## Features

- Lints on save and on open for Java test files (files under a `test` source root, or named like
  `FooTest.java`, `TestFoo.java`, `FooTests.java`, `FooIT.java`).
- Inline diagnostics carry the rule id, severity and a fix hint.
- A quick fix inserts `// tql:ignore <rule-id>` to suppress a finding on its line.
- **TQL: Explain rule** (Command Palette) opens a rule's docs page for a rule id you type in.

## Requirements

Java 17 or newer on your PATH (or configure `tql.jarPath` if `java` itself needs a different
setup).

## Settings

| Setting       | Default | Meaning                                                                                                                              |
| ------------- | ------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| `tql.jarPath` | `""`    | Path to a `tql-cli` jar. Leave empty to download and cache the matching release jar on first use, in the extension's global storage. |
| `tql.version` | `""`    | Which `tql` release to download when `tql.jarPath` is empty. Defaults to this extension's own version.                               |

## Architecture

Structured as a language server (`vscode-languageserver`) rather than logic embedded in the
extension, so any LSP-capable editor can run the same server, not only VS Code. The extension
(`src/extension.ts`) is a thin client: it resolves or downloads the jar, launches the server as a
child process over IPC, and registers the **TQL: Explain rule** command. The server
(`src/server/server.ts`) does the actual work: on open and on save, for files
`src/testFile.ts` recognises as tests, it spawns `java -jar <jar> lint <file> --format json`,
maps the findings to LSP diagnostics, and answers `textDocument/codeAction` requests with the
`// tql:ignore` quick fix.

## Development

```bash
npm install
npm run check          # tsc --noEmit, eslint, prettier --check
npm run test:unit       # Vitest over the server/client logic, 85% line coverage threshold
npm run test:extension  # @vscode/test-electron: a real VS Code instance runs test/suite/*.test.ts
npm run build            # bundles src/extension.ts and src/server/server.ts with @vercel/ncc
npm run package           # builds, then packages dist/ into tql-vscode.vsix with @vscode/vsce
```

`dist/` is not committed (unlike the GitHub Action's, this package ships as a `.vsix`, not run
from the repository directly); it is built fresh by `npm run package` and by the release
workflow, which attaches the `.vsix` to each GitHub Release.

## Status

Publishing to the VS Code Marketplace is free but **planned, not done yet**. Install the `.vsix`
from a [GitHub Release](https://github.com/byreshb/test-quality-linter/releases) with **Extensions
→ … → Install from VSIX…**, or build one yourself with `npm run package` above.
