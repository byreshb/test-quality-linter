package io.github.byreshb.tql.rule;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Decides whether an expression is a compile-time constant: a literal, an arithmetic, logical or
 * string expression over constants, or a {@code static final} field of the same file whose
 * initialiser is itself constant.
 */
public final class ConstantExpressions {

  private ConstantExpressions() {}

  /**
   * Whether an expression is a compile-time constant.
   *
   * @param expression the expression
   * @param unit the file it appears in, used to look up constant fields
   * @return true when the value is fixed at compile time
   */
  public static boolean isConstant(Expression expression, CompilationUnit unit) {
    return isConstant(expression, constantFields(unit), new HashSet<>());
  }

  /**
   * Strips redundant parentheses.
   *
   * @param expression the expression
   * @return the innermost expression
   */
  public static Expression unwrap(Expression expression) {
    Expression current = expression;
    while (current instanceof EnclosedExpr enclosed) {
      current = enclosed.getInner();
    }
    return current;
  }

  private static boolean isConstant(
      Expression expression, Map<String, Expression> fields, Set<String> visiting) {
    Expression e = unwrap(expression);
    if (e instanceof LiteralExpr) {
      return true;
    }
    if (e instanceof UnaryExpr unary) {
      return isConstant(unary.getExpression(), fields, visiting);
    }
    if (e instanceof BinaryExpr binary) {
      return isConstant(binary.getLeft(), fields, visiting)
          && isConstant(binary.getRight(), fields, visiting);
    }
    if (e instanceof ConditionalExpr conditional) {
      return isConstant(conditional.getCondition(), fields, visiting)
          && isConstant(conditional.getThenExpr(), fields, visiting)
          && isConstant(conditional.getElseExpr(), fields, visiting);
    }
    if (e instanceof CastExpr cast) {
      return isConstant(cast.getExpression(), fields, visiting);
    }
    Optional<String> name = fieldName(e);
    if (name.isPresent() && fields.containsKey(name.get()) && visiting.add(name.get())) {
      return isConstant(fields.get(name.get()), fields, visiting);
    }
    return false;
  }

  private static Optional<String> fieldName(Expression e) {
    if (e instanceof NameExpr nameExpr) {
      return Optional.of(nameExpr.getNameAsString());
    }
    if (e instanceof FieldAccessExpr access
        && (access.getScope().isThisExpr()
            || (access.getScope() instanceof NameExpr scope
                && Character.isUpperCase(scope.getNameAsString().charAt(0))))) {
      return Optional.of(access.getNameAsString());
    }
    return Optional.empty();
  }

  private static Map<String, Expression> constantFields(CompilationUnit unit) {
    Map<String, Expression> fields = new java.util.HashMap<>();
    for (FieldDeclaration field : unit.findAll(FieldDeclaration.class)) {
      if (!field.hasModifier(Modifier.Keyword.FINAL)) {
        continue;
      }
      for (VariableDeclarator variable : field.getVariables()) {
        variable.getInitializer().ifPresent(init -> fields.put(variable.getNameAsString(), init));
      }
    }
    return fields;
  }
}
