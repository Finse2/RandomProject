import java.util.List;

/**
 * Abstract syntax tree node definitions for J2RV programs.
 *
 * <p>The parser builds these lightweight immutable nodes, and the code
 * generator walks them to emit assembly.
 */
public class Ast {
    /**
     * Root node containing all top-level statements in source order.
     */
    public static class Program {
        public final List<Stmt> statements;

        /**
         * @param statements top-level statements in source order
         */
        public Program(List<Stmt> statements) {
            this.statements = statements;
        }
    }

    /**
     * Base type for all statement nodes.
     */
    public static abstract class Stmt {
    }

    /**
     * Integer variable declaration, optionally with an initializer expression.
     */
    public static class IntDecl extends Stmt {
        public final String name;
        public final Expr initializer;

        /**
         * @param name declared variable name
         * @param initializer expression assigned at declaration time, or null
         */
        public IntDecl(String name, Expr initializer) {
            this.name = name;
            this.initializer = initializer;
        }
    }

    /**
     * Statement that prints the value of an expression.
     */
    public static class PrintStmt extends Stmt {
        public final Expr expression;

        /**
         * @param expression expression to evaluate and print
         */
        public PrintStmt(Expr expression) {
            this.expression = expression;
        }
    }

    /**
     * Conditional statement with a required then branch and optional else branch.
     */
    public static class IfStmt extends Stmt {
        public final Expr condition;
        public final Stmt thenBranch;
        public final Stmt elseBranch;

        /**
         * @param condition expression treated as false when it evaluates to zero
         * @param thenBranch statement executed when the condition is non-zero
         * @param elseBranch statement executed otherwise, or null
         */
        public IfStmt(Expr condition, Stmt thenBranch, Stmt elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }
    }

    /**
     * Base type for all expression nodes.
     */
    public static abstract class Expr {
    }

    /**
     * Integer literal expression.
     */
    public static class Literal extends Expr {
        public final int value;

        /**
         * @param value integer value represented by the literal
         */
        public Literal(int value) {
            this.value = value;
        }
    }

    /**
     * Variable reference expression.
     */
    public static class Variable extends Expr {
        public final String name;

        /**
         * @param name referenced variable name
         */
        public Variable(String name) {
            this.name = name;
        }
    }

    /**
     * Binary expression such as addition, comparison, or equality.
     */
    public static class Binary extends Expr {
        public final Expr left;
        public final TokenType operator;
        public final Expr right;

        /**
         * @param left expression on the left side of the operator
         * @param operator binary operator token type
         * @param right expression on the right side of the operator
         */
        public Binary(Expr left, TokenType operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }
    }
}
