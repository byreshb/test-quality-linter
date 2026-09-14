package io.github.byreshb.tql.rule;

import io.github.byreshb.tql.model.Severity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * What the user configured for the rules: which are disabled, which severities are overridden,
 * per-rule options and the file globs to exclude. Immutable; build one with {@link #builder()} or
 * start from {@link #defaults()}.
 */
public final class RuleConfig {

  private static final RuleConfig DEFAULTS = builder().build();

  private final Set<String> disabled;
  private final Map<String, Severity> severities;
  private final Map<String, Map<String, Object>> options;
  private final List<String> excludes;

  private RuleConfig(Builder builder) {
    this.disabled = Collections.unmodifiableSet(new LinkedHashSet<>(builder.disabled));
    this.severities = Collections.unmodifiableMap(new LinkedHashMap<>(builder.severities));
    Map<String, Map<String, Object>> copy = new LinkedHashMap<>();
    builder.options.forEach(
        (rule, opts) -> copy.put(rule, Collections.unmodifiableMap(new LinkedHashMap<>(opts))));
    this.options = Collections.unmodifiableMap(copy);
    this.excludes = List.copyOf(builder.excludes);
  }

  /**
   * The configuration with every rule enabled at its default severity and nothing excluded.
   *
   * @return the default configuration
   */
  public static RuleConfig defaults() {
    return DEFAULTS;
  }

  /**
   * Starts building a configuration.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Whether a rule should run.
   *
   * @param ruleId the rule id
   * @return true unless the rule was disabled
   */
  public boolean isEnabled(String ruleId) {
    return !disabled.contains(ruleId);
  }

  /**
   * The severity a rule's findings should have.
   *
   * @param ruleId the rule id
   * @param defaultSeverity the rule's own default
   * @return the configured override, or the default
   */
  public Severity severityOf(String ruleId, Severity defaultSeverity) {
    return severities.getOrDefault(ruleId, defaultSeverity);
  }

  /**
   * One option of a rule, as configured.
   *
   * @param ruleId the rule id
   * @param key the option name
   * @return the value, or empty when not configured
   */
  public Optional<Object> option(String ruleId, String key) {
    return Optional.ofNullable(options.getOrDefault(ruleId, Map.of()).get(key));
  }

  /**
   * A string option of a rule.
   *
   * @param ruleId the rule id
   * @param key the option name
   * @param defaultValue the value when not configured
   * @return the configured value as a string, or the default
   */
  public String stringOption(String ruleId, String key, String defaultValue) {
    return option(ruleId, key).map(Object::toString).orElse(defaultValue);
  }

  /**
   * An integer option of a rule.
   *
   * @param ruleId the rule id
   * @param key the option name
   * @param defaultValue the value when not configured
   * @return the configured value, or the default
   * @throws IllegalArgumentException when the configured value is not an integer
   */
  public int intOption(String ruleId, String key, int defaultValue) {
    Optional<Object> value = option(ruleId, key);
    if (value.isEmpty()) {
      return defaultValue;
    }
    if (value.get() instanceof Number number) {
      return number.intValue();
    }
    try {
      return Integer.parseInt(value.get().toString().trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Option " + key + " of " + ruleId + " must be an integer, was: " + value.get(), e);
    }
  }

  /**
   * A boolean option of a rule.
   *
   * @param ruleId the rule id
   * @param key the option name
   * @param defaultValue the value when not configured
   * @return the configured value, or the default
   */
  public boolean booleanOption(String ruleId, String key, boolean defaultValue) {
    return option(ruleId, key)
        .map(v -> v instanceof Boolean b ? b : Boolean.parseBoolean(v.toString().trim()))
        .orElse(defaultValue);
  }

  /**
   * A list option of a rule. A single scalar is treated as a one-element list.
   *
   * @param ruleId the rule id
   * @param key the option name
   * @param defaultValue the value when not configured
   * @return the configured strings, or the default
   */
  public List<String> listOption(String ruleId, String key, List<String> defaultValue) {
    Optional<Object> value = option(ruleId, key);
    if (value.isEmpty()) {
      return defaultValue;
    }
    if (value.get() instanceof Iterable<?> iterable) {
      List<String> result = new ArrayList<>();
      iterable.forEach(item -> result.add(String.valueOf(item)));
      return Collections.unmodifiableList(result);
    }
    return List.of(value.get().toString());
  }

  /**
   * The ids of the disabled rules.
   *
   * @return the disabled rule ids
   */
  public Set<String> disabled() {
    return disabled;
  }

  /**
   * The configured severity overrides.
   *
   * @return rule id to severity
   */
  public Map<String, Severity> severities() {
    return severities;
  }

  /**
   * The glob patterns of files to skip, matched against the path as given to the linter.
   *
   * @return the exclude globs
   */
  public List<String> excludes() {
    return excludes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RuleConfig other)) {
      return false;
    }
    return disabled.equals(other.disabled)
        && severities.equals(other.severities)
        && options.equals(other.options)
        && excludes.equals(other.excludes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(disabled, severities, options, excludes);
  }

  @Override
  public String toString() {
    return "RuleConfig{disabled="
        + disabled
        + ", severities="
        + severities
        + ", options="
        + options
        + ", excludes="
        + excludes
        + '}';
  }

  /** Builds a {@link RuleConfig}. */
  public static final class Builder {

    private final Set<String> disabled = new LinkedHashSet<>();
    private final Map<String, Severity> severities = new LinkedHashMap<>();
    private final Map<String, Map<String, Object>> options = new LinkedHashMap<>();
    private final List<String> excludes = new ArrayList<>();

    private Builder() {}

    /**
     * Disables a rule.
     *
     * @param ruleId the rule id
     * @return this builder
     */
    public Builder disable(String ruleId) {
      disabled.add(Objects.requireNonNull(ruleId, "ruleId"));
      return this;
    }

    /**
     * Enables a rule that was disabled earlier in this builder.
     *
     * @param ruleId the rule id
     * @return this builder
     */
    public Builder enable(String ruleId) {
      disabled.remove(Objects.requireNonNull(ruleId, "ruleId"));
      return this;
    }

    /**
     * Overrides the severity of a rule.
     *
     * @param ruleId the rule id
     * @param severity the severity its findings should have
     * @return this builder
     */
    public Builder severity(String ruleId, Severity severity) {
      severities.put(
          Objects.requireNonNull(ruleId, "ruleId"), Objects.requireNonNull(severity, "severity"));
      return this;
    }

    /**
     * Sets an option of a rule.
     *
     * @param ruleId the rule id
     * @param key the option name
     * @param value the value: a string, number, boolean or list
     * @return this builder
     */
    public Builder option(String ruleId, String key, Object value) {
      options
          .computeIfAbsent(
              Objects.requireNonNull(ruleId, "ruleId"), unused -> new LinkedHashMap<>())
          .put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
      return this;
    }

    /**
     * Adds a glob of files to skip, for example {@code **}{@code /generated/**}.
     *
     * @param glob the glob pattern
     * @return this builder
     */
    public Builder exclude(String glob) {
      excludes.add(Objects.requireNonNull(glob, "glob"));
      return this;
    }

    /**
     * Builds the configuration.
     *
     * @return the immutable configuration
     */
    public RuleConfig build() {
      return new RuleConfig(this);
    }
  }
}
