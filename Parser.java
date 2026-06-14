import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Recursive-descent parser for the J2RV language.
 *
 * <p>The parser consumes lexer tokens, validates declarations and variable
 * references, and produces an AST for the code generator.
 */
public class Parser {
    private final List<Token> tokens;
    private int current = 0;
    private final Set<String> declaredVariables = new HashSet<String>();

    /**
     * Creates a parser over a token stream.
     *
     * @param tokens tokens produced by {@link Lexer#scanTokens()}
     */
    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * Parses a complete program.
     *
     * @return AST root for the parsed program
     * @throws RuntimeException when the token stream does not match the grammar
     */
    public Ast.Program parse() {
        List<Ast.Stmt> statements = new ArrayList<Ast.Stmt>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return new Ast.Program(statements);
    }

    private Ast.Stmt declaration() {
        if (match(TokenType.INT)) {
            Token name = consume(TokenType.IDENTIFIER, "Expect variable name after 'int'.");
            if (declaredVariables.contains(name.getLexeme())) {
                throw error(name, "Variable '" + name.getLexeme() + "' is already declared.");
            }
            // Track declarations immediately so later expressions can reference them.
            declaredVariables.add(name.getLexeme());

            Ast.Expr initializer = null;
            if (match(TokenType.ASSIGN)) {
                initializer = expression();
            }
            consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
            return new Ast.IntDecl(name.getLexeme(), initializer);
        }

        if (match(TokenType.PRINT)) {
            consume(TokenType.LEFT_PAREN, "Expect '(' after 'print'.");
            Ast.Expr value = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            consume(TokenType.SEMICOLON, "Expect ';' after print statement.");
            return new Ast.PrintStmt(value);
        }

        if (match(TokenType.IF)) {
            return ifStatement();
        }

        throw error(peek(), "Expect variable declaration, print, or if statement.");
    }

    private Ast.Stmt ifStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
        Ast.Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.");
        Ast.Stmt thenBranch = declaration();
        Ast.Stmt elseBranch = parseIfTail();
        return new Ast.IfStmt(condition, thenBranch, elseBranch);
    }

    private Ast.Stmt parseIfTail() {
        // Represent elif as an else branch containing another IfStmt.
        if (match(TokenType.ELIF)) {
            consume(TokenType.LEFT_PAREN, "Expect '(' after 'elif'.");
            Ast.Expr condition = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after elif condition.");
            Ast.Stmt thenBranch = declaration();
            Ast.Stmt elseBranch = parseIfTail();
            return new Ast.IfStmt(condition, thenBranch, elseBranch);
        }

        if (match(TokenType.ELSE)) {
            return declaration();
        }

        return null;
    }

    private Ast.Expr expression() {
        // Start at the lowest-precedence expression level.
        return equality();
    }

    private Ast.Expr equality() {
        Ast.Expr expr = comparison();

        // Left-associative chain of == and != comparisons.
        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            TokenType operator = previous().getType();
            Ast.Expr right = comparison();
            expr = new Ast.Binary(expr, operator, right);
        }

        return expr;
    }

    private Ast.Expr comparison() {
        Ast.Expr expr = term();

        // Comparisons bind less tightly than arithmetic terms.
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            TokenType operator = previous().getType();
            Ast.Expr right = term();
            expr = new Ast.Binary(expr, operator, right);
        }

        return expr;
    }

    private Ast.Expr term() {
        Ast.Expr expr = factor();

        // Addition and subtraction bind less tightly than multiplication.
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            TokenType operator = previous().getType();
            Ast.Expr right = factor();
            expr = new Ast.Binary(expr, operator, right);
        }

        return expr;
    }

    private Ast.Expr factor() {
        Ast.Expr expr = unary();

        // Multiplication and division share the same precedence.
        while (match(TokenType.STAR, TokenType.SLASH)) {
            TokenType operator = previous().getType();
            Ast.Expr right = unary();
            expr = new Ast.Binary(expr, operator, right);
        }

        return expr;
    }

    private Ast.Expr unary() {
        if (match(TokenType.MINUS)) {
            // Model unary minus as subtraction from zero to reuse binary codegen.
            return new Ast.Binary(new Ast.Literal(0), TokenType.MINUS, unary());
        }

        return primary();
    }

    private Ast.Expr primary() {
        if (match(TokenType.INTEGER)) {
            return new Ast.Literal(Integer.parseInt(previous().getLexeme()));
        }

        if (match(TokenType.IDENTIFIER)) {
            String name = previous().getLexeme();
            // Catch undefined variables during parsing instead of code generation.
            if (!declaredVariables.contains(name)) {
                throw error(previous(), "Undefined variable '" + name + "'.");
            }
            return new Ast.Variable(name);
        }

        if (match(TokenType.LEFT_PAREN)) {
            Ast.Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return expr;
        }

        throw error(peek(), "Expect expression.");
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return peek().getType() == type;
    }

    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    private boolean isAtEnd() {
        return peek().getType() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private RuntimeException error(Token token, String message) {
        return new RuntimeException(message + " at line " + token.getLine() + ", column " + token.getColumn() + ".");
    }
}
