package io.github.byreshb.tql.output;

import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.LintResult;
import io.github.byreshb.tql.model.ParseProblem;
import java.io.IOException;

/**
 * Machine-readable output: one JSON object with {@code filesScanned}, the full {@code findings}
 * array (in the same order as {@link LintResult#findings()}) and the {@code problems} array of
 * files that failed to parse. A finding without a snippet has a JSON {@code null} in its place.
 */
public final class JsonReporter implements Reporter {

  @Override
  public void write(LintResult result, Appendable out) throws IOException {
    out.append("{\n");
    out.append("  \"filesScanned\": ").append(String.valueOf(result.filesScanned())).append(",\n");
    writeFindings(result, out);
    out.append(",\n");
    writeProblems(result, out);
    out.append('\n');
    out.append("}\n");
  }

  private static void writeFindings(LintResult result, Appendable out) throws IOException {
    out.append("  \"findings\": [");
    var findings = result.findings();
    for (int i = 0; i < findings.size(); i++) {
      out.append(i == 0 ? "\n" : ",\n");
      writeFinding(findings.get(i), out);
    }
    out.append(findings.isEmpty() ? "]" : "\n  ]");
  }

  private static void writeFinding(Finding finding, Appendable out) throws IOException {
    out.append("    {\n");
    out.append("      \"ruleId\": ");
    Json.writeString(out, finding.ruleId());
    out.append(",\n      \"severity\": ");
    Json.writeString(out, finding.severity().toString());
    out.append(",\n      \"file\": ");
    Json.writeString(out, finding.file().toString());
    out.append(",\n      \"line\": ").append(String.valueOf(finding.line()));
    out.append(",\n      \"column\": ").append(String.valueOf(finding.column()));
    out.append(",\n      \"message\": ");
    Json.writeString(out, finding.message());
    out.append(",\n      \"fixHint\": ");
    Json.writeString(out, finding.fixHint());
    out.append(",\n      \"snippet\": ");
    if (finding.snippet().isPresent()) {
      Json.writeString(out, finding.snippet().get());
    } else {
      out.append("null");
    }
    out.append("\n    }");
  }

  private static void writeProblems(LintResult result, Appendable out) throws IOException {
    out.append("  \"problems\": [");
    var problems = result.problems();
    for (int i = 0; i < problems.size(); i++) {
      out.append(i == 0 ? "\n" : ",\n");
      writeProblem(problems.get(i), out);
    }
    out.append(problems.isEmpty() ? "]" : "\n  ]");
  }

  private static void writeProblem(ParseProblem problem, Appendable out) throws IOException {
    out.append("    {\n");
    out.append("      \"file\": ");
    Json.writeString(out, problem.file().toString());
    out.append(",\n      \"line\": ").append(String.valueOf(problem.line()));
    out.append(",\n      \"message\": ");
    Json.writeString(out, problem.message());
    out.append("\n    }");
  }
}
