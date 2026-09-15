/**
 * The VS Code extension's entry point: launches the language server as a child process and
 * registers the "TQL: Explain rule" command. All linting logic lives in the server
 * (`src/server`); this file is glue, excluded from the coverage threshold and exercised by the
 * `@vscode/test-electron` suite under `test/` instead of Vitest.
 */

import { mkdir } from 'node:fs/promises';
import * as vscode from 'vscode';
import {
  LanguageClient,
  LanguageClientOptions,
  ServerOptions,
  TransportKind,
} from 'vscode-languageclient/node';
import { cachedJarPath, ensureDownloaded, releaseJarUrl } from './download.js';
import { ruleDocsUrl } from './commands/explainRule.js';
import pkg from '../package.json' with { type: 'json' };

let client: LanguageClient | undefined;

/**
 * Activates the extension.
 *
 * @param context - the extension's context, for its storage path and subscriptions
 */
export async function activate(context: vscode.ExtensionContext): Promise<void> {
  const jarPath = await resolveJarPath(context);

  const serverModule = context.asAbsolutePath('dist/server/index.js');
  const serverOptions: ServerOptions = {
    run: { module: serverModule, transport: TransportKind.ipc },
    debug: { module: serverModule, transport: TransportKind.ipc },
  };
  const clientOptions: LanguageClientOptions = {
    documentSelector: [{ scheme: 'file', language: 'java' }],
    initializationOptions: { jarPath },
  };
  client = new LanguageClient('tql', 'Test Quality Linter', serverOptions, clientOptions);
  context.subscriptions.push(client);
  await client.start();

  context.subscriptions.push(
    vscode.commands.registerCommand('tql.explainRule', async () => {
      const ruleId = await vscode.window.showInputBox({
        prompt: 'Rule id to explain, e.g. TQL001',
        placeHolder: 'TQL001',
      });
      if (ruleId) {
        await vscode.env.openExternal(vscode.Uri.parse(ruleDocsUrl(ruleId.trim().toUpperCase())));
      }
    }),
  );
}

/** Deactivates the extension, stopping the language server. */
export function deactivate(): Thenable<void> | undefined {
  return client?.stop();
}

async function resolveJarPath(context: vscode.ExtensionContext): Promise<string> {
  const config = vscode.workspace.getConfiguration('tql');
  const configuredPath = config.get<string>('jarPath', '');
  if (configuredPath) {
    return configuredPath;
  }
  const version = config.get<string>('version', '') || (pkg as { version: string }).version;
  const storageDir = context.globalStorageUri.fsPath;
  await mkdir(storageDir, { recursive: true });
  const destPath = cachedJarPath(storageDir, version);
  await ensureDownloaded(releaseJarUrl(version), destPath);
  return destPath;
}
