import { describe, expect, it } from 'vitest';
import { buildLintArgs, isJavaAvailable, run } from './tql.js';

describe('buildLintArgs', () => {
  it('builds the java -jar tql lint command line', () => {
    expect(
      buildLintArgs({ jarPath: '/tmp/tql.jar', paths: 'src/test/java', config: undefined }),
    ).toEqual(['-jar', '/tmp/tql.jar', 'lint', 'src/test/java', '--format', 'sarif']);
  });

  it('splits multiple space-separated paths', () => {
    expect(buildLintArgs({ jarPath: 'j', paths: 'a  b   c', config: undefined })).toEqual([
      '-jar',
      'j',
      'lint',
      'a',
      'b',
      'c',
      '--format',
      'sarif',
    ]);
  });

  it('adds --config when given', () => {
    expect(buildLintArgs({ jarPath: 'j', paths: 'a', config: '.tql.yaml' })).toEqual([
      '-jar',
      'j',
      'lint',
      'a',
      '--config',
      '.tql.yaml',
      '--format',
      'sarif',
    ]);
  });

  it('rejects an empty paths input', () => {
    expect(() => buildLintArgs({ jarPath: 'j', paths: '   ', config: undefined })).toThrowError(
      /at least one/,
    );
  });
});

describe('run', () => {
  it('collects stdout, stderr and the exit code without rejecting on non-zero', async () => {
    const result = await run(process.execPath, ['-e', 'console.log("hi"); process.exit(3)']);
    expect(result.stdout.trim()).toBe('hi');
    expect(result.exitCode).toBe(3);
  });

  it('rejects when the command cannot be started', async () => {
    await expect(run('no-such-command-xyz', [])).rejects.toThrow();
  });
});

describe('isJavaAvailable', () => {
  it('returns a boolean without throwing', async () => {
    expect(typeof (await isJavaAvailable())).toBe('boolean');
  });
});
