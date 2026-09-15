package io.github.byreshb.tql.rule;

import io.github.byreshb.tql.model.Severity;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * Loads a {@link RuleConfig} from a {@code .tql.yaml} file:
 *
 * <pre>{@code
 * rules:
 *   TQL001:
 *     enabled: false
 *   TQL003:
 *     severity: ERROR
 *     options:
 *       methods: [ensureSaved]
 *       packages: [com.acme.testing]
 *   TQL006:
 *     options:
 *       maxMillis: 50
 * exclude:
 *   - "**&#47;generated/**"
 * }</pre>
 *
 * <p>Every key is optional; a rule not mentioned keeps its default severity and stays enabled. An
 * unreadable or malformed file is reported as an {@link UncheckedIOException} or {@link
 * IllegalArgumentException} rather than silently ignored, since a typo in the configuration should
 * not quietly disable it.
 */
public final class RuleConfigLoader {

  private RuleConfigLoader() {}

  /**
   * Loads configuration from a YAML file.
   *
   * @param path the {@code .tql.yaml} file
   * @return the configuration
   * @throws UncheckedIOException when the file cannot be read
   * @throws IllegalArgumentException when the YAML is not shaped as expected
   */
  public static RuleConfig load(Path path) {
    try {
      return load(Files.readString(path, StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read " + path, e);
    }
  }

  /**
   * Loads configuration from YAML text.
   *
   * @param yaml the YAML document; blank or {@code null} yields {@link RuleConfig#defaults()}
   * @return the configuration
   * @throws IllegalArgumentException when the YAML is not shaped as expected
   */
  public static RuleConfig load(String yaml) {
    if (yaml == null || yaml.isBlank()) {
      return RuleConfig.defaults();
    }
    Object root = new Yaml().load(yaml);
    if (root == null) {
      return RuleConfig.defaults();
    }
    if (!(root instanceof Map<?, ?> rootMap)) {
      throw new IllegalArgumentException("The top level of .tql.yaml must be a mapping");
    }
    RuleConfig.Builder builder = RuleConfig.builder();
    applyRules(rootMap.get("rules"), builder);
    applyExcludes(rootMap.get("exclude"), builder);
    return builder.build();
  }

  private static void applyRules(Object rulesValue, RuleConfig.Builder builder) {
    if (rulesValue == null) {
      return;
    }
    if (!(rulesValue instanceof Map<?, ?> rules)) {
      throw new IllegalArgumentException("'rules' must be a mapping of rule id to settings");
    }
    for (Map.Entry<?, ?> entry : rules.entrySet()) {
      String ruleId = String.valueOf(entry.getKey());
      if (!(entry.getValue() instanceof Map<?, ?> settings)) {
        throw new IllegalArgumentException("Settings for rule " + ruleId + " must be a mapping");
      }
      applyRule(ruleId, settings, builder);
    }
  }

  private static void applyRule(String ruleId, Map<?, ?> settings, RuleConfig.Builder builder) {
    Object enabled = settings.get("enabled");
    if (enabled instanceof Boolean enabledFlag && !enabledFlag) {
      builder.disable(ruleId);
    }
    Object severity = settings.get("severity");
    if (severity != null) {
      builder.severity(ruleId, Severity.parse(String.valueOf(severity)));
    }
    Object options = settings.get("options");
    if (options != null) {
      if (!(options instanceof Map<?, ?> optionsMap)) {
        throw new IllegalArgumentException("'options' of rule " + ruleId + " must be a mapping");
      }
      for (Map.Entry<?, ?> option : optionsMap.entrySet()) {
        if (option.getValue() != null) {
          builder.option(ruleId, String.valueOf(option.getKey()), option.getValue());
        }
      }
    }
  }

  private static void applyExcludes(Object excludeValue, RuleConfig.Builder builder) {
    if (excludeValue == null) {
      return;
    }
    if (!(excludeValue instanceof List<?> excludes)) {
      throw new IllegalArgumentException("'exclude' must be a list of glob patterns");
    }
    for (Object glob : excludes) {
      builder.exclude(String.valueOf(glob));
    }
  }
}
