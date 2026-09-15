import { DiagnosticSeverity } from 'vscode-languageserver/node';
import { describe, expect, it } from 'vitest';
import type { TqlFinding } from './jarRunner.js';
import { DIAGNOSTIC_SOURCE, toDiagnostic } from './diagnostics.js';

function finding(overrides: Partial<TqlFinding> = {}): TqlFinding {
  return {
    ruleId: 'TQL001',
    severity: 'ERROR',
    file: '/repo/FooTest.java',
    line: 5,
    column: 3,
    message: 'assertEquals compares x with itself',
    fixHint: 'Assert on the real outcome',
    snippet: 'assertEquals(x, x);',
    ...overrides,
  };
}

describe('toDiagnostic', () => {
  it('converts 1-based line/column to a 0-based range underlining to the end of the line', () => {
    const diagnostic = toDiagnostic(finding(), 25);
    expect(diagnostic.range).toEqual({
      start: { line: 4, character: 2 },
      end: { line: 4, character: 25 },
    });
    expect(diagnostic.source).toBe(DIAGNOSTIC_SOURCE);
    expect(diagnostic.code).toBe('TQL001');
    expect(diagnostic.message).toBe(
      'assertEquals compares x with itself (fix: Assert on the real outcome)',
    );
  });

  it('maps severities', () => {
    expect(toDiagnostic(finding({ severity: 'ERROR' }), 10).severity).toBe(
      DiagnosticSeverity.Error,
    );
    expect(toDiagnostic(finding({ severity: 'WARN' }), 10).severity).toBe(
      DiagnosticSeverity.Warning,
    );
    expect(toDiagnostic(finding({ severity: 'INFO' }), 10).severity).toBe(
      DiagnosticSeverity.Information,
    );
  });

  it('clamps positions and end column when the line is unexpectedly short', () => {
    const diagnostic = toDiagnostic(finding({ line: 1, column: 1 }), 0);
    expect(diagnostic.range).toEqual({
      start: { line: 0, character: 0 },
      end: { line: 0, character: 0 },
    });
  });
});
