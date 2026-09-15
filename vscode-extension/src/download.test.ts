import { readFileSync, mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cachedJarPath, ensureDownloaded, releaseJarUrl } from './download.js';

describe('releaseJarUrl', () => {
  it('builds the GitHub Release asset URL for a version', () => {
    expect(releaseJarUrl('1.0.0')).toBe(
      'https://github.com/byreshb/test-quality-linter/releases/download/v1.0.0/tql-cli-1.0.0.jar',
    );
  });
});

describe('cachedJarPath', () => {
  it('names the cached jar after its version, under the storage directory', () => {
    expect(cachedJarPath('/storage', '1.0.0')).toBe('/storage/tql-cli-1.0.0.jar');
  });
});

describe('ensureDownloaded', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('downloads and writes the jar when it is not already cached', async () => {
    const body = new ReadableStream({
      start(controller) {
        controller.enqueue(new TextEncoder().encode('jar-bytes'));
        controller.close();
      },
    });
    const fetchMock = vi.fn().mockResolvedValue(new Response(body, { status: 200 }));
    vi.stubGlobal('fetch', fetchMock);

    const dest = join(mkdtempSync(join(tmpdir(), 'tql-vscode-dl-')), 'nested', 'tql.jar');
    await ensureDownloaded('https://example.test/tql.jar', dest);

    expect(readFileSync(dest, 'utf8')).toBe('jar-bytes');
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('skips the download when the file already exists', async () => {
    const dir = mkdtempSync(join(tmpdir(), 'tql-vscode-dl-'));
    const dest = join(dir, 'tql.jar');
    writeFileSync(dest, 'already here');
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);

    await ensureDownloaded('https://example.test/tql.jar', dest);

    expect(fetchMock).not.toHaveBeenCalled();
    expect(readFileSync(dest, 'utf8')).toBe('already here');
  });

  it('throws when the response is not ok', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 404 })));
    const dest = join(mkdtempSync(join(tmpdir(), 'tql-vscode-dl-')), 'tql.jar');
    await expect(ensureDownloaded('https://example.test/missing.jar', dest)).rejects.toThrow(
      /HTTP 404/,
    );
  });
});
