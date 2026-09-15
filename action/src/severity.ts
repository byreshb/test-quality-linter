/**
 * Severity as tql reports it (`INFO`/`WARN`/`ERROR`) and as SARIF reports it
 * (`note`/`warning`/`error`), plus the ordering used to compare a finding against a threshold.
 */

export type Severity = 'INFO' | 'WARN' | 'ERROR';

const ORDER: Record<Severity, number> = { INFO: 0, WARN: 1, ERROR: 2 };

/**
 * Maps a SARIF result level to a {@link Severity}. SARIF also allows `none`; it is treated as
 * `INFO` since tql never emits it.
 *
 * @param level - the SARIF `level` field of a result
 * @returns the corresponding severity
 */
export function severityFromSarifLevel(level: string): Severity {
  switch (level) {
    case 'error':
      return 'ERROR';
    case 'warning':
      return 'WARN';
    default:
      return 'INFO';
  }
}

/**
 * Parses a user-supplied severity name (the `fail-on` input), case-insensitively, accepting the
 * `warning` alias tql itself accepts.
 *
 * @param raw - the raw input text
 * @returns the parsed severity
 * @throws {Error} when the text is not a recognised severity
 */
export function parseSeverity(raw: string): Severity {
  const upper = raw.trim().toUpperCase();
  if (upper === 'WARNING') {
    return 'WARN';
  }
  if (upper === 'INFO' || upper === 'WARN' || upper === 'ERROR') {
    return upper;
  }
  throw new Error(`Not a severity: "${raw}" (expected info, warn or error)`);
}

/**
 * Whether `severity` is at least as serious as `threshold`.
 *
 * @param severity - the severity to check
 * @param threshold - the minimum severity that counts
 * @returns true when `severity` meets or exceeds `threshold`
 */
export function isAtLeast(severity: Severity, threshold: Severity): boolean {
  return ORDER[severity] >= ORDER[threshold];
}

/**
 * The most serious severity in a non-empty list, or `undefined` for an empty one.
 *
 * @param severities - the severities to compare
 * @returns the maximum, or undefined when the list is empty
 */
export function maxSeverity(severities: readonly Severity[]): Severity | undefined {
  return severities.reduce<Severity | undefined>((max, current) => {
    if (max === undefined || ORDER[current] > ORDER[max]) {
      return current;
    }
    return max;
  }, undefined);
}
