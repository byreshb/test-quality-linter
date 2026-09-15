import { describe, expect, it } from 'vitest';
import { isAtLeast, maxSeverity, parseSeverity, severityFromSarifLevel } from './severity.js';

describe('severityFromSarifLevel', () => {
  it('maps SARIF levels to severities', () => {
    expect(severityFromSarifLevel('error')).toBe('ERROR');
    expect(severityFromSarifLevel('warning')).toBe('WARN');
    expect(severityFromSarifLevel('note')).toBe('INFO');
    expect(severityFromSarifLevel('none')).toBe('INFO');
  });
});

describe('parseSeverity', () => {
  it('parses names case-insensitively and the warning alias', () => {
    expect(parseSeverity('error')).toBe('ERROR');
    expect(parseSeverity('WARN')).toBe('WARN');
    expect(parseSeverity(' Warning ')).toBe('WARN');
    expect(parseSeverity('Info')).toBe('INFO');
  });

  it('rejects an unknown severity', () => {
    expect(() => parseSeverity('fatal')).toThrowError(/Not a severity/);
  });
});

describe('isAtLeast', () => {
  it('compares severities by seriousness', () => {
    expect(isAtLeast('ERROR', 'WARN')).toBe(true);
    expect(isAtLeast('WARN', 'WARN')).toBe(true);
    expect(isAtLeast('INFO', 'WARN')).toBe(false);
  });
});

describe('maxSeverity', () => {
  it('returns the most serious severity', () => {
    expect(maxSeverity(['INFO', 'ERROR', 'WARN'])).toBe('ERROR');
    expect(maxSeverity(['INFO', 'INFO'])).toBe('INFO');
  });

  it('returns undefined for an empty list', () => {
    expect(maxSeverity([])).toBeUndefined();
  });
});
