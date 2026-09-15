package io.github.byreshb.tql.benchmark.tool;

import io.github.byreshb.tql.benchmark.Weak;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads the planted ground truth from the compiled benchmark test classes, via reflection on the
 * {@link Weak} annotation.
 */
final class GroundTruthReader {

  private GroundTruthReader() {}

  /**
   * Reads every {@code @Test}-annotated method under the given test class directory.
   *
   * @param testClassesDir the directory of compiled test classes, e.g. {@code target/test-classes}
   * @param packageName the package the benchmark's test classes live in
   * @return the ground truth, one entry per test method
   * @throws Exception when a class cannot be loaded
   */
  static List<GroundTruth> read(Path testClassesDir, String packageName) throws Exception {
    List<GroundTruth> truth = new ArrayList<>();
    try (URLClassLoader loader =
        new URLClassLoader(
            new java.net.URL[] {testClassesDir.toUri().toURL()},
            GroundTruthReader.class.getClassLoader())) {
      for (Path classFile : testClassFiles(testClassesDir)) {
        String relative = testClassesDir.relativize(classFile).toString();
        String qualified =
            relative.substring(0, relative.length() - ".class".length()).replace('/', '.');
        // Exact package match, not startsWith: excludes the tool.* subpackage's own tests, which
        // are not planted benchmark examples.
        int lastDot = qualified.lastIndexOf('.');
        if (lastDot < 0 || !qualified.substring(0, lastDot).equals(packageName)) {
          continue;
        }
        Class<?> type = Class.forName(qualified, false, loader);
        for (Method method : type.getDeclaredMethods()) {
          boolean isTest =
              java.util.Arrays.stream(method.getAnnotations())
                  .anyMatch(a -> a.annotationType().getSimpleName().equals("Test"));
          if (!isTest) {
            continue;
          }
          Weak weak = method.getAnnotation(Weak.class);
          Set<String> ruleIds = weak == null ? Set.of() : Set.of(weak.value());
          truth.add(new GroundTruth(type.getSimpleName(), method.getName(), ruleIds));
        }
      }
    }
    return truth;
  }

  private static List<Path> testClassFiles(Path dir) throws Exception {
    try (var walk = Files.walk(dir)) {
      return walk.filter(p -> p.toString().endsWith(".class"))
          .filter(p -> !p.toString().contains("$"))
          .collect(Collectors.toList());
    }
  }
}
