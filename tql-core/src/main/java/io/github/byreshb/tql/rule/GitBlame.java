package io.github.byreshb.tql.rule;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

/**
 * Looks up when a line of a file was last changed, so a rule can tell how long something has stood
 * as it is. {@link io.github.byreshb.tql.rules.DisabledWithoutReason} uses it to flag a
 * long-dormant {@code @Disabled}.
 */
public interface GitBlame {

  /**
   * When a line was last changed, according to git.
   *
   * @param file the file, which must exist on disk inside a git working tree for this to succeed
   * @param line the 1-based line number
   * @return the commit time of the last change to that line, or empty when the file is not on disk,
   *     not inside a git repository, {@code git} is not installed, or the lookup otherwise fails
   */
  Optional<Instant> lastChanged(Path file, int line);
}
