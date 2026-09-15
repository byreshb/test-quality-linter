# Benchmark

`tql-benchmark` is a small sample project of hand-written tests: 24 planted clean and
23 planted weak, the weak ones written in the styles each rule targets and labelled with
the rule id(s) they are meant to trigger (see `@Weak` on the test methods under
`src/test/java`). This page reports what the linter actually finds against that known
answer, and what mutation testing with [PIT](https://pitest.org/) says about the tests
either way.

Regenerated on 2026-09-14 from a real run of both tools; see "Method" below to reproduce it.

## Method

```bash
mvn -pl tql-benchmark -am install -DskipTests
mvn -pl tql-benchmark org.pitest:pitest-maven:mutationCoverage
mvn -pl tql-benchmark exec:java@benchmark-report
```

The first command builds `tql-core` and the benchmark's classes. The second runs PIT
over `io.github.byreshb.tql.benchmark`, writing `target/pit-reports/mutations.xml` (each
mutant's `killingTest`, when detected, names the test method that caught it). The third
runs `tql`'s own `Linter` over the benchmark's test sources (with the benchmark's own
compiled classes on the classpath, so `TQL011` can resolve types), attributes each
finding to the test method it falls inside, and rewrites this file from both reports and
the planted `@Weak` ground truth.

## Findings per test

| Test | Planted as | tql found |
|---|---|---|
| `CalculatorTest#addCompletesWithoutError` | TQL001 | TQL001 |
| `CalculatorTest#addIsFastEnough` | TQL006 | TQL006 |
| `CalculatorTest#addMatchesArithmetic` | TQL002 | TQL002 |
| `CalculatorTest#addsNegativeNumbers` | - | - |
| `CalculatorTest#addsTwoNegativeNumbers` | TQL008 | TQL008 |
| `CalculatorTest#addsTwoNumbers` | - | - |
| `CalculatorTest#divideByZeroThrows` | - | - |
| `CalculatorTest#dividesTwoNumbers` | - | - |
| `CalculatorTest#handlesOverflow` | TQL009 | TQL009 |
| `CalculatorTest#isEvenChecksParity` | TQL010 | TQL010 |
| `CalculatorTest#maxReturnsTheLargerNumber` | - | - |
| `CalculatorTest#multipliesTwoNumbers` | - | - |
| `CalculatorTest#recognisesEvenNumbers` | - | - |
| `CalculatorTest#recognisesOddNumbers` | - | - |
| `CalculatorTest#subtractsTwoNumbers` | - | - |
| `InvoiceTest#anInvoiceWithLinesIsNotEmpty` | - | - |
| `InvoiceTest#anInvoiceWithNoLinesIsEmpty` | - | - |
| `InvoiceTest#appliesADiscount` | - | - |
| `InvoiceTest#computesTheTotal` | - | - |
| `InvoiceTest#computesTotalWithoutChecking` | TQL003, TQL011 | TQL003, TQL011 |
| `InvoiceTest#countsTheLines` | - | - |
| `InvoiceTest#discountValidatesRange` | TQL007, TQL011 | TQL007, TQL011 |
| `InvoiceTest#rejectsADiscountOutOfRange` | - | - |
| `OrderServiceTest#backlogSizeFromAMockedService` | TQL012 | TQL012 |
| `OrderServiceTest#backlogSizeReturnsTheRepositoryCount` | - | - |
| `OrderServiceTest#computesBacklogButIgnoresIt` | TQL011 | TQL011 |
| `OrderServiceTest#countsBacklogOrders` | TQL004 | TQL004 |
| `OrderServiceTest#handlesConcurrentOrderPlacement` | TQL009 | TQL009 |
| `OrderServiceTest#placedOrderEventuallyAppearsInBacklog` | TQL006 | TQL006 |
| `OrderServiceTest#placesAValidOrderAndReturnsTheSavedOrder` | - | - |
| `OrderServiceTest#placesAWidgetOrder` | - | - |
| `OrderServiceTest#placesAWidgetOrderAgain` | TQL008 | TQL008 |
| `OrderServiceTest#placesOrderAndOnlyVerifiesNoExtraInteractions` | TQL005 | TQL005 |
| `OrderServiceTest#placesOrderAndOnlyVerifiesSave` | TQL005 | TQL005 |
| `OrderServiceTest#placesOrderThroughAMockedService` | TQL012 | TQL012 |
| `OrderServiceTest#placingAnInvalidOrderIsHandledSilently` | TQL007 | TQL007 |
| `OrderServiceTest#placingAnInvalidOrderThrows` | - | - |
| `OrderServiceTest#savesAndAssertsStubbedOrder` | TQL004 | TQL004 |
| `PasswordValidatorTest#acceptsAStrongPassword` | - | - |
| `PasswordValidatorTest#checksPolicyConstant` | TQL002 | TQL002 |
| `PasswordValidatorTest#checksStrongPassword` | TQL003, TQL011 | TQL003, TQL011 |
| `PasswordValidatorTest#rejectsANullPassword` | - | - |
| `PasswordValidatorTest#rejectsAPasswordThatIsTooShort` | - | - |
| `PasswordValidatorTest#rejectsAPasswordWithoutADigit` | - | - |
| `PasswordValidatorTest#rejectsAPasswordWithoutAnUpperCaseLetter` | - | - |
| `PasswordValidatorTest#rejectsShortPasswords` | TQL010 | TQL010 |
| `PasswordValidatorTest#validatesLongPassword` | TQL001 | TQL001 |

## Precision and recall, per rule

Against the planted ground truth: a true positive is a rule firing on a test planted
for that rule; a false positive is a rule firing on a test not planted for it
(clean, or planted for a different rule); a false negative is a planted test the
rule missed.

| Rule | Planted | True positives | False positives | False negatives | Precision | Recall |
|---|---|---|---|---|---|---|
| TQL001 | 2 | 2 | 0 | 0 | 100% | 100% |
| TQL002 | 2 | 2 | 0 | 0 | 100% | 100% |
| TQL003 | 2 | 2 | 0 | 0 | 100% | 100% |
| TQL004 | 2 | 2 | 0 | 0 | 100% | 100% |
| TQL005 | 2 | 2 | 0 | 0 | 100% | 100% |
| TQL006 | 2 | 2 | 0 | 0 | 100% | 100% |
| TQL007 | 2 | 2 | 0 | 0 | 100% | 100% |
| TQL008 | 2 | 2 | 0 | 0 | 100% | 100% |
| TQL009 | 2 | 2 | 0 | 0 | 100% | 100% |
| TQL010 | 2 | 2 | 0 | 0 | 100% | 100% |
| TQL011 | 4 | 4 | 0 | 0 | 100% | 100% |
| TQL012 | 2 | 2 | 0 | 0 | 100% | 100% |

## Mutation score: flagged vs clean

Mutants killed per test, averaged, using PIT's `killingTest` on each detected mutant:

| Group | Tests | Avg. mutants killed |
|---|---|---|
| Tests tql flagged (at least one finding) | 23 | 0.30 |
| Tests tql left clean | 24 | 1.38 |
| Planted weak examples | 23 | 0.30 |
| Planted clean examples | 24 | 1.38 |

The flagged and planted-weak rows are expected to sit well below the clean rows: a test tql flags contributes fewer surviving-mutant kills than one it leaves alone, which is the headline this benchmark exists to demonstrate with a number rather than an assertion.
