/**
 * Immutable representation of a lexical token.
 *
 * <p>A token stores its syntactic type, original source text, and source
 * position for diagnostics.
 */
public class Token {
    private final TokenType type;
    private final String lexeme;
    private final int line;
    private final int column;

    /**
     * Creates a token from the lexer output.
     *
     * @param type token category
     * @param lexeme exact source text that produced the token
     * @param line 1-based source line
     * @param column 1-based source column where the token starts
     */
    public Token(TokenType type, String lexeme, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.column = column;
    }

    /**
     * @return token category
     */
    public TokenType getType() {
        return type;
    }

    /**
     * @return exact source text that produced this token
     */
    public String getLexeme() {
        return lexeme;
    }

    /**
     * @return 1-based source line
     */
    public int getLine() {
        return line;
    }

    /**
     * @return 1-based source column where the token starts
     */
    public int getColumn() {
        return column;
    }

    @Override
    public String toString() {
        return type + " '" + lexeme + "' " + line + ":" + column;
    }
}
