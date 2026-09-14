package io.github.byreshb.tql.rule;

import io.github.byreshb.tql.model.Severity;
import java.util.Objects;

/**
 * Base class for rules that fixes the id, name, description and default severity in the constructor
 * so that a rule class only has to implement {@link #check}.
 */
public abstract class AbstractRule implements Rule {

  private final String id;
  private final String name;
  private final String description;
  private final Severity defaultSeverity;

  /**
   * Creates a rule.
   *
   * @param id the stable id, for example {@code TQL001}
   * @param name the CamelCase name
   * @param description one sentence saying what the rule catches
   * @param defaultSeverity the severity unless overridden
   */
  protected AbstractRule(String id, String name, String description, Severity defaultSeverity) {
    this.id = Objects.requireNonNull(id, "id");
    this.name = Objects.requireNonNull(name, "name");
    this.description = Objects.requireNonNull(description, "description");
    this.defaultSeverity = Objects.requireNonNull(defaultSeverity, "defaultSeverity");
  }

  @Override
  public final String id() {
    return id;
  }

  @Override
  public final String name() {
    return name;
  }

  @Override
  public final String description() {
    return description;
  }

  @Override
  public final Severity defaultSeverity() {
    return defaultSeverity;
  }

  @Override
  public String toString() {
    return id + " " + name;
  }
}
