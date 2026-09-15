/**
 * Launches a real VS Code instance and runs `test/suite/**` against it via `@vscode/test-electron`
 * (per this repository's house rule of testing the extension with the real thing, not a mock of
 * VS Code's API). Compiled to `dist-test/` and run with `npm run test:extension`.
 */

import { runTests } from '@vscode/test-electron';
import { mkdtempSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

async function main(): Promise<void> {
  // This file compiles to dist-test/test/runTest.js, so two levels up is the extension root
  // (vscode-extension/, where package.json and the built dist/ live).
  const extensionDevelopmentPath = fileURLToPath(new URL('../..', import.meta.url));
  const extensionTestsPath = fileURLToPath(new URL('./suite/index.js', import.meta.url));

  // A short --user-data-dir outside the repository: this project's own path is deep enough that
  // VS Code's default (nested under the repo) produces a Unix socket path over the platform's
  // ~103-character limit.
  const userDataDir = mkdtempSync(join(tmpdir(), 'tql-vscode-test-'));

  await runTests({
    extensionDevelopmentPath,
    extensionTestsPath,
    launchArgs: [
      '--user-data-dir',
      userDataDir,
      // CI runners disable the namespaces Chromium's sandbox needs, and have no real GPU.
      '--no-sandbox',
      '--disable-gpu',
    ],
  });
}

main().catch((error: unknown) => {
  console.error('Failed to run extension tests', error);
  process.exitCode = 1;
});
