import { mkdtempSync, readFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { appendSummary, getInput, setFailed, setOutput, warning } from './actionsEnv.js';

describe('getInput', () => {
  it('reads INPUT_<NAME> with spaces to underscores and hyphens kept, trimmed', () => {
    const env = { 'INPUT_FAIL-ON': '  warn  ', INPUT_MY_NAME: 'x' };
    expect(getInput('fail-on', env)).toBe('warn');
    expect(getInput('my name', env)).toBe('x');
  });

  it('returns an empty string when unset', () => {
    expect(getInput('missing', {})).toBe('');
  });
});

describe('setOutput and appendSummary', () => {
  it('append to the files named by GITHUB_OUTPUT and GITHUB_STEP_SUMMARY', () => {
    const dir = mkdtempSync(join(tmpdir(), 'tql-action-test-'));
    const outputFile = join(dir, 'output');
    const summaryFile = join(dir, 'summary');
    const env = { GITHUB_OUTPUT: outputFile, GITHUB_STEP_SUMMARY: summaryFile };

    setOutput('findings', '3', env);
    setOutput('sarif-id', 'abc', env);
    appendSummary('## Report\n', env);

    expect(readFileSync(outputFile, 'utf8')).toBe('findings=3\nsarif-id=abc\n');
    expect(readFileSync(summaryFile, 'utf8')).toBe('## Report\n');
  });

  it('warns instead of throwing when the env var is unset', () => {
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => undefined);
    setOutput('x', 'y', {});
    appendSummary('z', {});
    expect(warn).toHaveBeenCalledTimes(2);
    warn.mockRestore();
  });
});

describe('warning and setFailed', () => {
  afterEach(() => {
    process.exitCode = undefined;
  });

  it('prints an annotation and, for setFailed, sets a non-zero exit code', () => {
    const log = vi.spyOn(console, 'log').mockImplementation(() => undefined);
    warning('be careful');
    expect(log).toHaveBeenCalledWith('::warning::be careful');

    setFailed('boom');
    expect(log).toHaveBeenCalledWith('::error::boom');
    expect(process.exitCode).toBe(1);
    log.mockRestore();
  });
});
