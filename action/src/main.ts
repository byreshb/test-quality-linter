/**
 * Orchestrates the action: download the jar, run `tql lint`, summarise, upload, decide whether to
 * fail. Deliberately thin — the logic worth testing lives in the other modules, each covered on
 * its own; this file is glue and is excluded from the coverage threshold, the same way
 * `tql-cli`'s `Main.main` is.
 */

import { mkdtemp, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { appendSummary, getInput, setFailed, setOutput, warning } from './actionsEnv.js';
import { downloadTo, releaseJarUrl } from './download.js';
import { countBySeverity, parseSarifFindings } from './sarif.js';
import { isAtLeast, maxSeverity, parseSeverity } from './severity.js';
import { renderSummary } from './summary.js';
import { buildLintArgs, isJavaAvailable, run } from './tql.js';
import { buildSarifPayload, uploadSarif } from './upload.js';
import pkg from '../package.json' with { type: 'json' };

/** Runs the action end to end. */
export async function main(): Promise<void> {
  const paths = getInput('paths') || 'src/test/java';
  const config = getInput('config') || undefined;
  const failOnRaw = getInput('fail-on');
  const version = getInput('version') || (pkg as { version: string }).version;
  const token = getInput('token') || process.env.GITHUB_TOKEN || '';

  if (!(await isJavaAvailable())) {
    setFailed('java is not available on this runner. Add actions/setup-java before this step.');
    return;
  }

  const workDir = await mkdtemp(join(tmpdir(), 'tql-action-'));
  try {
    const jarPath = join(workDir, 'tql-cli.jar');
    await downloadTo(releaseJarUrl(version), jarPath, token || undefined);

    const args = buildLintArgs({ jarPath, paths, config });
    const result = await run('java', args);

    if (!result.stdout.trim()) {
      setFailed(`tql produced no output.\n${result.stderr}`);
      return;
    }
    const findings = parseSarifFindings(result.stdout);

    appendSummary(renderSummary(findings));
    const counts = countBySeverity(findings);
    setOutput('findings', String(findings.length));

    const repository = process.env.GITHUB_REPOSITORY;
    const sha = process.env.GITHUB_SHA;
    const ref = process.env.GITHUB_REF;
    const apiUrl = process.env.GITHUB_API_URL ?? 'https://api.github.com';
    if (token && repository && sha && ref) {
      try {
        const payload = buildSarifPayload(result.stdout, sha, ref);
        const uploaded = await uploadSarif(apiUrl, repository, payload, token);
        if (uploaded.id) {
          setOutput('sarif-id', uploaded.id);
        }
      } catch (error) {
        warning(`Could not upload SARIF to code scanning: ${(error as Error).message}`);
      }
    } else {
      warning('Skipping SARIF upload: missing a token or GitHub context environment variables.');
    }

    console.log(
      `tql found ${findings.length} finding(s): ` +
        `${counts.ERROR} error(s), ${counts.WARN} warning(s), ${counts.INFO} info.`,
    );

    if (failOnRaw) {
      const threshold = parseSeverity(failOnRaw);
      const worst = maxSeverity(findings.map((f) => f.severity));
      if (worst && isAtLeast(worst, threshold)) {
        setFailed(
          `tql found a finding at or above "${threshold}" ` +
            `(${counts.ERROR} error(s), ${counts.WARN} warning(s), ${counts.INFO} info).`,
        );
      }
    }
  } finally {
    await rm(workDir, { recursive: true, force: true });
  }
}
