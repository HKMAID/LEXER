package lexer;

import java.util.ArrayList;
import java.util.List;

public class Lexer {
    private final String input;
    private int position;
    private int line;
    private int column;

    public Lexer(String input) {
        this.input = input;
        this.position = 0;
        this.line = 1;
        this.column = 1;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (currentChar() != '\0') {
            skipWhiteSpace();

            // keep skipping comments + whitespace
            while (skipCommentIfAny()) {
                skipWhiteSpace();
            }

            if (currentChar() == '\0') break;

            if (Character.isLetter(currentChar()) || currentChar() == '_') {
                tokens.add(readIdentifierOrKeyword());
            } else if (Character.isDigit(currentChar())) {
                tokens.add(readNumber());
            } else {
                tokens.add(readOperatorOrSeparator());
            }
        }

        tokens.add(new Token(TokenType.EOF, "", line, column));
        return tokens;
    }

    private char currentChar() {
        if (position >= input.length()) {
            return '\0';
        }
        return input.charAt(position);
    }

    private void advance() {
        if (currentChar() == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        position++;
    }

    private void skipWhiteSpace() {
        while (Character.isWhitespace(currentChar())) {
            advance();
        }
    }
    private Token readIdentifierOrKeyword() {
        int startColumn = column;
        int startLine = line;

        StringBuilder sb = new StringBuilder();
        while (Character.isLetterOrDigit(currentChar()) || currentChar() == '_') {
            sb.append(currentChar());
            advance();
        }

        String text = sb.toString();

        // Special case: "else if" => ELSEIF token
        if ("else".equals(text)) {
            // Save current lexer position (we are right after "else")
            int savedPos = position;
            int savedLine = line;
            int savedCol = column;

            // Skip whitespace between else and if
            while (Character.isWhitespace(currentChar())) {
                advance();
            }

            // Check if next identifier is "if"
            if (Character.isLetter(currentChar()) || currentChar() == '_') {
                StringBuilder sb2 = new StringBuilder();
                while (Character.isLetterOrDigit(currentChar()) || currentChar() == '_') {
                    sb2.append(currentChar());
                    advance();
                }

                if ("if".contentEquals(sb2)) {
                    return new Token(TokenType.ELSEIF, "else if", startLine, startColumn);
                }
            }

            // Not "else if" => rollback, return ELSE only
            position = savedPos;
            line = savedLine;
            column = savedCol;

            return new Token(TokenType.ELSE, text, startLine, startColumn);
        }

        return switch (text) {
            case "int" -> new Token(TokenType.INT, text, startLine, startColumn);
            case "if" -> new Token(TokenType.IF, text, startLine, startColumn);
            case "while" -> new Token(TokenType.WHILE, text, startLine, startColumn);
            case "for" -> new Token(TokenType.FOR, text, startLine, startColumn);
            case "do" -> new Token(TokenType.DO, text, startLine, startColumn);
            case "switch" -> new Token(TokenType.SWITCH, text, startLine, startColumn);
            case "case" -> new Token(TokenType.CASE, text, startLine, startColumn);
            case "default" -> new Token(TokenType.DEFAULT, text, startLine, startColumn);
            case "break" -> new Token(TokenType.BREAK, text, startLine, startColumn);
            default -> new Token(TokenType.IDENT, text, startLine, startColumn);
        };
    }
    private Token readNumber() {
        int startColumn = column;
        int startLine = line;
        StringBuilder sb = new StringBuilder();

        while (Character.isDigit(currentChar())) {
            sb.append(currentChar());
            advance();
        }

        return new Token(TokenType.NUMBER, sb.toString(), startLine, startColumn);
    }

    private char peekNextChar() {
        if (position + 1 >= input.length()) return '\0';
        return input.charAt(position + 1);
    }

    // Skip //... or /*...*/ if present. Returns true if it skipped a comment.
    private boolean skipCommentIfAny() {
        if (currentChar() == '/' && peekNextChar() == '/') {
            // single-line: consume until newline or EOF
            advance(); // '/'
            advance(); // '/'
            while (currentChar() != '\n' && currentChar() != '\0') {
                advance();
            }
            return true;
        }

        if (currentChar() == '/' && peekNextChar() == '*') {
            // multi-line: consume until */
            advance(); // '/'
            advance(); // '*'
            while (currentChar() != '\0') {
                if (currentChar() == '*' && peekNextChar() == '/') {
                    advance(); // '*'
                    advance(); // '/'
                    return true;
                }
                advance();
            }
            throw new RuntimeException("Unterminated block comment at " + line + ":" + column);
        }

        return false;
    }

    private Token readOperatorOrSeparator() {
        int startColumn = column;
        char c = currentChar();

        switch (c) {
            case '+' -> {
                advance();
                if (currentChar() == '+') {
                    advance();
                    return new Token(TokenType.INC, "++", line, startColumn);
                }
                return new Token(TokenType.PLUS, "+", line, startColumn);
            }
            case '-' -> {
                advance();
                if (currentChar() == '-') {
                    advance();
                    return new Token(TokenType.DEC, "--", line, startColumn);
                }
                return new Token(TokenType.MINUS, "-", line, startColumn);
            }
            case '*' -> {
                advance();
                return new Token(TokenType.MUL, "*", line, startColumn);
            }
            case '/' -> {
                advance();
                return new Token(TokenType.DIV, "/", line, startColumn);
            }
            case '=' -> {
                advance();
                if (currentChar() == '=') {
                    advance();
                    return new Token(TokenType.EQ, "==", line, startColumn);
                }
                return new Token(TokenType.ASSIGN, "=", line, startColumn);
            }
            case '!' -> {
                advance();
                if (currentChar() == '=') {
                    advance();
                    return new Token(TokenType.NEQ, "!=", line, startColumn);
                }
                return new Token(TokenType.UNKNOWN, "!", line, startColumn);
            }
            case '>' -> {
                advance();
                if (currentChar() == '=') {
                    advance();
                    return new Token(TokenType.GTE, ">=", line, startColumn);
                }
                return new Token(TokenType.GT, ">", line, startColumn);
            }
            case '<' -> {
                advance();
                if (currentChar() == '=') {
                    advance();
                    return new Token(TokenType.LTE, "<=", line, startColumn);
                }
                return new Token(TokenType.LT, "<", line, startColumn);
            }
            case ';' -> {
                advance();
                return new Token(TokenType.SEMICOLON, ";", line, startColumn);
            }
            case ',' -> {
                advance();
                return new Token(TokenType.COMMA, ",", line, startColumn);
            }
            case '(' -> {
                advance();
                return new Token(TokenType.LPAREN, "(", line, startColumn);
            }
            case ')' -> {
                advance();
                return new Token(TokenType.RPAREN, ")", line, startColumn);
            }
            case '{' -> {
                advance();
                return new Token(TokenType.LBRACE, "{", line, startColumn);
            }
            case '}' -> {
                advance();
                return new Token(TokenType.RBRACE, "}", line, startColumn);
            }
            case ':' -> {
                advance();
                return new Token(TokenType.COLON, ":", line, startColumn);
            }
            default -> {
                advance();
                return new Token(TokenType.UNKNOWN, String.valueOf(c), line, startColumn);
            }
        }
    }
}