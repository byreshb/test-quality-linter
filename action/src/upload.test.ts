import { gunzipSync } from 'node:zlib';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { buildSarifPayload, uploadSarif } from './upload.js';

describe('buildSarifPayload', () => {
  it('gzip-compresses and base64-encodes the SARIF text', () => {
    const payload = buildSarifPayload('{"a":1}', 'sha123', 'refs/heads/main');
    expect(payload.commit_sha).toBe('sha123');
    expect(payload.ref).toBe('refs/heads/main');
    expect(payload.tool_name).toBe('tql');
    const decompressed = gunzipSync(Buffer.from(payload.sarif, 'base64')).toString('utf8');
    expect(decompressed).toBe('{"a":1}');
  });
});

describe('uploadSarif', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('posts to the code-scanning/sarifs endpoint and returns the parsed body', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ id: 'abc123' }), {
        status: 202,
        headers: { 'content-type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    const payload = buildSarifPayload('{}', 'sha', 'refs/heads/main');
    const result = await uploadSarif('https://api.github.com', 'owner/repo', payload, 'tok');

    expect(result.id).toBe('abc123');
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe('https://api.github.com/repos/owner/repo/code-scanning/sarifs');
    expect(init.method).toBe('POST');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer tok');
  });

  it('throws with the API error message on a non-2xx response', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ message: 'Resource not accessible' }), {
          status: 403,
          headers: { 'content-type': 'application/json' },
        }),
      ),
    );
    const payload = buildSarifPayload('{}', 'sha', 'refs/heads/main');
    await expect(
      uploadSarif('https://api.github.com', 'owner/repo', payload, 'tok'),
    ).rejects.toThrow(/403.*Resource not accessible/);
  });
});
