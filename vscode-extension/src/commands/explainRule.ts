/** Builds the docs URL the "TQL: Explain rule" command opens. */

/**
 * The docs page URL for a rule id.
 *
 * @param ruleId - the rule id, e.g. `"TQL001"`
 * @returns the GitHub URL for that rule's docs page
 */
export function ruleDocsUrl(ruleId: string): string {
  return `https://github.com/byreshb/test-quality-linter/blob/main/docs/rules/${ruleId}.md`;
}
