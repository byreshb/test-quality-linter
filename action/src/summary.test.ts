import { describe, expect, it } from 'vitest';
import { renderSummary } from './summary.js';

describe('renderSummary', () => {
  it('reports no findings', () => {
    expect(renderSummary([])).toBe('## Test Quality Linter\n\nNo findings.\n');
  });

  it('summarises counts by severity and by rule', () => {
    const markdown = renderSummary([
      { ruleId: 'TQL001', severity: 'ERROR' },
      { ruleId: 'TQL001', severity: 'ERROR' },
      { ruleId: 'TQL003', severity: 'WARN' },
    ]);
    expect(markdown).toContain('3 findings (2 errors, 1 warning, 0 info)');
    expect(markdown).toContain('| `TQL001` | 2 |');
    expect(markdown).toContain('| `TQL003` | 1 |');
  });

  it('uses the singular for exactly one finding', () => {
    const markdown = renderSummary([{ ruleId: 'TQL001', severity: 'ERROR' }]);
    expect(markdown).toContain('1 finding (1 error, 0 warnings, 0 info)');
  });
});
