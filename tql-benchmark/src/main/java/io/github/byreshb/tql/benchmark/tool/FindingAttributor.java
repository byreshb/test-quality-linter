package io.github.byreshb.tql.benchmark.tool;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import io.github.byreshb.tql.model.Finding;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Maps each {@link Finding} back to the test method it falls inside, by re-parsing the file. */
final class FindingAttributor {

  private FindingAttributor() {}

  /**
   * Groups findings by {@code ClassName#methodName}, dropping any finding that does not fall inside
   * a method (for example one attached to a field declaration).
   *
   * @param findings the findings to attribute
   * @return findings keyed by the enclosing test method
   */
  static Map<String, List<Finding>> byTestMethod(List<Finding> findings) {
    Map<Path, CompilationUnit> parsedByFile = new HashMap<>();
    Map<String, List<Finding>> result = new HashMap<>();
    for (Finding finding : findings) {
      CompilationUnit unit = parsedByFile.computeIfAbsent(finding.file(), FindingAttributor::parse);
      enclosingMethod(unit, finding.line())
          .ifPresent(
              method -> {
                String className =
                    method
                        .findAncestor(ClassOrInterfaceDeclaration.class)
                        .map(ClassOrInterfaceDeclaration::getNameAsString)
                        .orElse("?");
                String key = className + "#" + method.getNameAsString();
                result.computeIfAbsent(key, unused -> new ArrayList<>()).add(finding);
              });
    }
    return result;
  }

  private static CompilationUnit parse(Path path) {
    try {
      return StaticJavaParser.parse(path);
    } catch (java.io.IOException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }

  private static Optional<MethodDeclaration> enclosingMethod(CompilationUnit unit, int line) {
    return unit.findAll(MethodDeclaration.class).stream()
        .filter(m -> m.getBegin().isPresent() && m.getEnd().isPresent())
        .filter(m -> line >= m.getBegin().get().line && line <= m.getEnd().get().line)
        .findFirst();
  }
}
