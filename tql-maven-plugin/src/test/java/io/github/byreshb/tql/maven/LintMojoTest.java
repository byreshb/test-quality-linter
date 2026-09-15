package io.github.byreshb.tql.maven;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LintMojoTest {

  private static final String BAD_TEST =
      """
      import static org.junit.jupiter.api.Assertions.assertEquals;
      import org.junit.jupiter.api.Test;

      class BadTest {
        @Test
        void same() {
          int x = 1;
          assertEquals(x, x);
        }
      }
      """;

  private static final String GOOD_TEST =
      """
      import static org.junit.jupiter.api.Assertions.assertEquals;
      import org.junit.jupiter.api.Test;

      class GoodTest {
        @Test
        void sum() {
          assertEquals(2, Math.addExact(1, 1));
        }
      }
      """;

  private static LintMojo mojo(Path dir) {
    LintMojo mojo = new LintMojo();
    mojo.setLog(new SystemStreamLog());
    mojo.testSourceDirectory = dir.resolve("src/test/java").toFile();
    mojo.configFile = dir.resolve(".tql.yaml").toFile();
    mojo.outputDirectory = dir.resolve("target/tql").toFile();
    mojo.failOnSeverity = "ERROR";
    mojo.skip = false;
    return mojo;
  }

  private static Path writeTestSources(Path dir, String fileName, String content) throws Exception {
    Path testSrc = Files.createDirectories(dir.resolve("src/test/java"));
    Files.writeString(testSrc.resolve(fileName), content);
    return testSrc;
  }

  @Test
  void failsTheBuildOnAnErrorFindingAndWritesSarif(@TempDir Path dir) throws Exception {
    writeTestSources(dir, "BadTest.java", BAD_TEST);
    LintMojo mojo = mojo(dir);

    assertThatThrownBy(mojo::execute)
        .isInstanceOf(MojoFailureException.class)
        .hasMessageContaining("1 finding")
        .hasMessageContaining("tql.sarif");

    Path sarif = dir.resolve("target/tql/tql.sarif");
    assertThat(sarif).exists();
    assertThat(Files.readString(sarif)).contains("\"TQL001\"");
  }

  @Test
  void passesOnCleanSources(@TempDir Path dir) throws Exception {
    writeTestSources(dir, "GoodTest.java", GOOD_TEST);
    LintMojo mojo = mojo(dir);
    mojo.execute();
    assertThat(dir.resolve("target/tql/tql.sarif")).exists();
  }

  @Test
  void skipsWhenSkipIsTrue(@TempDir Path dir) throws Exception {
    writeTestSources(dir, "BadTest.java", BAD_TEST);
    LintMojo mojo = mojo(dir);
    mojo.skip = true;
    mojo.execute();
    assertThat(dir.resolve("target/tql/tql.sarif")).doesNotExist();
  }

  @Test
  void skipsGracefullyWhenTestSourceDirectoryIsMissing(@TempDir Path dir) throws Exception {
    LintMojo mojo = mojo(dir);
    mojo.execute();
    assertThat(dir.resolve("target/tql/tql.sarif")).doesNotExist();
  }

  @Test
  void honoursAnExplicitConfigFileThatDisablesARule(@TempDir Path dir) throws Exception {
    writeTestSources(dir, "BadTest.java", BAD_TEST);
    Files.writeString(dir.resolve(".tql.yaml"), "rules:\n  TQL001:\n    enabled: false\n");
    LintMojo mojo = mojo(dir);
    mojo.execute();
  }

  @Test
  void raisingFailOnSeverityAllowsWarnFindingsThrough(@TempDir Path dir) throws Exception {
    String noAssertionTest =
        """
        import org.junit.jupiter.api.Test;

        class NoAssertionTest {
          @Test
          void t() {
            int x = 1;
          }
        }
        """;
    writeTestSources(dir, "NoAssertionTest.java", noAssertionTest);
    LintMojo mojo = mojo(dir);
    mojo.failOnSeverity = "ERROR";
    mojo.execute();
  }

  @Test
  void failsWhenAFileCannotBeParsedEvenWithoutFindings(@TempDir Path dir) throws Exception {
    writeTestSources(dir, "Broken.java", "class { oops");
    LintMojo mojo = mojo(dir);
    assertThatThrownBy(mojo::execute).isInstanceOf(MojoFailureException.class);
  }

  @Test
  void wrapsAnUnwritableOutputDirectoryAsMojoExecutionException(@TempDir Path dir)
      throws Exception {
    writeTestSources(dir, "GoodTest.java", GOOD_TEST);
    Path blocker = dir.resolve("target/tql");
    Files.createDirectories(dir.resolve("target"));
    Files.writeString(blocker, "not a directory");
    LintMojo mojo = mojo(dir);
    assertThatThrownBy(mojo::execute).isInstanceOf(MojoExecutionException.class);
  }
}
