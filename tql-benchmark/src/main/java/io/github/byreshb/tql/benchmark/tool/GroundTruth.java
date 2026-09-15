package io.github.byreshb.tql.benchmark.tool;

/**
 * One planted test method: where it is, and which rule ids (if any) it was written to trigger. An
 * empty {@code weakRuleIds} means the test is a planted clean example.
 *
 * @param className the test class's simple name
 * @param methodName the test method's name
 * @param weakRuleIds the rule ids this test is written to trigger, empty for a clean example
 */
record GroundTruth(String className, String methodName, java.util.Set<String> weakRuleIds) {

  /**
   * Whether this test is a planted weak example.
   *
   * @return true when it carries at least one expected rule id
   */
  boolean isWeak() {
    return !weakRuleIds.isEmpty();
  }
}
