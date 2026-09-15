package io.github.byreshb.tql.benchmark.tool;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.Severity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FindingAttributorTest {

  @Test
  void groupsFindingsByTheirEnclosingMethod(@TempDir Path dir) throws Exception {
    Path file = dir.resolve("SampleTest.java");
    Files.writeString(
        file,
        """
        class SampleTest {
          void a() {
            int x = 1;
          }

          void b() {
            int y = 2;
          }
        }
        """);

    Finding inA = finding(file, 3, "TQL001");
    Finding inB1 = finding(file, 7, "TQL002");
    Finding inB2 = finding(file, 7, "TQL003");

    var byMethod = FindingAttributor.byTestMethod(List.of(inA, inB1, inB2));

    assertThat(byMethod.get("SampleTest#a")).containsExactly(inA);
    assertThat(byMethod.get("SampleTest#b")).containsExactlyInAnyOrder(inB1, inB2);
  }

  @Test
  void dropsFindingsThatDoNotFallInsideAnyMethod(@TempDir Path dir) throws Exception {
    Path file = dir.resolve("SampleTest.java");
    Files.writeString(file, "class SampleTest {\n  int field;\n}\n");
    Finding onField = finding(file, 2, "TQL012");
    assertThat(FindingAttributor.byTestMethod(List.of(onField))).isEmpty();
  }

  private static Finding finding(Path file, int line, String ruleId) {
    return new Finding(ruleId, Severity.WARN, file, line, 1, "m", "f", Optional.empty());
  }
}
