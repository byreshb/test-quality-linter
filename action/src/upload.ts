/**
 * Uploads a SARIF report to GitHub's code scanning API directly (the same endpoint
 * `github/codeql-action/upload-sarif` calls), so this action needs no other action as a step.
 *
 * @see https://docs.github.com/en/rest/code-scanning/code-scanning#upload-an-analysis-as-sarif-data
 */

import { gzipSync } from 'node:zlib';

/** The request body GitHub's "upload SARIF" endpoint expects. */
export interface SarifUploadPayload {
  readonly commit_sha: string;
  readonly ref: string;
  readonly sarif: string;
  readonly tool_name: string;
}

/**
 * Builds the upload payload: the SARIF text, gzip-compressed and base64-encoded, as the API
 * requires.
 *
 * @param sarifText - the SARIF document
 * @param commitSha - the commit the analysis is for (`GITHUB_SHA`)
 * @param ref - the ref the analysis is for (`GITHUB_REF`)
 * @returns the request body
 */
export function buildSarifPayload(
  sarifText: string,
  commitSha: string,
  ref: string,
): SarifUploadPayload {
  const compressed = gzipSync(Buffer.from(sarifText, 'utf8'));
  return {
    commit_sha: commitSha,
    ref,
    sarif: compressed.toString('base64'),
    tool_name: 'tql',
  };
}

/** What the API returns; the interesting part is `id`, used to look up the analysis later. */
export interface SarifUploadResult {
  readonly id?: string;
}

/**
 * Uploads a SARIF payload.
 *
 * @param apiUrl - the GitHub API base URL (`GITHUB_API_URL`, e.g. `https://api.github.com`)
 * @param repository - `owner/repo` (`GITHUB_REPOSITORY`)
 * @param payload - the payload from {@link buildSarifPayload}
 * @param token - a token with `security_events: write`
 * @returns the parsed response body
 * @throws {Error} when the API responds with a non-2xx status
 */
export async function uploadSarif(
  apiUrl: string,
  repository: string,
  payload: SarifUploadPayload,
  token: string,
): Promise<SarifUploadResult> {
  const response = await fetch(`${apiUrl}/repos/${repository}/code-scanning/sarifs`, {
    method: 'POST',
    headers: {
      Accept: 'application/vnd.github+json',
      Authorization: `Bearer ${token}`,
      'X-GitHub-Api-Version': '2022-11-28',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });
  const body: unknown = await response.json().catch(() => undefined);
  if (!response.ok) {
    const message = extractMessage(body);
    throw new Error(`SARIF upload failed: HTTP ${response.status}${message ? ` ${message}` : ''}`);
  }
  return isSarifUploadResult(body) ? body : {};
}

function extractMessage(body: unknown): string | undefined {
  if (
    body !== null &&
    typeof body === 'object' &&
    'message' in body &&
    typeof body.message === 'string'
  ) {
    return body.message;
  }
  return undefined;
}

function isSarifUploadResult(body: unknown): body is SarifUploadResult {
  return body !== null && typeof body === 'object';
}
