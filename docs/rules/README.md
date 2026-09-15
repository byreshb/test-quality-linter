# Rules

Every rule has a stable id, a default severity and a page describing what it catches, a bad
example, a fixed example and its options. Severities can be overridden and rules disabled in
`.tql.yaml`; see [docs/configuration.md](../configuration.md) once that page exists (delivery
step 5).

| Id | Name | Default severity | What it catches |
|----|------|------------------|-----------------|
| [TQL001](TQL001.md) | TautologicalAssertion | ERROR | An assertion compares an expression with itself or can never fail. |
| [TQL002](TQL002.md) | ConstantAssertion | ERROR | Both sides of an assertion are literals or compile-time constants. |
| [TQL003](TQL003.md) | NoAssertion | WARN | A test method has no assertion, verification or expected exception. |
| [TQL004](TQL004.md) | AssertsOnStub | ERROR | An assertion compares a mock's return value to the literal it was stubbed with. |
| [TQL005](TQL005.md) | VerifyOnly | WARN | A test contains only mock verifications and no assertion on an observable outcome. |
| [TQL006](TQL006.md) | HardcodedSleep | WARN | A fixed sleep or timeout is used instead of waiting for a condition. |
| [TQL007](TQL007.md) | SwallowedException | WARN | A catch block in a test is empty or only prints or logs the exception. |
| [TQL008](TQL008.md) | DuplicateTestBody | WARN | Two test methods in the same class have identical bodies. |
| [TQL009](TQL009.md) | DisabledWithoutReason | WARN | @Disabled or @Ignore has no reason, or has stood disabled past the configured age. |
| [TQL010](TQL010.md) | ContradictoryMessage | INFO | An assertion's message says the opposite of what the assertion checks. |
| [TQL011](TQL011.md) | UnusedTestResult | WARN | A call's return value is discarded as a bare statement and never checked. |
| [TQL012](TQL012.md) | MockOfTypeUnderTest | WARN | The class named in the test class name is mocked instead of exercised. |

Every rule of the planned set now exists. See [../configuration.md](../configuration.md) for
`.tql.yaml`, severity overrides, options and suppressions.

TQL011 needs symbol resolution (a classpath) to work; without one it reports nothing rather than
guess a call's return type. Every other rule works from syntax alone.
