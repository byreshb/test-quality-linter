package io.github.byreshb.tql.output;

import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.LintResult;
import io.github.byreshb.tql.model.Severity;
import io.github.byreshb.tql.rule.Rule;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * SARIF 2.1.0 output, so GitHub code scanning (or any other SARIF consumer) shows findings inline
 * on a pull request. One run, one tool driver named {@code tql} listing every rule the linter was
 * configured with (so the viewer can show a rule's description even for a severity it did not
 * trigger), and one result per finding with a single physical location.
 *
 * @see <a href="https://docs.oasis-open.org/sarif/sarif/v2.1.0/sarif-v2.1.0.html">SARIF 2.1.0</a>
 */
public final class SarifReporter implements Reporter {

  private final List<Rule> rules;
  private final String toolVersion;

  /**
   * Creates a reporter listing the given rules, with an unspecified tool version.
   *
   * @param rules the rules the linter ran with, for the {@code tool.driver.rules} array
   */
  public SarifReporter(List<Rule> rules) {
    this(rules, "0.0.0");
  }

  /**
   * Creates a reporter.
   *
   * @param rules the rules the linter ran with, for the {@code tool.driver.rules} array
   * @param toolVersion the linter's own version, reported as {@code tool.driver.version}
   */
  public SarifReporter(List<Rule> rules, String toolVersion) {
    this.rules = List.copyOf(Objects.requireNonNull(rules, "rules"));
    this.toolVersion = Objects.requireNonNull(toolVersion, "toolVersion");
  }

  @Override
  public void write(LintResult result, Appendable out) throws IOException {
    out.append("{\n");
    out.append(
        "  \"$schema\":"
            + " \"https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json\",\n");
    out.append("  \"version\": \"2.1.0\",\n");
    out.append("  \"runs\": [\n");
    out.append("    {\n");
    writeDriver(out);
    out.append(",\n");
    writeResults(result, out);
    out.append('\n');
    out.append("    }\n");
    out.append("  ]\n");
    out.append("}\n");
  }

  private void writeDriver(Appendable out) throws IOException {
    out.append("      \"tool\": {\n");
    out.append("        \"driver\": {\n");
    out.append("          \"name\": \"tql\",\n");
    out.append(
        "          \"informationUri\": \"https://github.com/byreshb/test-quality-linter\",\n");
    out.append("          \"version\": ");
    Json.writeString(out, toolVersion);
    out.append(",\n          \"rules\": [");
    for (int i = 0; i < rules.size(); i++) {
      out.append(i == 0 ? "\n" : ",\n");
      writeRuleDescriptor(rules.get(i), out);
    }
    out.append(rules.isEmpty() ? "]\n" : "\n          ]\n");
    out.append("        }\n");
    out.append("      }");
  }

  private static void writeRuleDescriptor(Rule rule, Appendable out) throws IOException {
    out.append("            {\n");
    out.append("              \"id\": ");
    Json.writeString(out, rule.id());
    out.append(",\n              \"name\": ");
    Json.writeString(out, rule.name());
    out.append(",\n              \"shortDescription\": { \"text\": ");
    Json.writeString(out, rule.description());
    out.append(" },\n              \"defaultConfiguration\": { \"level\": \"")
        .append(sarifLevel(rule.defaultSeverity()))
        .append("\" }\n");
    out.append("            }");
  }

  private static void writeResults(LintResult result, Appendable out) throws IOException {
    out.append("      \"results\": [");
    List<Finding> findings = result.findings();
    for (int i = 0; i < findings.size(); i++) {
      out.append(i == 0 ? "\n" : ",\n");
      writeResult(findings.get(i), out);
    }
    out.append(findings.isEmpty() ? "]" : "\n      ]");
  }

  private static void writeResult(Finding finding, Appendable out) throws IOException {
    out.append("        {\n");
    out.append("          \"ruleId\": ");
    Json.writeString(out, finding.ruleId());
    out.append(",\n          \"level\": \"").append(sarifLevel(finding.severity())).append("\",\n");
    out.append("          \"message\": { \"text\": ");
    Json.writeString(out, finding.message());
    out.append(" },\n");
    out.append("          \"locations\": [\n");
    out.append("            {\n");
    out.append("              \"physicalLocation\": {\n");
    out.append("                \"artifactLocation\": { \"uri\": ");
    Json.writeString(out, finding.file().toString().replace('\\', '/'));
    out.append(" },\n");
    out.append("                \"region\": { \"startLine\": ")
        .append(String.valueOf(finding.line()))
        .append(", \"startColumn\": ")
        .append(String.valueOf(finding.column()))
        .append(" }\n");
    out.append("              }\n");
    out.append("            }\n");
    out.append("          ]\n");
    out.append("        }");
  }

  private static String sarifLevel(Severity severity) {
    return switch (severity) {
      case ERROR -> "error";
      case WARN -> "warning";
      case INFO -> "note";
    };
  }
}
