package io.github.byreshb.tql.engine;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.LintResult;
import io.github.byreshb.tql.model.ParseProblem;
import io.github.byreshb.tql.model.SourceFile;
import io.github.byreshb.tql.rule.Rule;
import io.github.byreshb.tql.rule.RuleConfig;
import io.github.byreshb.tql.rule.RuleContext;
import io.github.byreshb.tql.rule.RuleRegistry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Parses test sources with JavaParser and runs the enabled rules over each of them.
 *
 * <p>A linter is immutable and safe to reuse: create one with a {@link RuleRegistry} and a {@link
 * RuleConfig}, then call {@link #lint(Collection)} for in-memory sources or {@link
 * #lintPaths(Collection)} for files and directories.
 */
public final class Linter {

  private final RuleRegistry registry;
  private final RuleConfig config;
  private final List<Rule> enabledRules;
  private final List<PathMatcher> excludes;

  /**
   * Creates a linter.
   *
   * @param registry the rules to choose from
   * @param config which of them run, with what severity and options, and which files to skip
   */
  public Linter(RuleRegistry registry, RuleConfig config) {
    this.registry = Objects.requireNonNull(registry, "registry");
    this.config = Objects.requireNonNull(config, "config");
    this.enabledRules = registry.enabled(config);
    this.excludes =
        config.excludes().stream()
            .map(glob -> FileSystems.getDefault().getPathMatcher("glob:" + glob))
            .toList();
  }

  /**
   * A linter with every discovered rule enabled at its default severity.
   *
   * @return the linter
   */
  public static Linter withDefaults() {
    return new Linter(RuleRegistry.discover(), RuleConfig.defaults());
  }

  /**
   * The registry this linter draws its rules from.
   *
   * @return the registry
   */
  public RuleRegistry registry() {
    return registry;
  }

  /**
   * The configuration this linter runs with.
   *
   * @return the configuration
   */
  public RuleConfig config() {
    return config;
  }

  /**
   * The rules that run, after applying the configuration.
   *
   * @return the enabled rules, sorted by id
   */
  public List<Rule> enabledRules() {
    return enabledRules;
  }

  /**
   * Lints files and directories. Directories are walked recursively for {@code .java} files; paths
   * matching an exclude glob are skipped.
   *
   * @param paths files or directories
   * @return the result over every file found
   * @throws UncheckedIOException when a directory cannot be walked or a file cannot be read
   */
  public LintResult lintPaths(Collection<Path> paths) {
    List<SourceFile> sources = new ArrayList<>();
    for (Path path : paths) {
      for (Path file : javaFilesUnder(path)) {
        if (!isExcluded(file)) {
          sources.add(SourceFile.read(file));
        }
      }
    }
    return lint(sources);
  }

  /**
   * Lints in-memory sources. Exclude globs are not applied here; the caller chose the sources.
   *
   * @param sources the sources
   * @return the result over every source
   */
  public LintResult lint(Collection<SourceFile> sources) {
    List<Finding> findings = new ArrayList<>();
    List<ParseProblem> problems = new ArrayList<>();
    for (SourceFile source : sources) {
      ParseResult<CompilationUnit> parsed = newParser().parse(source.content());
      if (!parsed.isSuccessful() || parsed.getResult().isEmpty()) {
        problems.add(toProblem(source, parsed));
        continue;
      }
      CompilationUnit unit = parsed.getResult().get();
      RuleContext context = new RuleContext(source, config, false);
      for (Rule rule : enabledRules) {
        findings.addAll(rule.check(unit, context));
      }
    }
    return new LintResult(sources.size(), findings, problems);
  }

  /**
   * Whether a path matches one of the configured exclude globs, either as a whole or by file name.
   *
   * @param path the path as it will be reported
   * @return true when the file should be skipped
   */
  public boolean isExcluded(Path path) {
    for (PathMatcher matcher : excludes) {
      if (matcher.matches(path) || matcher.matches(path.getFileName())) {
        return true;
      }
    }
    return false;
  }

  private static JavaParser newParser() {
    ParserConfiguration configuration =
        new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
            .setLexicalPreservationEnabled(false);
    return new JavaParser(configuration);
  }

  private static ParseProblem toProblem(SourceFile source, ParseResult<CompilationUnit> parsed) {
    Problem first = parsed.getProblems().isEmpty() ? null : parsed.getProblems().get(0);
    if (first == null) {
      return new ParseProblem(source.path(), 0, "Could not parse file");
    }
    int line =
        first.getLocation().flatMap(l -> l.getBegin().getRange()).map(r -> r.begin.line).orElse(0);
    return new ParseProblem(source.path(), line, first.getMessage());
  }

  private static List<Path> javaFilesUnder(Path path) {
    if (!Files.isDirectory(path)) {
      return List.of(path);
    }
    try (Stream<Path> walk = Files.walk(path)) {
      return walk.filter(Files::isRegularFile)
          .filter(p -> p.getFileName().toString().endsWith(".java"))
          .sorted()
          .toList();
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot walk " + path, e);
    }
  }
}
