import { describe, expect, it } from 'vitest';
import { ruleDocsUrl } from './explainRule.js';

describe('ruleDocsUrl', () => {
  it('builds the docs page URL for a rule id', () => {
    expect(ruleDocsUrl('TQL001')).toBe(
      'https://github.com/byreshb/test-quality-linter/blob/main/docs/rules/TQL001.md',
    );
  });
});
