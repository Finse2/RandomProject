/**
 * Token categories recognized by the J2RV lexer and consumed by the parser.
 */
public enum TokenType {
    // Keywords
    INT,
    PRINT,
    IF,
    ELSE,
    ELIF,

    // Literals and identifiers
    IDENTIFIER,
    INTEGER,

    // Arithmetic operators
    PLUS,
    MINUS,
    STAR,
    SLASH,

    // Assignment
    ASSIGN,

    // Comparison operators
    BANG_EQUAL,
    EQUAL_EQUAL,
    GREATER,
    GREATER_EQUAL,
    LESS,
    LESS_EQUAL,

    // Delimiters
    LEFT_PAREN,
    RIGHT_PAREN,
    LEFT_BRACE,
    RIGHT_BRACE,
    SEMICOLON,

    // End-of-file sentinel
    EOF
}
