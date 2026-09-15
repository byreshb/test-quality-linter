import { readFileSync, mkdtempSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { downloadTo, releaseJarUrl } from './download.js';

describe('releaseJarUrl', () => {
  it('builds the GitHub Release asset URL for a version', () => {
    expect(releaseJarUrl('1.0.0')).toBe(
      'https://github.com/byreshb/test-quality-linter/releases/download/v1.0.0/tql-cli-1.0.0.jar',
    );
  });
});

describe('downloadTo', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('writes the response body to the destination file', async () => {
    const body = new ReadableStream({
      start(controller) {
        controller.enqueue(new TextEncoder().encode('jar-bytes'));
        controller.close();
      },
    });
    const fetchMock = vi.fn().mockResolvedValue(new Response(body, { status: 200 }));
    vi.stubGlobal('fetch', fetchMock);

    const dest = join(mkdtempSync(join(tmpdir(), 'tql-dl-')), 'out.jar');
    await downloadTo('https://example.test/tql.jar', dest, 'a-token');

    expect(readFileSync(dest, 'utf8')).toBe('jar-bytes');
    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer a-token');
  });

  it('throws when the response is not ok', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 404 })));
    await expect(downloadTo('https://example.test/missing.jar', '/tmp/x')).rejects.toThrow(
      /HTTP 404/,
    );
  });
});
