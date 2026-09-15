package io.github.byreshb.tql.rules;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import io.github.byreshb.tql.model.Finding;
import io.github.byreshb.tql.model.Severity;
import io.github.byreshb.tql.rule.AbstractRule;
import io.github.byreshb.tql.rule.RuleContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * TQL012: a class named {@code FooTest} (or {@code FooTests}, {@code FooIT}, {@code FooTestCase},
 * {@code FooITCase}) creates a mock of {@code Foo} itself, through a Mockito or EasyMock
 * {@code @Mock} field or a {@code mock}/{@code spy}/{@code createMock} call. When the class under
 * test is replaced by a mock, the test exercises Mockito, not {@code Foo}.
 */
public final class MockOfTypeUnderTest extends AbstractRule {

  /** The rule id. */
  public static final String ID = "TQL012";

  private static final String FIX =
      "Instantiate the real class under test and mock its collaborators instead";

  private static final List<String> TEST_SUFFIXES =
      List.of("ITCase", "TestCase", "Tests", "Test", "IT");

  private static final Set<String> MOCK_ANNOTATIONS = Set.of("Mock", "Spy");

  private static final Set<String> MOCK_FACTORY_METHODS =
      Set.of("mock", "spy", "createMock", "createNiceMock", "createStrictMock");

  /** Creates the rule. */
  public MockOfTypeUnderTest() {
    super(
        ID,
        "MockOfTypeUnderTest",
        "The class named in the test class name is mocked instead of exercised.",
        Severity.WARN);
  }

  @Override
  public List<Finding> check(CompilationUnit unit, RuleContext context) {
    List<Finding> findings = new ArrayList<>();
    for (ClassOrInterfaceDeclaration type : unit.findAll(ClassOrInterfaceDeclaration.class)) {
      classUnderTest(type.getNameAsString())
          .ifPresent(target -> findings.addAll(checkType(type, target, context)));
    }
    return findings;
  }

  private List<Finding> checkType(
      ClassOrInterfaceDeclaration type, String target, RuleContext context) {
    List<Finding> findings = new ArrayList<>();
    for (FieldDeclaration field : type.getFields()) {
      if (field.getAnnotations().stream()
          .noneMatch(a -> MOCK_ANNOTATIONS.contains(a.getName().getIdentifier()))) {
        continue;
      }
      for (VariableDeclarator variable : field.getVariables()) {
        if (typeSimpleName(variable.getType()).equals(target)) {
          findings.add(
              context.finding(
                  this,
                  field,
                  "Field '"
                      + variable.getNameAsString()
                      + "' mocks "
                      + target
                      + ", the class"
                      + " under test",
                  FIX));
        }
      }
    }
    for (MethodCallExpr call : type.findAll(MethodCallExpr.class)) {
      if (!MOCK_FACTORY_METHODS.contains(call.getNameAsString())
          || call.getArguments().size() != 1) {
        continue;
      }
      Expression argument = call.getArgument(0);
      if (argument instanceof ClassExpr classExpr
          && typeSimpleName(classExpr.getType()).equals(target)) {
        findings.add(
            context.finding(
                this,
                call,
                call.getNameAsString() + "(" + target + ".class) mocks the class under test",
                FIX));
      }
    }
    return findings;
  }

  private static Optional<String> classUnderTest(String testClassName) {
    for (String suffix : TEST_SUFFIXES) {
      if (testClassName.length() > suffix.length() && testClassName.endsWith(suffix)) {
        return Optional.of(testClassName.substring(0, testClassName.length() - suffix.length()));
      }
    }
    return Optional.empty();
  }

  private static String typeSimpleName(Type type) {
    if (type instanceof ClassOrInterfaceType classType) {
      return classType.getNameAsString();
    }
    return type.asString();
  }
}
