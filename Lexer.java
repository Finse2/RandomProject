import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts J2RV source text into a sequence of tokens.
 *
 * <p>The lexer keeps line and column information on each token so parser errors
 * can point back to the original source location.
 */
public class Lexer {
    private static final Map<String, TokenType> keywords = new HashMap<>();

    static {
        keywords.put("int", TokenType.INT);
        keywords.put("print", TokenType.PRINT);
        keywords.put("if", TokenType.IF);
        keywords.put("else", TokenType.ELSE);
        keywords.put("elif", TokenType.ELIF);
    }

    private final String source;
    private final List<Token> tokens;

    private int start;
    private int current;
    private int line;
    private int column;
    private int tokenColumn;

    /**
     * Creates a lexer for a single source file.
     *
     * @param source full source text to scan
     */
    public Lexer(String source) {
        this.source = source;
        this.tokens = new ArrayList<>();

        this.start = 0;
        this.current = 0;
        this.line = 1;
        this.column = 1;
        this.tokenColumn = 1;
    }

    /**
     * Scans the complete source and returns all tokens, including a final EOF.
     *
     * @return ordered list of tokens from the source text
     */
    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            // Mark the beginning of the next token before consuming characters.
            start = current;
            tokenColumn = column;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", line, column));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case ' ':
            case '\r':
            case '\t':
            case '\n':
                break;
            case '(':
                addToken(TokenType.LEFT_PAREN);
                break;
            case ')':
                addToken(TokenType.RIGHT_PAREN);
                break;
            case '{':
                addToken(TokenType.LEFT_BRACE);
                break;
            case '}':
                addToken(TokenType.RIGHT_BRACE);
                break;
            case ';':
                addToken(TokenType.SEMICOLON);
                break;
            case '+':
                addToken(TokenType.PLUS);
                break;
            case '-':
                addToken(TokenType.MINUS);
                break;
            case '*':
                addToken(TokenType.STAR);
                break;
            case '/':
                addToken(TokenType.SLASH);
                break;
            case '=':
                if (match('=')) {
                    addToken(TokenType.EQUAL_EQUAL);
                } else {
                    addToken(TokenType.ASSIGN);
                }
                break;
            case '!':
                if (match('=')) {
                    addToken(TokenType.BANG_EQUAL);
                } else {
                    System.err.println("Unexpected character '!' at line " + line + ", column " + column);
                }
                break;
            case '<':
                if (match('=')) {
                    addToken(TokenType.LESS_EQUAL);
                } else {
                    addToken(TokenType.LESS);
                }
                break;
            case '>':
                if (match('=')) {
                    addToken(TokenType.GREATER_EQUAL);
                } else {
                    addToken(TokenType.GREATER);
                }
                break;
            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    System.err.println("Unexpected character '" + c + "' at line " + line + ", column " + column);
                }
                break;
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) {
            advance();
        }
        String text = source.substring(start, current);
        TokenType type = keywords.getOrDefault(text, TokenType.IDENTIFIER);
        addToken(type);
    }

    private void number() {
        while (isDigit(peek())) {
            advance();
        }
        addToken(TokenType.INTEGER);
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        char c = source.charAt(current++);
        // Keep diagnostics source-oriented rather than index-oriented.
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    private char peek() {
        if (isAtEnd()) {
            return '\0';
        }
        return source.charAt(current);
    }

    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(current) != expected) {
            return false;
        }
        advance();
        return true;
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
               c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private void addToken(TokenType type) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, line, tokenColumn));
    }
}
