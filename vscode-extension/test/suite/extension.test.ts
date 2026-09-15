/**
 * Runs inside a real VS Code instance (via `@vscode/test-electron`), exercising activation and
 * the "TQL: Explain rule" command against the actual extension host, not a stand-in for it.
 */

import * as assert from 'node:assert/strict';
import * as vscode from 'vscode';

suite('Test Quality Linter extension', () => {
  test('is present and activates', async () => {
    const extension = vscode.extensions.getExtension('byreshb.tql-vscode');
    assert.ok(extension, 'extension byreshb.tql-vscode is not installed in the test instance');
    await extension.activate();
    assert.equal(extension.isActive, true);
  });

  test('registers the tql.explainRule command', async () => {
    const commands = await vscode.commands.getCommands(true);
    assert.ok(commands.includes('tql.explainRule'), 'tql.explainRule was not registered');
  });

  test('contributes the tql.jarPath and tql.version settings', () => {
    const config = vscode.workspace.getConfiguration('tql');
    assert.equal(config.get('jarPath'), '');
    assert.equal(config.get('version'), '');
  });
});
