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
| [TQL006](TQL006.md) | HardcodedSleep | WARN | A fixed sleep or timeout is used instead of waiting for a condition. |
| [TQL007](TQL007.md) | SwallowedException | WARN | A catch block in a test is empty or only prints or logs the exception. |

Rules planned for the next increments: TQL004 AssertsOnStub, TQL005 VerifyOnly,
TQL008 DuplicateTestBody, TQL009 DisabledWithoutReason, TQL010 ContradictoryMessage,
TQL011 UnusedTestResult, TQL012 MockOfTypeUnderTest.
