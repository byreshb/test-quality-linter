package io.github.byreshb.tql.benchmark.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GroundTruthReaderTest {

  @Test
  void readsThePlantedBenchmarkTestsAndOnlyThose() throws Exception {
    Path testClasses = Path.of("target/test-classes");
    var truth = GroundTruthReader.read(testClasses, "io.github.byreshb.tql.benchmark");

    assertThat(truth)
        .extracting(GroundTruth::className)
        .contains("CalculatorTest", "InvoiceTest", "PasswordValidatorTest", "OrderServiceTest")
        .doesNotContain("GroundTruthReaderTest", "FindingAttributorTest", "MutationReportTest");

    assertThat(truth)
        .filteredOn(
            t ->
                t.className().equals("CalculatorTest")
                    && t.methodName().equals("addCompletesWithoutError"))
        .singleElement()
        .satisfies(t -> assertThat(t.weakRuleIds()).containsExactly("TQL001"));

    assertThat(truth)
        .filteredOn(
            t -> t.className().equals("CalculatorTest") && t.methodName().equals("addsTwoNumbers"))
        .singleElement()
        .satisfies(t -> assertThat(t.isWeak()).isFalse());
  }
}
