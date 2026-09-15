/**
 * The small slice of the GitHub Actions "toolkit" this action needs, reimplemented directly over
 * `node:process` and `node:fs` (the environment variables and files the Actions runner itself
 * sets and reads) instead of depending on `@actions/core`, per this repository's house rule of
 * preferring the Node standard library over packages for a surface this small.
 */

import { appendFileSync } from 'node:fs';

/**
 * Reads an action input, matching the Actions runner's own env var convention
 * (`INPUT_<NAME>`, spaces to underscores, upper-cased; hyphens are left as-is).
 *
 * @param name - the input's name as declared in `action.yml`, e.g. `"fail-on"`
 * @param env - the environment to read from (defaults to `process.env`; overridable for tests)
 * @returns the trimmed value, or an empty string when unset
 */
export function getInput(name: string, env: NodeJS.ProcessEnv = process.env): string {
  const key = `INPUT_${name.replace(/ /g, '_').toUpperCase()}`;
  return (env[key] ?? '').trim();
}

/**
 * Writes one `key=value` output line to the file named by `GITHUB_OUTPUT`. A no-op (with a
 * console warning) when that variable is not set, which happens when running outside Actions.
 *
 * @param key - the output's name
 * @param value - the output's value; must not contain a newline
 * @param env - the environment to read `GITHUB_OUTPUT` from
 */
export function setOutput(key: string, value: string, env: NodeJS.ProcessEnv = process.env): void {
  const path = env.GITHUB_OUTPUT;
  if (!path) {
    console.warn(`GITHUB_OUTPUT is not set; would have written ${key}=${value}`);
    return;
  }
  appendFileSync(path, `${key}=${value}\n`, 'utf8');
}

/**
 * Appends Markdown to the job's step summary (the file named by `GITHUB_STEP_SUMMARY`). A no-op
 * (with a console warning) when that variable is not set.
 *
 * @param markdown - the Markdown to append
 * @param env - the environment to read `GITHUB_STEP_SUMMARY` from
 */
export function appendSummary(markdown: string, env: NodeJS.ProcessEnv = process.env): void {
  const path = env.GITHUB_STEP_SUMMARY;
  if (!path) {
    console.warn('GITHUB_STEP_SUMMARY is not set; summary was not written');
    return;
  }
  appendFileSync(path, markdown, 'utf8');
}

/**
 * Prints a warning annotation, in the form the Actions runner recognises and surfaces in the UI.
 *
 * @param message - the warning text
 */
export function warning(message: string): void {
  console.log(`::warning::${message}`);
}

/**
 * Prints an error annotation and marks the step (and so the job) as failed by setting a non-zero
 * exit code, without terminating the process immediately.
 *
 * @param message - the error text
 */
export function setFailed(message: string): void {
  console.log(`::error::${message}`);
  process.exitCode = 1;
}
