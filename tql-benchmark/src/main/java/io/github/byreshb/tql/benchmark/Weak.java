package io.github.byreshb.tql.benchmark;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ground truth for the benchmark: marks a test method as deliberately weak, in the style described
 * by the named rule ids, so {@code BenchmarkRunner} can score the linter's findings against a known
 * answer. A test method without this annotation is a planted "clean" example.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Weak {

  /**
   * The rule ids this test is written to trigger.
   *
   * @return the rule ids, e.g. {@code {"TQL001"}}
   */
  String[] value();
}
