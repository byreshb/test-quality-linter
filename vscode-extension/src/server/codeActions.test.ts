import { Diagnostic, DiagnosticSeverity } from 'vscode-languageserver/node';
import { describe, expect, it } from 'vitest';
import { buildSuppressActions } from './codeActions.js';
import { DIAGNOSTIC_SOURCE } from './diagnostics.js';

function tqlDiagnostic(ruleId: string, line = 4): Diagnostic {
  return {
    range: { start: { line, character: 2 }, end: { line, character: 20 } },
    severity: DiagnosticSeverity.Error,
    source: DIAGNOSTIC_SOURCE,
    code: ruleId,
    message: 'm',
  };
}

describe('buildSuppressActions', () => {
  it('appends a tql:ignore comment to the end of the diagnostic line', () => {
    const actions = buildSuppressActions(
      'file:///Foo.java',
      [tqlDiagnostic('TQL001')],
      '    assertEquals(x, x);',
    );
    expect(actions).toHaveLength(1);
    expect(actions[0]?.title).toBe('Suppress TQL001 with // tql:ignore');
    const edits = actions[0]?.edit?.changes?.['file:///Foo.java'];
    expect(edits).toEqual([
      {
        range: { start: { line: 4, character: 23 }, end: { line: 4, character: 23 } },
        newText: ' // tql:ignore TQL001',
      },
    ]);
  });

  it('omits the redundant // when the line already ends with one', () => {
    const actions = buildSuppressActions(
      'file:///Foo.java',
      [tqlDiagnostic('TQL006')],
      'Thread.sleep(50); //',
    );
    expect(actions[0]?.edit?.changes?.['file:///Foo.java']?.[0]?.newText).toBe(
      ' tql:ignore TQL006',
    );
  });

  it('ignores diagnostics not from tql or without a string code', () => {
    const other: Diagnostic = {
      range: { start: { line: 0, character: 0 }, end: { line: 0, character: 0 } },
      source: 'eslint',
      code: 'no-unused-vars',
      message: 'm',
    };
    const noCode: Diagnostic = {
      range: { start: { line: 0, character: 0 }, end: { line: 0, character: 0 } },
      source: DIAGNOSTIC_SOURCE,
      message: 'm',
    };
    expect(buildSuppressActions('file:///Foo.java', [other, noCode], '')).toEqual([]);
  });

  it('builds one action per suppressible diagnostic', () => {
    const actions = buildSuppressActions(
      'file:///Foo.java',
      [tqlDiagnostic('TQL001'), tqlDiagnostic('TQL003')],
      'x',
    );
    expect(actions.map((a) => a.title)).toEqual([
      'Suppress TQL001 with // tql:ignore',
      'Suppress TQL003 with // tql:ignore',
    ]);
  });
});
