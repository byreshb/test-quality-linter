/** Maps tql findings to LSP diagnostics. */

import { Diagnostic, DiagnosticSeverity } from 'vscode-languageserver/node';
import type { TqlFinding } from './jarRunner.js';

/** The diagnostic source every tql diagnostic carries, so a code action provider can find them. */
export const DIAGNOSTIC_SOURCE = 'tql';

const SEVERITY: Record<TqlFinding['severity'], DiagnosticSeverity> = {
  ERROR: DiagnosticSeverity.Error,
  WARN: DiagnosticSeverity.Warning,
  INFO: DiagnosticSeverity.Information,
};

/**
 * Converts one finding into an LSP diagnostic. tql reports 1-based line and column; LSP wants
 * 0-based, and a diagnostic needs an end position, so this underlines from the column tql
 * reported to the end of that line.
 *
 * @param finding - the finding to convert
 * @param lineLength - the length of the finding's source line, for the underline's end column
 * @returns the diagnostic, with `finding.ruleId` as its `code` and `finding.fixHint` appended to
 *   the message
 */
export function toDiagnostic(finding: TqlFinding, lineLength: number): Diagnostic {
  const line = Math.max(0, finding.line - 1);
  const startColumn = Math.max(0, finding.column - 1);
  return {
    range: {
      start: { line, character: startColumn },
      end: { line, character: Math.max(startColumn, lineLength) },
    },
    severity: SEVERITY[finding.severity],
    source: DIAGNOSTIC_SOURCE,
    code: finding.ruleId,
    message: `${finding.message} (fix: ${finding.fixHint})`,
  };
}
