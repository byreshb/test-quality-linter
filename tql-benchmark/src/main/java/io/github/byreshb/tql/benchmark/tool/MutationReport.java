package io.github.byreshb.tql.benchmark.tool;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Reads PIT's {@code mutations.xml} report and counts, per test method, how many distinct mutants
 * it alone (as PIT's recorded {@code killingTest}) killed.
 */
final class MutationReport {

  // pitest-junit5-plugin reports killingTest as a JUnit 5 unique id, e.g.
  // "io.acme.FooTest.[engine:junit-jupiter]/[class:io.acme.FooTest]/[method:bar()]".
  private static final Pattern JUNIT5_UNIQUE_ID =
      Pattern.compile("\\[class:([\\w.$]+)].*\\[method:([\\w$]+)\\(");
  // Fallback for a plain "Class.method(" form (older PIT test engines).
  private static final Pattern SIMPLE_FORM = Pattern.compile("([\\w.$]+)\\.([\\w$]+)\\(");

  private MutationReport() {}

  /**
   * Counts killed mutants attributed to each test method.
   *
   * @param mutationsXml PIT's {@code mutations.xml}
   * @return {@code ClassName#methodName} mapped to the number of mutants it killed
   * @throws Exception when the report cannot be parsed
   */
  static Map<String, Integer> killedMutantsByTestMethod(Path mutationsXml) throws Exception {
    Map<String, Integer> counts = new HashMap<>();
    Document doc =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(mutationsXml.toFile());
    NodeList mutations = doc.getElementsByTagName("mutation");
    for (int i = 0; i < mutations.getLength(); i++) {
      Element mutation = (Element) mutations.item(i);
      if (!"true".equals(mutation.getAttribute("detected"))) {
        continue;
      }
      String killingTest = text(mutation, "killingTest");
      if (killingTest.isBlank()) {
        continue;
      }
      key(killingTest).ifPresent(key -> counts.merge(key, 1, Integer::sum));
    }
    return counts;
  }

  private static String text(Element parent, String tag) {
    NodeList list = parent.getElementsByTagName(tag);
    return list.getLength() == 0 ? "" : list.item(0).getTextContent();
  }

  private static Optional<String> key(String killingTest) {
    Matcher junit5 = JUNIT5_UNIQUE_ID.matcher(killingTest);
    if (junit5.find()) {
      return Optional.of(simpleName(junit5.group(1)) + "#" + junit5.group(2));
    }
    Matcher simple = SIMPLE_FORM.matcher(killingTest);
    if (simple.find()) {
      return Optional.of(simpleName(simple.group(1)) + "#" + simple.group(2));
    }
    return Optional.empty();
  }

  private static String simpleName(String qualifiedClass) {
    return qualifiedClass.contains(".")
        ? qualifiedClass.substring(qualifiedClass.lastIndexOf('.') + 1)
        : qualifiedClass;
  }
}
