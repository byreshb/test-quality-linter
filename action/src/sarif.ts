/**
 * Reads the handful of SARIF fields this action needs out of `tql lint --format sarif`'s output,
 * without a SARIF library: the format is fixed (produced by tql-core's own `SarifReporter`), so a
 * few property accesses are simpler and more transparent than a schema-validating parser.
 */

import { type Severity, severityFromSarifLevel } from './severity.js';

/** One finding, reduced to what this action reports. */
export interface SarifFinding {
  readonly ruleId: string;
  readonly severity: Severity;
}

/**
 * Parses SARIF text into its findings.
 *
 * @param sarifText - the SARIF document, as text
 * @returns the findings in `runs[0].results`, or an empty array when there are none
 * @throws {Error} when the text is not valid JSON or not shaped like a SARIF 2.1.0 document
 */
export function parseSarifFindings(sarifText: string): SarifFinding[] {
  let doc: unknown;
  try {
    doc = JSON.parse(sarifText);
  } catch (cause) {
    throw new Error('tql did not produce valid SARIF JSON', { cause });
  }
  const results = firstRunResults(doc);
  return results.map((result) => ({
    ruleId: readString(result, 'ruleId') ?? 'unknown',
    severity: severityFromSarifLevel(readString(result, 'level') ?? 'note'),
  }));
}

/** Counts of findings per rule id, sorted by rule id. */
export function countByRule(findings: readonly SarifFinding[]): Map<string, number> {
  const counts = new Map<string, number>();
  for (const finding of findings) {
    counts.set(finding.ruleId, (counts.get(finding.ruleId) ?? 0) + 1);
  }
  return new Map([...counts.entries()].sort(([a], [b]) => a.localeCompare(b)));
}

/** Counts of findings per severity, always including all three keys. */
export function countBySeverity(findings: readonly SarifFinding[]): Record<Severity, number> {
  const counts: Record<Severity, number> = { INFO: 0, WARN: 0, ERROR: 0 };
  for (const finding of findings) {
    counts[finding.severity] += 1;
  }
  return counts;
}

function firstRunResults(doc: unknown): unknown[] {
  if (typeof doc !== 'object' || doc === null || !('runs' in doc)) {
    throw new Error('Not a SARIF document: missing "runs"');
  }
  const runs: unknown = doc.runs;
  if (!Array.isArray(runs) || runs.length === 0) {
    return [];
  }
  const firstRun: unknown = runs[0];
  if (typeof firstRun !== 'object' || firstRun === null || !('results' in firstRun)) {
    return [];
  }
  const results: unknown = firstRun.results;
  return Array.isArray(results) ? (results as unknown[]) : [];
}

function readString(value: unknown, key: string): string | undefined {
  if (typeof value !== 'object' || value === null || !(key in value)) {
    return undefined;
  }
  const raw = (value as Record<string, unknown>)[key];
  return typeof raw === 'string' ? raw : undefined;
}
