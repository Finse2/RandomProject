import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Emits RISC-V assembly for a parsed J2RV program.
 *
 * <p>The generator uses temporary registers for expression evaluation and stack
 * slots for declared integer locals.
 */
public class CodeGenerator {
    private final Ast.Program program;
    private final StringBuilder output = new StringBuilder();
    private final Map<String, Integer> locals = new LinkedHashMap<String, Integer>();
    private int stackSize = 0;
    private int labelCounter = 0;

    /**
     * Creates a generator for an already parsed program.
     *
     * @param program AST root produced by the parser
     */
    public CodeGenerator(Ast.Program program) {
        this.program = program;
    }

    /**
     * Generates complete RISC-V assembly for the program.
     *
     * @return assembly text ending with an exit syscall
     */
    public String generate() {
        collectLocals();
        emitLine(".data");
        emitLine("newline: .asciz \"\\n\"");
        emitLine("");
        emitLine(".text");
        emitLine(".globl main");
        emitLine("");
        emitLine("main:");
        emitLine("    addi sp, sp, -" + stackSize);

        for (Ast.Stmt statement : program.statements) {
            emitStatement(statement);
        }

        emitLine("    li a0, 0");
        emitLine("    li a7, 93");
        emitLine("    ecall");
        return output.toString();
    }

    private void collectLocals() {
        // Reserve stack slots before emitting code so every local has a stable offset.
        for (Ast.Stmt statement : program.statements) {
            collectLocalsFromStatement(statement);
        }
        stackSize = alignStack(locals.size() * 4);
        if (stackSize == 0) {
            stackSize = 16;
        }
    }

    private int alignStack(int bytes) {
        // Keep the stack 16-byte aligned for the RISC-V calling convention.
        int aligned = ((bytes + 15) / 16) * 16;
        return aligned < 16 ? 16 : aligned;
    }

    private void collectLocalsFromStatement(Ast.Stmt statement) {
        if (statement instanceof Ast.IntDecl) {
            Ast.IntDecl declaration = (Ast.IntDecl) statement;
            if (!locals.containsKey(declaration.name)) {
                locals.put(declaration.name, locals.size() * 4);
            }
        } else if (statement instanceof Ast.IfStmt) {
            Ast.IfStmt ifStatement = (Ast.IfStmt) statement;
            collectLocalsFromStatement(ifStatement.thenBranch);
            if (ifStatement.elseBranch != null) {
                collectLocalsFromStatement(ifStatement.elseBranch);
            }
        }
    }

    private void emitStatement(Ast.Stmt statement) {
        if (statement instanceof Ast.IntDecl) {
            emitIntDecl((Ast.IntDecl) statement);
        } else if (statement instanceof Ast.PrintStmt) {
            emitPrint((Ast.PrintStmt) statement);
        } else if (statement instanceof Ast.IfStmt) {
            emitIf((Ast.IfStmt) statement);
        }
    }

    private void emitIf(Ast.IfStmt statement) {
        // Fresh labels keep nested conditionals from colliding.
        String elseLabel = newLabel("else");
        String endLabel = newLabel("endif");

        emitExpression(statement.condition);
        emitLine("    beqz t0, " + elseLabel);
        emitStatement(statement.thenBranch);
        emitLine("    j " + endLabel);
        emitLine(elseLabel + ":");
        if (statement.elseBranch != null) {
            emitStatement(statement.elseBranch);
        }
        emitLine(endLabel + ":");
    }

    private String newLabel(String prefix) {
        return prefix + "_" + labelCounter++;
    }

    private void emitIntDecl(Ast.IntDecl declaration) {
        if (declaration.initializer != null) {
            emitExpression(declaration.initializer);
        } else {
            emitLine("    li t0, 0");
        }
        emitLine("    sw t0, " + locals.get(declaration.name) + "(sp)");
    }

    private void emitPrint(Ast.PrintStmt statement) {
        emitExpression(statement.expression);
        // RARS/Venus convention: a7=1 prints an integer, a7=4 prints a string.
        emitLine("    mv a0, t0");
        emitLine("    li a7, 1");
        emitLine("    ecall");
        emitLine("    la a0, newline");
        emitLine("    li a7, 4");
        emitLine("    ecall");
    }

    private void emitExpression(Ast.Expr expression) {
        if (expression instanceof Ast.Literal) {
            emitLine("    li t0, " + ((Ast.Literal) expression).value);
        } else if (expression instanceof Ast.Variable) {
            String name = ((Ast.Variable) expression).name;
            emitLine("    lw t0, " + locals.get(name) + "(sp)");
        } else if (expression instanceof Ast.Binary) {
            emitBinary((Ast.Binary) expression);
        }
    }

    private void emitBinary(Ast.Binary binary) {
        emitExpression(binary.left);
        // Save the left operand while the right operand is evaluated into t0.
        emitLine("    mv t1, t0");
        emitExpression(binary.right);

        if (binary.operator == TokenType.PLUS) {
            emitLine("    add t0, t1, t0");
        } else if (binary.operator == TokenType.MINUS) {
            emitLine("    sub t0, t1, t0");
        } else if (binary.operator == TokenType.STAR) {
            emitLine("    mul t0, t1, t0");
        } else if (binary.operator == TokenType.SLASH) {
            emitLine("    div t0, t1, t0");
        } else if (binary.operator == TokenType.EQUAL_EQUAL) {
            emitLine("    sub t0, t1, t0");
            emitLine("    seqz t0, t0");
        } else if (binary.operator == TokenType.BANG_EQUAL) {
            emitLine("    sub t0, t1, t0");
            emitLine("    snez t0, t0");
        } else if (binary.operator == TokenType.LESS) {
            emitLine("    slt t0, t1, t0");
        } else if (binary.operator == TokenType.GREATER) {
            emitLine("    slt t0, t0, t1");
        } else if (binary.operator == TokenType.LESS_EQUAL) {
            emitLine("    slt t0, t0, t1");
            emitLine("    seqz t0, t0");
        } else if (binary.operator == TokenType.GREATER_EQUAL) {
            emitLine("    slt t0, t1, t0");
            emitLine("    seqz t0, t0");
        }
    }

    private void emitLine(String line) {
        output.append(line).append('\n');
    }
}
