package io.github.byreshb.tql.cli;

import picocli.CommandLine.IVersionProvider;

/** Reports the linter's own version for {@code tql --version}. */
final class ManifestVersionProvider implements IVersionProvider {

  @Override
  public String[] getVersion() {
    return new String[] {"tql " + Cli.version()};
  }
}
