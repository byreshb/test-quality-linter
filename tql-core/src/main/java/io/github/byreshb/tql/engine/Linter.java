package io.github.byreshb.tql.engine;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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
 *
 * <p>A finding suppressed by a trailing {@code // tql:ignore} comment or a
 * {@code @SuppressWarnings("tql:...")} annotation never reaches the result.
 *
 * <p>By default parsing is syntax-only: fast, and it needs nothing but the sources themselves.
 * Passing a classpath through {@link #Linter(RuleRegistry, RuleConfig, List)} turns on JavaParser's
 * {@link JavaSymbolSolver}, so rules that need to know a call's declared return type (there are
 * none among the rules of step 3, and {@code UnusedTestResult} among those added in step 4) can
 * resolve it. JDK types resolve through reflection even with an empty classpath list; a non-empty
 * list adds jars and directories of {@code .class} files from the project under test.
 */
public final class Linter {

  private final RuleRegistry registry;
  private final RuleConfig config;
  private final List<Rule> enabledRules;
  private final List<PathMatcher> excludes;
  private final JavaSymbolSolver symbolSolver;
  private final boolean symbolsResolved;

  /**
   * Creates a syntax-only linter: no classpath, so {@link RuleContext#symbolsResolved()} is false
   * for every file.
   *
   * @param registry the rules to choose from
   * @param config which of them run, with what severity and options, and which files to skip
   */
  public Linter(RuleRegistry registry, RuleConfig config) {
    this(registry, config, null);
  }

  /**
   * Creates a linter, optionally with symbol resolution.
   *
   * @param registry the rules to choose from
   * @param config which of them run, with what severity and options, and which files to skip
   * @param classpath jars and directories of {@code .class} files to resolve types against, in
   *     addition to the JDK; {@code null} disables symbol resolution entirely, while an empty list
   *     still enables it for JDK types
   */
  public Linter(RuleRegistry registry, RuleConfig config, List<Path> classpath) {
    this.registry = Objects.requireNonNull(registry, "registry");
    this.config = Objects.requireNonNull(config, "config");
    this.enabledRules = registry.enabled(config);
    this.excludes =
        config.excludes().stream()
            .map(glob -> FileSystems.getDefault().getPathMatcher("glob:" + glob))
            .toList();
    this.symbolsResolved = classpath != null;
    this.symbolSolver = symbolsResolved ? buildSymbolSolver(classpath) : null;
  }

  /**
   * A linter with every discovered rule enabled at its default severity and no symbol resolution.
   *
   * @return the linter
   */
  public static Linter withDefaults() {
    return new Linter(RuleRegistry.discover(), RuleConfig.defaults());
  }

  /**
   * A linter with every discovered rule enabled at its default severity, resolving types against
   * the given classpath in addition to the JDK.
   *
   * @param classpath jars and directories of {@code .class} files
   * @return the linter
   */
  public static Linter withClasspath(List<Path> classpath) {
    return new Linter(RuleRegistry.discover(), RuleConfig.defaults(), classpath);
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
   * Whether this linter resolves symbols, i.e. was built with a (possibly empty) classpath.
   *
   * @return true when a classpath was given
   */
  public boolean symbolsResolved() {
    return symbolsResolved;
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
    JavaParser parser = newParser();
    for (SourceFile source : sources) {
      ParseResult<CompilationUnit> parsed = parser.parse(source.content());
      if (!parsed.isSuccessful() || parsed.getResult().isEmpty()) {
        problems.add(toProblem(source, parsed));
        continue;
      }
      CompilationUnit unit = parsed.getResult().get();
      RuleContext context = new RuleContext(source, config, symbolsResolved);
      List<Finding> fileFindings = new ArrayList<>();
      for (Rule rule : enabledRules) {
        fileFindings.addAll(rule.check(unit, context));
      }
      findings.addAll(Suppressions.filter(unit, source, fileFindings));
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

  private JavaParser newParser() {
    ParserConfiguration configuration =
        new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
            .setLexicalPreservationEnabled(false);
    if (symbolSolver != null) {
      configuration.setSymbolResolver(symbolSolver);
    }
    return new JavaParser(configuration);
  }

  private static JavaSymbolSolver buildSymbolSolver(List<Path> classpath) {
    CombinedTypeSolver combined = new CombinedTypeSolver();
    combined.add(new ReflectionTypeSolver());
    URL[] urls = new URL[classpath.size()];
    for (int i = 0; i < classpath.size(); i++) {
      urls[i] = toUrl(classpath.get(i));
    }
    combined.add(
        new ClassLoaderTypeSolver(new URLClassLoader(urls, Linter.class.getClassLoader())));
    return new JavaSymbolSolver(combined);
  }

  private static URL toUrl(Path path) {
    try {
      return path.toUri().toURL();
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Invalid classpath entry: " + path, e);
    }
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
