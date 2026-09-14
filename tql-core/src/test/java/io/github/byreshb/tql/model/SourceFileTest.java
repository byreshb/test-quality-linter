package io.github.byreshb.tql.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceFileTest {

  @Test
  void returnsTrimmedLinesAndEmptyOutOfRange() {
    SourceFile file = SourceFile.of("A.java", "class A {\n   int x;  \n}\n");
    assertThat(file.line(2)).isEqualTo("int x;");
    assertThat(file.line(0)).isEmpty();
    assertThat(file.line(4)).isEmpty();
  }

  @Test
  void readsFromDisk(@TempDir Path dir) throws Exception {
    Path path = dir.resolve("B.java");
    Files.writeString(path, "class B {}\n");
    SourceFile file = SourceFile.read(path);
    assertThat(file.path()).isEqualTo(path);
    assertThat(file.content()).isEqualTo("class B {}\n");
  }

  @Test
  void wrapsReadFailures(@TempDir Path dir) {
    assertThatThrownBy(() -> SourceFile.read(dir.resolve("missing.java")))
        .isInstanceOf(UncheckedIOException.class)
        .hasMessageContaining("missing.java");
  }
}
