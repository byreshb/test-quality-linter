/** Discovers and runs the Mocha suite under `test/suite/*.test.ts` (compiled to `.js`). */

import { glob } from 'glob';
import Mocha from 'mocha';
import { fileURLToPath } from 'node:url';

export async function run(): Promise<void> {
  const mocha = new Mocha({ ui: 'tdd', color: true, timeout: 20000 });
  const testsRoot = fileURLToPath(new URL('.', import.meta.url));

  const files = await glob('**/*.test.js', { cwd: testsRoot });
  for (const file of files) {
    mocha.addFile(`${testsRoot}${file}`);
  }

  await new Promise<void>((resolve, reject) => {
    mocha.run((failures) => {
      if (failures > 0) {
        reject(new Error(`${failures} test(s) failed.`));
      } else {
        resolve();
      }
    });
  });
}
