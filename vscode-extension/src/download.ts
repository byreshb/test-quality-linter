/** Downloads and caches the `tql-cli` release jar the extension runs. */

import { createWriteStream, existsSync } from 'node:fs';
import { mkdir } from 'node:fs/promises';
import { dirname } from 'node:path';
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
 * The cached jar's path for a version, under the extension's global storage directory.
 *
 * @param storageDir - the extension's global storage directory
 * @param version - the release version
 * @returns the path the jar is (or will be) cached at
 */
export function cachedJarPath(storageDir: string, version: string): string {
  return `${storageDir}/tql-cli-${version}.jar`;
}

/**
 * Downloads a jar to `destPath` unless it is already there.
 *
 * @param url - the jar's download URL
 * @param destPath - where to cache it
 */
export async function ensureDownloaded(url: string, destPath: string): Promise<void> {
  if (existsSync(destPath)) {
    return;
  }
  await mkdir(dirname(destPath), { recursive: true });
  const response = await fetch(url, { headers: { Accept: 'application/octet-stream' } });
  if (!response.ok || !response.body) {
    throw new Error(`Failed to download ${url}: HTTP ${response.status}`);
  }
  const out = createWriteStream(destPath);
  await finished(Readable.fromWeb(response.body as never).pipe(out));
}
