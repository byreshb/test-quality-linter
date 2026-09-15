/**
 * The language server. Runs `tql` on open and on save for likely test files, publishes the
 * findings as diagnostics, and offers a "suppress with // tql:ignore" code action for each one.
 * Structured as a standalone LSP server (rather than logic embedded in the extension) so any
 * LSP-capable editor, not only VS Code, can run it.
 */

import {
  createConnection,
  Diagnostic,
  DidChangeConfigurationNotification,
  ProposedFeatures,
  TextDocumentSyncKind,
  TextDocuments,
} from 'vscode-languageserver/node';
import { TextDocument } from 'vscode-languageserver-textdocument';
import { fileURLToPath } from 'node:url';
import { buildSuppressActions } from './codeActions.js';
import { toDiagnostic } from './diagnostics.js';
import { lintFile } from './jarRunner.js';
import { isLikelyTestFile } from '../testFile.js';

interface InitializationOptions {
  readonly jarPath?: string;
  readonly javaPath?: string;
}

const connection = createConnection(ProposedFeatures.all);
const documents = new TextDocuments(TextDocument);

let jarPath = '';
let javaPath = 'java';

connection.onInitialize((params) => {
  const options = (params.initializationOptions ?? {}) as InitializationOptions;
  jarPath = options.jarPath ?? '';
  javaPath = options.javaPath ?? 'java';
  return {
    capabilities: {
      textDocumentSync: TextDocumentSyncKind.Full,
      codeActionProvider: true,
    },
  };
});

connection.onInitialized(() => {
  void connection.client.register(DidChangeConfigurationNotification.type, undefined);
});

connection.onDidChangeConfiguration((change) => {
  const settings = change.settings as { tql?: InitializationOptions } | undefined;
  if (settings?.tql?.jarPath) {
    jarPath = settings.tql.jarPath;
  }
  if (settings?.tql?.javaPath) {
    javaPath = settings.tql.javaPath;
  }
});

async function lintAndPublish(document: TextDocument): Promise<void> {
  const filePath = fileURLToPath(document.uri);
  if (!isLikelyTestFile(filePath) || !jarPath) {
    return;
  }
  try {
    const result = await lintFile(javaPath, jarPath, filePath);
    const lines = document.getText().split(/\r\n|\r|\n/);
    const diagnostics: Diagnostic[] = result.findings
      .filter((finding) => finding.file === filePath || finding.file.endsWith(filePath))
      .map((finding) => toDiagnostic(finding, (lines[finding.line - 1] ?? '').length));
    void connection.sendDiagnostics({ uri: document.uri, diagnostics });
  } catch (error) {
    connection.console.error(`tql failed on ${filePath}: ${(error as Error).message}`);
  }
}

documents.onDidOpen((event) => {
  void lintAndPublish(event.document);
});

documents.onDidSave((event) => {
  void lintAndPublish(event.document);
});

connection.onCodeAction((params) => {
  const document = documents.get(params.textDocument.uri);
  if (!document) {
    return [];
  }
  const line = params.range.start.line;
  const lineText = document.getText({
    start: { line, character: 0 },
    end: { line, character: Number.MAX_SAFE_INTEGER },
  });
  return buildSuppressActions(params.textDocument.uri, params.context.diagnostics, lineText);
});

documents.listen(connection);
connection.listen();
