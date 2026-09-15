/** Runs `tql lint --format json` against one file and parses its output. */

import { spawn } from 'node:child_process';

/** One finding, as `tql --format json` reports it. */
export interface TqlFinding {
  readonly ruleId: string;
  readonly severity: 'INFO' | 'WARN' | 'ERROR';
  readonly file: string;
  readonly line: number;
  readonly column: number;
  readonly message: string;
  readonly fixHint: string;
  readonly snippet: string | null;
}

/** `tql --format json`'s top-level shape. */
export interface TqlResult {
  readonly filesScanned: number;
  readonly findings: readonly TqlFinding[];
  readonly problems: readonly { file: string; line: number; message: string }[];
}

/**
 * Builds the `java -jar <jar> lint <file> --format json` argument list.
 *
 * @param jarPath - path to the `tql-cli` jar
 * @param filePath - the file to lint
 * @returns the arguments for `spawn('java', args)`
 */
export function buildArgs(jarPath: string, filePath: string): string[] {
  return ['-jar', jarPath, 'lint', filePath, '--format', 'json'];
}

/**
 * Parses `tql --format json`'s stdout.
 *
 * @param stdout - the process's standard output
 * @returns the parsed result
 * @throws {Error} when the text is not valid JSON shaped like a tql result
 */
export function parseResult(stdout: string): TqlResult {
  let parsed: unknown;
  try {
    parsed = JSON.parse(stdout);
  } catch (cause) {
    throw new Error('tql did not produce valid JSON', { cause });
  }
  if (typeof parsed !== 'object' || parsed === null || !('findings' in parsed)) {
    throw new Error('Not a tql result: missing "findings"');
  }
  const findings: unknown = parsed.findings;
  if (!Array.isArray(findings)) {
    throw new Error('Not a tql result: missing "findings"');
  }
  const filesScanned: unknown = 'filesScanned' in parsed ? parsed.filesScanned : undefined;
  const problems: unknown = 'problems' in parsed ? parsed.problems : undefined;
  return {
    filesScanned: typeof filesScanned === 'number' ? filesScanned : 0,
    findings: findings as TqlFinding[],
    problems: Array.isArray(problems) ? (problems as TqlResult['problems']) : [],
  };
}

/**
 * Runs `tql lint <filePath> --format json` and parses its output.
 *
 * @param javaPath - the `java` executable
 * @param jarPath - path to the `tql-cli` jar
 * @param filePath - the file to lint
 * @returns the parsed result
 */
export function lintFile(javaPath: string, jarPath: string, filePath: string): Promise<TqlResult> {
  return new Promise((resolve, reject) => {
    const child = spawn(javaPath, buildArgs(jarPath, filePath));
    let stdout = '';
    let stderr = '';
    child.stdout.on('data', (chunk: Buffer) => {
      stdout += chunk.toString('utf8');
    });
    child.stderr.on('data', (chunk: Buffer) => {
      stderr += chunk.toString('utf8');
    });
    child.on('error', reject);
    child.on('close', () => {
      if (!stdout.trim()) {
        reject(new Error(`tql produced no output.\n${stderr}`));
        return;
      }
      try {
        resolve(parseResult(stdout));
      } catch (error) {
        reject(error instanceof Error ? error : new Error(String(error)));
      }
    });
  });
}
