package lexer;

public enum TokenType {
    //keywords
    INT, IF, ELSE, ELSEIF, WHILE, DO, SWITCH, CASE, DEFAULT, BREAK,FOR,

    //identifiers
    IDENT, NUMBER,

    //operators
    ASSIGN, EQ, LT, GT, LTE ,GTE, NEQ, PLUS, MINUS, MUL , DIV, INC ,DEC,

    //SEPERATORS
    SEMICOLON, COMMA, LPAREN, RPAREN, LBRACE, RBRACE ,COLON,
    
    //SPECIAL
    EOF,
    UNKNOWN


}
