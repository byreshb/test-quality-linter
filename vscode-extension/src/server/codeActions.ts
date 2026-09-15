/** Builds the "suppress with // tql:ignore" code action for a tql diagnostic. */

import { CodeAction, CodeActionKind, Diagnostic, TextEdit } from 'vscode-languageserver/node';
import { DIAGNOSTIC_SOURCE } from './diagnostics.js';

/**
 * Builds a code action that appends `// tql:ignore <ruleId>` to the end of a diagnostic's line,
 * for every tql diagnostic in the given list.
 *
 * @param uri - the document's URI
 * @param diagnostics - the diagnostics offered to the code action request (only `tql` ones with
 *   a string `code` produce an action; anything else is ignored)
 * @param lineText - the diagnostic's source line, so the edit lands after any existing content
 * @returns one code action per suppressible diagnostic
 */
export function buildSuppressActions(
  uri: string,
  diagnostics: readonly Diagnostic[],
  lineText: string,
): CodeAction[] {
  const actions: CodeAction[] = [];
  for (const diagnostic of diagnostics) {
    if (diagnostic.source !== DIAGNOSTIC_SOURCE || typeof diagnostic.code !== 'string') {
      continue;
    }
    const ruleId = diagnostic.code;
    const line = diagnostic.range.start.line;
    const suffix = lineText.trimEnd().endsWith('//')
      ? ` tql:ignore ${ruleId}`
      : ` // tql:ignore ${ruleId}`;
    const edit: TextEdit = {
      range: {
        start: { line, character: lineText.length },
        end: { line, character: lineText.length },
      },
      newText: suffix,
    };
    actions.push({
      title: `Suppress ${ruleId} with // tql:ignore`,
      kind: CodeActionKind.QuickFix,
      diagnostics: [diagnostic],
      edit: { changes: { [uri]: [edit] } },
    });
  }
  return actions;
}
