import { describe, expect, it } from 'vitest';
import { isLikelyTestFile } from './testFile.js';

describe('isLikelyTestFile', () => {
  it('accepts files under a test source root', () => {
    expect(isLikelyTestFile('/repo/src/test/java/com/acme/OrderServiceTest.java')).toBe(true);
    expect(isLikelyTestFile('/repo/test/java/Foo.java')).toBe(true);
  });

  it('accepts files named like a test class regardless of location', () => {
    expect(isLikelyTestFile('/repo/src/main/java/OrderServiceTest.java')).toBe(true);
    expect(isLikelyTestFile('/repo/TestOrderService.java')).toBe(true);
    expect(isLikelyTestFile('/repo/OrderServiceTests.java')).toBe(true);
    expect(isLikelyTestFile('/repo/OrderServiceIT.java')).toBe(true);
  });

  it('rejects ordinary production classes', () => {
    expect(isLikelyTestFile('/repo/src/main/java/com/acme/OrderService.java')).toBe(false);
  });

  it('rejects non-Java files', () => {
    expect(isLikelyTestFile('/repo/src/test/java/notes.txt')).toBe(false);
  });

  it('normalises backslash-separated paths', () => {
    expect(isLikelyTestFile('C:\\repo\\src\\test\\java\\FooTest.java')).toBe(true);
  });
});
