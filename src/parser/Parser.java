package parser;

import lexer.Token;
import lexer.TokenType;

import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int position;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.position = 0;
    }

    // ---------------------------
    // Core helpers
    // ---------------------------

    private Token currentToken() {
        if (position >= tokens.size()) return tokens.get(tokens.size() - 1);
        return tokens.get(position);
    }

    private TokenType peekType() {
        return currentToken().getType();
    }

    private void advance() {
        if (position < tokens.size()) position++;
    }

    private boolean match(TokenType type) {
        if (peekType() == type) {
            advance();
            return true;
        }
        return false;
    }

    private void expect(TokenType type, String message) {
        if (!match(type)) error(message);
    }

    private void error(String message) {
        throw new RuntimeException(message + " at token " + currentToken());
    }

    // ---------------------------
    // Entry
    // ---------------------------

    public void parse() {
        while (peekType() != TokenType.EOF) {
            parseStatement();
        }
    }

    // ---------------------------
    // Statements
    // ---------------------------

    private void parseStatement() {
        switch (peekType()) {
            case INT -> parseDeclarationStatement();
            case IDENT -> parseIdentifierStatement();
            case IF -> parseIfStatement();
            case WHILE -> parseWhileStatement();
            case DO -> parseDoWhileStatement();
            case FOR -> parseForStatement();
            case SWITCH -> parseSwitchStatement();
            case BREAK -> parseBreakStatement();
            case LBRACE -> parseBlock();
            default -> error("Expected statement");
        }
    }

    private void parseBlock() {
        expect(TokenType.LBRACE, "Expected '{'");

        while (peekType() != TokenType.RBRACE && peekType() != TokenType.EOF) {
            parseStatement();
        }

        expect(TokenType.RBRACE, "Expected '}'");
    }

    private void parseStatementOrBlock() {
        if (peekType() == TokenType.LBRACE) parseBlock();
        else parseStatement();
    }

    // int x; OR int x = expr;
    private void parseDeclarationStatement() {
        expect(TokenType.INT, "Expected 'int'");
        expect(TokenType.IDENT, "Expected identifier");

        if (match(TokenType.ASSIGN)) {
            parseExpression();
        }

        expect(TokenType.SEMICOLON, "Expected ';' after declaration");
    }

    // Handles:
    //   x = expr; x++; x--;
    private void parseIdentifierStatement() {
        expect(TokenType.IDENT, "Expected identifier");

        if (match(TokenType.ASSIGN)) {
            parseExpression();
            expect(TokenType.SEMICOLON, "Expected ';' after assignment");
            return;
        }

        if (match(TokenType.INC) || match(TokenType.DEC)) {
            expect(TokenType.SEMICOLON, "Expected ';' after ++/--");
            return;
        }

        error("Expected '=', '++', or '--' after identifier");
    }

    private void parseBreakStatement() {
        expect(TokenType.BREAK, "Expected 'break'");
        expect(TokenType.SEMICOLON, "Expected ';' after break");
    }

    private void parseIfStatement() {
        expect(TokenType.IF, "Expected 'if'");
        expect(TokenType.LPAREN, "Expected '(' after if");
        parseExpression();
        expect(TokenType.RPAREN, "Expected ')' after if condition");

        parseStatementOrBlock();

        // optional else / elseif 
        while (peekType() == TokenType.ELSEIF) {
            advance(); // ELSEIF
            expect(TokenType.LPAREN, "Expected '(' after else if");
            parseExpression();
            expect(TokenType.RPAREN, "Expected ')' after else if condition");
            parseStatementOrBlock();
        }

        if (match(TokenType.ELSE)) {
            parseStatementOrBlock();
        }
    }

    private void parseWhileStatement() {
        expect(TokenType.WHILE, "Expected 'while'");
        expect(TokenType.LPAREN, "Expected '(' after while");
        parseExpression();
        expect(TokenType.RPAREN, "Expected ')' after while condition");
        parseStatementOrBlock();
    }

    private void parseDoWhileStatement() {
        expect(TokenType.DO, "Expected 'do'");
        parseStatementOrBlock();
        expect(TokenType.WHILE, "Expected 'while' after do block");
        expect(TokenType.LPAREN, "Expected '(' after while");
        parseExpression();
        expect(TokenType.RPAREN, "Expected ')' after do-while condition");
        expect(TokenType.SEMICOLON, "Expected ';' after do-while");
    }

    private void parseForStatement() {
        expect(TokenType.FOR, "Expected 'for'");
        expect(TokenType.LPAREN, "Expected '(' after for");

        // init:  [ empty ] | declaration(no trailing ;) | assignment/update(no trailing ;)
        if (peekType() != TokenType.SEMICOLON) {
            if (peekType() == TokenType.INT) {
                parseForInitDeclaration();
            } else {
                parseForInitOrUpdate(); // e.g. i = 0 or i++
            }
        }
        expect(TokenType.SEMICOLON, "Expected first ';' in for");

        // condition: [ empty ] | expression
        if (peekType() != TokenType.SEMICOLON) {
            parseExpression();
        }
        expect(TokenType.SEMICOLON, "Expected second ';' in for");

        // update: [ empty ] | assignment/update/expression
        if (peekType() != TokenType.RPAREN) {
            parseForInitOrUpdate(); // crucial fix: allow i = i + 1 here
        }

        expect(TokenType.RPAREN, "Expected ')' after for clauses");
        parseStatementOrBlock();
    }

    // "int i = 0" (no ending semicolon here, the for parser consumes it)
    private void parseForInitDeclaration() {
        expect(TokenType.INT, "Expected 'int' in for-init");
        expect(TokenType.IDENT, "Expected identifier in for-init");
        if (match(TokenType.ASSIGN)) {
            parseExpression();
        }
    }

    // For init/update in for(): allow:
    //   i = expr
    //   i++
    //   i--
    private void parseForInitOrUpdate() {
        if (peekType() != TokenType.IDENT) {
            error("Expected identifier in for init/update");
        }

        advance(); // consume IDENT

        if (match(TokenType.ASSIGN)) {
            parseExpression();
            return;
        }

        if (match(TokenType.INC) || match(TokenType.DEC)) {
            return;
        }

        error("Expected '=', '++', or '--' after identifier in for init/update");
    }

    private void parseSwitchStatement() {
        expect(TokenType.SWITCH, "Expected 'switch'");
        expect(TokenType.LPAREN, "Expected '(' after switch");
        parseExpression();
        expect(TokenType.RPAREN, "Expected ')' after switch expression");
        expect(TokenType.LBRACE, "Expected '{' after switch");

        // Parse 0+ case/default blocks until '}'
        while (peekType() != TokenType.RBRACE && peekType() != TokenType.EOF) {
            if (match(TokenType.CASE)) {
                parseExpression();
                expect(TokenType.COLON, "Expected ':' after case value");
            } else if (match(TokenType.DEFAULT)) {
                expect(TokenType.COLON, "Expected ':' after default");
            } else {
                parseStatement();
            }
        }

        expect(TokenType.RBRACE, "Expected '}' to close switch");
    }

    // ---------------------------
    // Expressions (precedence)
    // ---------------------------
    //
    // expression -> equality
    // equality   -> comparison ( (== | !=) comparison )*
    // comparison -> term       ( (< | > | <= | >=) term )*
    // term       -> factor     ( (+ | -) factor )*
    // factor     -> unary      ( (* | /) unary )*
    // unary      -> (+ | -) unary | primary
    // primary    -> NUMBER | IDENT | '(' expression ')'
    //
    // NOTE: We intentionally do NOT include assignment '=' as an expression operator.
    // Assignments are statements (x = expr;) and for-init/update handled separately.

    private void parseExpression() {
        parseEquality();
    }

    private void parseEquality() {
        parseComparison();
        while (peekType() == TokenType.EQ || peekType() == TokenType.NEQ) {
            advance();
            parseComparison();
        }
    }

    private void parseComparison() {
        parseTerm();
        while (peekType() == TokenType.LT
                || peekType() == TokenType.GT
                || peekType() == TokenType.LTE
                || peekType() == TokenType.GTE) {
            advance();
            parseTerm();
        }
    }

    private void parseTerm() {
        parseFactor();
        while (peekType() == TokenType.PLUS || peekType() == TokenType.MINUS) {
            advance();
            parseFactor();
        }
    }

    private void parseFactor() {
        parseUnary();
        while (peekType() == TokenType.MUL || peekType() == TokenType.DIV) {
            advance();
            parseUnary();
        }
    }

    private void parseUnary() {
        if (peekType() == TokenType.PLUS || peekType() == TokenType.MINUS) {
            advance();
            parseUnary();
            return;
        }
        parsePrimary();
    }

    private void parsePrimary() {
        if (match(TokenType.NUMBER)) return;
        if (match(TokenType.IDENT)) return;

        if (match(TokenType.LPAREN)) {
            parseExpression();
            expect(TokenType.RPAREN, "Expected ')'");
            return;
        }

        error("Expected number, identifier, or '('");
    }
}