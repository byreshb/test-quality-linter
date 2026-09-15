import { chmodSync, mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { buildArgs, lintFile, parseResult } from './jarRunner.js';

describe('buildArgs', () => {
  it('builds the java -jar tql lint --format json argument list', () => {
    expect(buildArgs('/tmp/tql.jar', '/repo/FooTest.java')).toEqual([
      '-jar',
      '/tmp/tql.jar',
      'lint',
      '/repo/FooTest.java',
      '--format',
      'json',
    ]);
  });
});

const SAMPLE_JSON = JSON.stringify({
  filesScanned: 1,
  findings: [
    {
      ruleId: 'TQL001',
      severity: 'ERROR',
      file: '/repo/FooTest.java',
      line: 5,
      column: 3,
      message: 'assertEquals compares x with itself',
      fixHint: 'Assert on the real outcome',
      snippet: 'assertEquals(x, x);',
    },
  ],
  problems: [],
});

describe('parseResult', () => {
  it('parses a well-formed tql JSON result', () => {
    const result = parseResult(SAMPLE_JSON);
    expect(result.filesScanned).toBe(1);
    expect(result.findings).toHaveLength(1);
    expect(result.findings[0]?.ruleId).toBe('TQL001');
    expect(result.problems).toEqual([]);
  });

  it('defaults missing optional fields', () => {
    const result = parseResult(JSON.stringify({ findings: [] }));
    expect(result.filesScanned).toBe(0);
    expect(result.problems).toEqual([]);
  });

  it('rejects invalid JSON', () => {
    expect(() => parseResult('not json')).toThrowError(/valid JSON/);
  });

  it('rejects JSON that is not shaped like a tql result', () => {
    expect(() => parseResult(JSON.stringify({ foo: 'bar' }))).toThrowError(/Not a tql result/);
  });
});

/** Writes an executable stand-in for `java` that ignores its arguments and prints `output`. */
function fakeJava(output: string): string {
  const dir = mkdtempSync(join(tmpdir(), 'tql-vscode-fakejava-'));
  const script = join(dir, 'java');
  writeFileSync(script, `#!/bin/sh\ncat <<'JSON'\n${output}\nJSON\n`);
  chmodSync(script, 0o755);
  return script;
}

describe('lintFile', () => {
  it('runs the command and parses its stdout as a tql result', async () => {
    const result = await lintFile(fakeJava(SAMPLE_JSON), '/tmp/tql.jar', '/repo/FooTest.java');
    expect(result.findings[0]?.ruleId).toBe('TQL001');
  });

  it('rejects when the command produces no output', async () => {
    await expect(lintFile(fakeJava(''), '/tmp/tql.jar', '/repo/FooTest.java')).rejects.toThrow(
      /no output/,
    );
  });

  it('rejects when the output is not valid tql JSON', async () => {
    await expect(
      lintFile(fakeJava('not json'), '/tmp/tql.jar', '/repo/FooTest.java'),
    ).rejects.toThrow(/valid JSON/);
  });

  it('rejects when the command cannot be started', async () => {
    await expect(
      lintFile('no-such-java-xyz', '/tmp/tql.jar', '/repo/FooTest.java'),
    ).rejects.toThrow();
  });
});
