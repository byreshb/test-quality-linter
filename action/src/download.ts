/** Downloads the `tql-cli` release jar for a given version. */

import { createWriteStream } from 'node:fs';
import { Readable } from 'node:stream';
import { finished } from 'node:stream/promises';

const REPO = 'byreshb/test-quality-linter';

/**
 * The GitHub Release asset URL for a version's `tql-cli` jar.
 *
 * @param version - the release version, without a leading `v`, e.g. `"1.0.0"`
 * @returns the download URL
 */
export function releaseJarUrl(version: string): string {
  return `https://github.com/${REPO}/releases/download/v${version}/tql-cli-${version}.jar`;
}

/**
 * Downloads a URL to a local file.
 *
 * @param url - the URL to fetch
 * @param destPath - where to write the response body
 * @param token - an optional bearer token, sent when the URL needs authorization
 * @throws {Error} when the response is not successful
 */
export async function downloadTo(url: string, destPath: string, token?: string): Promise<void> {
  const headers: Record<string, string> = { Accept: 'application/octet-stream' };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  const response = await fetch(url, { headers });
  if (!response.ok || !response.body) {
    throw new Error(`Failed to download ${url}: HTTP ${response.status}`);
  }
  const out = createWriteStream(destPath);
  await finished(Readable.fromWeb(response.body as never).pipe(out));
}
