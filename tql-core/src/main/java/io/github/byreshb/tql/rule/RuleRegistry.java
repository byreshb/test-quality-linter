package io.github.byreshb.tql.rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * The set of rules available to a linter run, sorted by id. The built-in rules, and any rule on the
 * classpath registered as a {@link ServiceLoader} service, are found by {@link #discover()}.
 */
public final class RuleRegistry {

  private final List<Rule> rules;

  /**
   * Creates a registry from explicit rules.
   *
   * @param rules the rules, in any order
   * @throws IllegalArgumentException when two rules share an id
   */
  public RuleRegistry(List<? extends Rule> rules) {
    List<Rule> sorted = new ArrayList<>(rules);
    sorted.sort(Comparator.comparing(Rule::id));
    for (int i = 1; i < sorted.size(); i++) {
      if (sorted.get(i - 1).id().equals(sorted.get(i).id())) {
        throw new IllegalArgumentException("Duplicate rule id " + sorted.get(i).id());
      }
    }
    this.rules = Collections.unmodifiableList(sorted);
  }

  /**
   * Discovers every rule registered as a service in {@code
   * META-INF/services/io.github.byreshb.tql.rule.Rule}, which includes all built-in rules.
   *
   * @return the registry
   */
  public static RuleRegistry discover() {
    List<Rule> found = new ArrayList<>();
    ServiceLoader.load(Rule.class, RuleRegistry.class.getClassLoader()).forEach(found::add);
    return new RuleRegistry(found);
  }

  /**
   * Creates a registry from explicit rules.
   *
   * @param rules the rules
   * @return the registry
   */
  public static RuleRegistry of(Rule... rules) {
    return new RuleRegistry(List.of(rules));
  }

  /**
   * All rules, sorted by id.
   *
   * @return the rules
   */
  public List<Rule> rules() {
    return rules;
  }

  /**
   * Looks a rule up by id, case-insensitively.
   *
   * @param id the rule id
   * @return the rule, or empty when unknown
   */
  public Optional<Rule> find(String id) {
    return rules.stream().filter(rule -> rule.id().equalsIgnoreCase(id)).findFirst();
  }

  /**
   * The rules the configuration leaves enabled.
   *
   * @param config the configuration
   * @return the enabled rules, sorted by id
   */
  public List<Rule> enabled(RuleConfig config) {
    return rules.stream().filter(rule -> config.isEnabled(rule.id())).toList();
  }
}
