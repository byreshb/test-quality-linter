/** Decides which `.java` files are worth running `tql` on: test sources, not production code. */

/**
 * Whether a file path looks like a Java test source: under a `test` source root, or named like
 * a test class (`FooTest.java`, `TestFoo.java`, `FooTests.java`, `FooIT.java`).
 *
 * @param filePath - a filesystem path or URI path, forward-slash or backslash separated
 * @returns true when the file is worth linting
 */
export function isLikelyTestFile(filePath: string): boolean {
  const normalised = filePath.replace(/\\/g, '/');
  if (!normalised.endsWith('.java')) {
    return false;
  }
  if (/\/(src\/)?test\//.test(normalised)) {
    return true;
  }
  const fileName = normalised.slice(normalised.lastIndexOf('/') + 1, -'.java'.length);
  return /^Test[A-Z]/.test(fileName) || /(Test|Tests|IT|TestCase|ITCase)$/.test(fileName);
}
