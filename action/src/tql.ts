/** Builds the `tql lint` command line and runs it. */

import { spawn } from 'node:child_process';

/** The inputs that shape the `tql lint` invocation. */
export interface LintInputs {
  readonly jarPath: string;
  readonly paths: string;
  readonly config: string | undefined;
}

/**
 * Builds the argument list for `java -jar <jar> lint ...`.
 *
 * @param inputs - the jar path, the paths to lint (space-separated), and an optional config file
 * @returns the arguments, ready for `spawn('java', args)`
 * @throws {Error} when `paths` has no entries
 */
export function buildLintArgs(inputs: LintInputs): string[] {
  const paths = inputs.paths
    .split(/\s+/)
    .map((p) => p.trim())
    .filter((p) => p.length > 0);
  if (paths.length === 0) {
    throw new Error('The "paths" input must name at least one file or directory');
  }
  const args = ['-jar', inputs.jarPath, 'lint', ...paths];
  if (inputs.config) {
    args.push('--config', inputs.config);
  }
  args.push('--format', 'sarif');
  return args;
}

/** The outcome of running a command: its combined stdout and its exit code. */
export interface CommandResult {
  readonly stdout: string;
  readonly stderr: string;
  readonly exitCode: number;
}

/**
 * Runs a command, collecting its output. Never rejects on a non-zero exit code (tql's own exit
 * code reflects `--fail-on`, which this action decides independently from the SARIF content); it
 * rejects only when the command cannot be started at all (e.g. `java` is not on the PATH).
 *
 * @param command - the executable to run
 * @param args - its arguments
 * @returns the command's stdout, stderr and exit code
 */
export function run(command: string, args: readonly string[]): Promise<CommandResult> {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args);
    let stdout = '';
    let stderr = '';
    child.stdout.on('data', (chunk: Buffer) => {
      stdout += chunk.toString('utf8');
    });
    child.stderr.on('data', (chunk: Buffer) => {
      stderr += chunk.toString('utf8');
    });
    child.on('error', reject);
    child.on('close', (code) => {
      resolve({ stdout, stderr, exitCode: code ?? 1 });
    });
  });
}

/**
 * Whether `java` is on the PATH and runs.
 *
 * @returns true when `java -version` could be launched, whatever it prints
 */
export async function isJavaAvailable(): Promise<boolean> {
  try {
    await run('java', ['-version']);
    return true;
  } catch {
    return false;
  }
}
