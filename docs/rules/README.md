# Rules

Every rule has a stable id, a default severity and a page describing what it catches, a bad
example, a fixed example and its options. Severities can be overridden and rules disabled in
`.tql.yaml`; see [docs/configuration.md](../configuration.md) once that page exists (delivery
step 5).

| Id | Name | Default severity | What it catches |
|----|------|------------------|-----------------|
| [TQL001](TQL001.md) | TautologicalAssertion | ERROR | An assertion compares an expression with itself or can never fail. |

Rules planned for the next increments: TQL002 ConstantAssertion, TQL003 NoAssertion,
TQL004 AssertsOnStub, TQL005 VerifyOnly, TQL006 HardcodedSleep, TQL007 SwallowedException,
TQL008 DuplicateTestBody, TQL009 DisabledWithoutReason, TQL010 ContradictoryMessage,
TQL011 UnusedTestResult, TQL012 MockOfTypeUnderTest.
