import { describe, expect, it } from 'vitest';
import { countByRule, countBySeverity, parseSarifFindings } from './sarif.js';

const SARIF = JSON.stringify({
  version: '2.1.0',
  runs: [
    {
      tool: { driver: { name: 'tql' } },
      results: [
        { ruleId: 'TQL001', level: 'error', message: { text: 'm1' } },
        { ruleId: 'TQL001', level: 'error', message: { text: 'm2' } },
        { ruleId: 'TQL003', level: 'warning', message: { text: 'm3' } },
        { ruleId: 'TQL010', level: 'note', message: { text: 'm4' } },
      ],
    },
  ],
});

describe('parseSarifFindings', () => {
  it('extracts rule id and severity from each result', () => {
    const findings = parseSarifFindings(SARIF);
    expect(findings).toEqual([
      { ruleId: 'TQL001', severity: 'ERROR' },
      { ruleId: 'TQL001', severity: 'ERROR' },
      { ruleId: 'TQL003', severity: 'WARN' },
      { ruleId: 'TQL010', severity: 'INFO' },
    ]);
  });

  it('returns an empty array when there are no runs or results', () => {
    expect(parseSarifFindings(JSON.stringify({ runs: [] }))).toEqual([]);
    expect(parseSarifFindings(JSON.stringify({ runs: [{ tool: {} }] }))).toEqual([]);
  });

  it('rejects invalid JSON', () => {
    expect(() => parseSarifFindings('not json')).toThrowError(/valid SARIF JSON/);
  });

  it('rejects JSON that is not shaped like SARIF', () => {
    expect(() => parseSarifFindings(JSON.stringify({ foo: 'bar' }))).toThrowError(/Not a SARIF/);
  });

  it('defaults a missing ruleId or level', () => {
    const sarif = JSON.stringify({ runs: [{ results: [{}] }] });
    expect(parseSarifFindings(sarif)).toEqual([{ ruleId: 'unknown', severity: 'INFO' }]);
  });
});

describe('countByRule', () => {
  it('counts findings per rule, sorted by rule id', () => {
    const findings = parseSarifFindings(SARIF);
    expect([...countByRule(findings).entries()]).toEqual([
      ['TQL001', 2],
      ['TQL003', 1],
      ['TQL010', 1],
    ]);
  });
});

describe('countBySeverity', () => {
  it('counts findings per severity, including zero counts', () => {
    const findings = parseSarifFindings(SARIF);
    expect(countBySeverity(findings)).toEqual({ INFO: 1, WARN: 1, ERROR: 2 });
    expect(countBySeverity([])).toEqual({ INFO: 0, WARN: 0, ERROR: 0 });
  });
});
