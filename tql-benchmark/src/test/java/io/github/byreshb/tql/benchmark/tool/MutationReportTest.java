package io.github.byreshb.tql.benchmark.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MutationReportTest {

  private static final String XML =
      """
<mutations>
  <mutation detected="true">
    <killingTest>io.acme.CalculatorTest.[engine:junit-jupiter]/[class:io.acme.CalculatorTest]/[method:addsTwoNumbers()]</killingTest>
  </mutation>
  <mutation detected="true">
    <killingTest>io.acme.CalculatorTest.[engine:junit-jupiter]/[class:io.acme.CalculatorTest]/[method:addsTwoNumbers()]</killingTest>
  </mutation>
  <mutation detected="true">
    <killingTest>io.acme.CalculatorTest.[engine:junit-jupiter]/[class:io.acme.CalculatorTest]/[method:subtractsTwoNumbers()]</killingTest>
  </mutation>
  <mutation detected="true">
    <killingTest>io.acme.InvoiceTest.totalIsComputed(io.acme.InvoiceTest)</killingTest>
  </mutation>
  <mutation detected="false">
    <killingTest></killingTest>
  </mutation>
  <mutation detected="false">
  </mutation>
</mutations>
""";

  @Test
  void countsKilledMutantsPerTestMethodFromBothKillingTestFormats(@TempDir Path dir)
      throws Exception {
    Path xml = dir.resolve("mutations.xml");
    Files.writeString(xml, XML);

    Map<String, Integer> counts = MutationReport.killedMutantsByTestMethod(xml);

    assertThat(counts)
        .containsEntry("CalculatorTest#addsTwoNumbers", 2)
        .containsEntry("CalculatorTest#subtractsTwoNumbers", 1)
        .containsEntry("InvoiceTest#totalIsComputed", 1)
        .hasSize(3);
  }

  @Test
  void ignoresUndetectedMutationsAndMissingKillingTest(@TempDir Path dir) throws Exception {
    Path xml = dir.resolve("mutations.xml");
    Files.writeString(
        xml,
        """
<mutations>
  <mutation detected="false">
    <killingTest>io.acme.FooTest.[engine:junit-jupiter]/[class:io.acme.FooTest]/[method:bar()]</killingTest>
  </mutation>
</mutations>
""");
    assertThat(MutationReport.killedMutantsByTestMethod(xml)).isEmpty();
  }
}
