/** Renders the job summary Markdown from a SARIF run's findings. */

import { countByRule, countBySeverity, type SarifFinding } from './sarif.js';

/**
 * Renders the summary.
 *
 * @param findings - every finding from the SARIF run
 * @returns Markdown suited to `GITHUB_STEP_SUMMARY`
 */
export function renderSummary(findings: readonly SarifFinding[]): string {
  const lines: string[] = ['## Test Quality Linter', ''];
  if (findings.length === 0) {
    lines.push('No findings.', '');
    return lines.join('\n');
  }

  const bySeverity = countBySeverity(findings);
  lines.push(
    `${findings.length} finding${findings.length === 1 ? '' : 's'} ` +
      `(${bySeverity.ERROR} error${bySeverity.ERROR === 1 ? '' : 's'}, ` +
      `${bySeverity.WARN} warning${bySeverity.WARN === 1 ? '' : 's'}, ` +
      `${bySeverity.INFO} info)`,
    '',
    '| Rule | Count |',
    '|---|---|',
  );
  for (const [ruleId, count] of countByRule(findings)) {
    lines.push(`| \`${ruleId}\` | ${count} |`);
  }
  lines.push('');
  return lines.join('\n');
}
