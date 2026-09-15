package test;

import lexer.Lexer;
import lexer.Token;
import lexer.TokenType;

import java.util.List;

/**
 * Tests unitaires autonomes pour l'analyseur lexical (Lexer).
 * Aucune dependance externe — executer avec :
 *   javac -d out src/lexer/*.java src/test/LexerTest.java
 *   java -cp out test.LexerTest
 */
public class LexerTest {

    private static int passed = 0;
    private static int failed = 0;

    // ── Assertions ────────────────────────────────────────────────────────────

    private static void assertEquals(String testName, Object expected, Object actual) {
        if (expected.equals(actual)) {
            System.out.println("  [PASS] " + testName);
            passed++;
        } else {
            System.out.println("  [FAIL] " + testName
                    + "\n         expected : " + expected
                    + "\n         actual   : " + actual);
            failed++;
        }
    }

    private static void assertThrows(String testName, Runnable block) {
        try {
            block.run();
            System.out.println("  [FAIL] " + testName + " (expected exception, none thrown)");
            failed++;
        } catch (RuntimeException e) {
            System.out.println("  [PASS] " + testName + " — threw: " + e.getMessage());
            passed++;
        }
    }

    /** Tokenise l'input et retourne la liste SANS le token EOF final. */
    private static List<Token> lex(String input) {
        List<Token> tokens = new Lexer(input).tokenize();
        if (!tokens.isEmpty() && tokens.getLast().getType() == TokenType.EOF) {
            return tokens.subList(0, tokens.size() - 1);
        }
        return tokens;
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    private static void testKeywords() {
        System.out.println("\n=== testKeywords ===");
        String[] keywords = {"int", "if", "else", "while", "for", "do", "switch", "case", "default", "break"};
        TokenType[] types  = {
            TokenType.INT, TokenType.IF, TokenType.ELSE,
            TokenType.WHILE, TokenType.FOR, TokenType.DO,
            TokenType.SWITCH, TokenType.CASE, TokenType.DEFAULT, TokenType.BREAK
        };
        for (int i = 0; i < keywords.length; i++) {
            List<Token> tokens = lex(keywords[i]);
            assertEquals("keyword '" + keywords[i] + "' size=1", 1, tokens.size());
            assertEquals("keyword '" + keywords[i] + "' type", types[i], tokens.get(0).getType());
            assertEquals("keyword '" + keywords[i] + "' lexeme", keywords[i], tokens.get(0).getLexeme());
        }
    }

    private static void testIdentifiers() {
        System.out.println("\n=== testIdentifiers ===");
        String[] idents = {"x", "myVar", "_count", "abc123", "_"};
        for (String id : idents) {
            List<Token> tokens = lex(id);
            assertEquals("ident '" + id + "' type", TokenType.IDENT, tokens.get(0).getType());
            assertEquals("ident '" + id + "' lexeme", id, tokens.get(0).getLexeme());
        }
    }

    private static void testNumbers() {
        System.out.println("\n=== testNumbers ===");
        String[] nums = {"0", "1", "42", "1000", "99999"};
        for (String n : nums) {
            List<Token> tokens = lex(n);
            assertEquals("number '" + n + "' type", TokenType.NUMBER, tokens.get(0).getType());
            assertEquals("number '" + n + "' lexeme", n, tokens.get(0).getLexeme());
        }
    }

    private static void testNumberPosition() {
        System.out.println("\n=== testNumberPosition (bug fix: startLine) ===");
        // "int x =\n42;" — 42 est sur la ligne 2
        List<Token> tokens = new Lexer("int x =\n42;").tokenize();
        // INT IDENT ASSIGN NUMBER(42) SEMICOLON EOF
        Token numToken = tokens.get(3);
        assertEquals("number '42' type", TokenType.NUMBER, numToken.getType());
        assertEquals("number '42' line=2", 2, numToken.getLine());
        assertEquals("number '42' col=1", 1, numToken.getColumn());
    }

    private static void testSingleCharOperators() {
        System.out.println("\n=== testSingleCharOperators ===");
        Object[][] cases = {
            {"+", TokenType.PLUS}, {"-", TokenType.MINUS},
            {"*", TokenType.MUL},  {"/", TokenType.DIV},
            {"=", TokenType.ASSIGN}, {">", TokenType.GT},
            {"<", TokenType.LT},  {";", TokenType.SEMICOLON},
            {",", TokenType.COMMA}, {"(", TokenType.LPAREN},
            {")", TokenType.RPAREN}, {"{", TokenType.LBRACE},
            {"}", TokenType.RBRACE}, {":", TokenType.COLON}
        };
        for (Object[] c : cases) {
            String input = (String) c[0];
            TokenType expected = (TokenType) c[1];
            List<Token> tokens = lex(input);
            assertEquals("op '" + input + "' type", expected, tokens.get(0).getType());
        }
    }

    private static void testDoubleCharOperators() {
        System.out.println("\n=== testDoubleCharOperators ===");
        Object[][] cases = {
            {"==", TokenType.EQ}, {"!=", TokenType.NEQ},
            {">=", TokenType.GTE}, {"<=", TokenType.LTE},
            {"++", TokenType.INC}, {"--", TokenType.DEC}
        };
        for (Object[] c : cases) {
            String input = (String) c[0];
            TokenType expected = (TokenType) c[1];
            List<Token> tokens = lex(input);
            assertEquals("op '" + input + "' type", expected, tokens.get(0).getType());
            assertEquals("op '" + input + "' lexeme", input, tokens.get(0).getLexeme());
        }
    }

    private static void testElseIf() {
        System.out.println("\n=== testElseIf ===");

        List<Token> t1 = lex("else if");
        assertEquals("'else if' size=1", 1, t1.size());
        assertEquals("'else if' type=ELSEIF", TokenType.ELSEIF, t1.get(0).getType());
        assertEquals("'else if' lexeme", "else if", t1.get(0).getLexeme());

        List<Token> t2 = lex("else");
        assertEquals("'else' alone type=ELSE", TokenType.ELSE, t2.get(0).getType());

        List<Token> t3 = lex("else   if");
        assertEquals("'else   if' (spaces) type=ELSEIF", TokenType.ELSEIF, t3.get(0).getType());

        List<Token> t4 = lex("else { }");
        assertEquals("'else {' tok0=ELSE", TokenType.ELSE, t4.get(0).getType());
        assertEquals("'else {' tok1=LBRACE", TokenType.LBRACE, t4.get(1).getType());
    }

    private static void testSingleLineComment() {
        System.out.println("\n=== testSingleLineComment ===");

        List<Token> t1 = lex("// commentaire seul");
        assertEquals("commentaire seul -> 0 tokens", 0, t1.size());

        List<Token> t2 = lex("// commentaire\nint x;");
        assertEquals("commentaire + 'int x;' -> 3 tokens", 3, t2.size());
        assertEquals("premier token INT", TokenType.INT, t2.get(0).getType());

        List<Token> t3 = lex("int x; // trailing");
        assertEquals("commentaire trailing -> 3 tokens", 3, t3.size());
    }

    private static void testBlockComment() {
        System.out.println("\n=== testBlockComment ===");

        List<Token> t1 = lex("/* commentaire bloc */");
        assertEquals("bloc seul -> 0 tokens", 0, t1.size());

        List<Token> t2 = lex("/* ligne1\nligne2\nligne3 */\nint x;");
        assertEquals("bloc multi-lignes + 'int x;' -> 3 tokens", 3, t2.size());
        assertEquals("premier token INT", TokenType.INT, t2.get(0).getType());

        assertThrows("bloc non ferme -> RuntimeException",
                () -> new Lexer("/* non ferme").tokenize());
    }

    private static void testUnknownToken() {
        System.out.println("\n=== testUnknownToken ===");
        List<Token> t1 = lex("@");
        assertEquals("'@' type=UNKNOWN", TokenType.UNKNOWN, t1.get(0).getType());
        assertEquals("'@' lexeme", "@", t1.get(0).getLexeme());

        List<Token> t2 = lex("!");
        assertEquals("'!' seul type=UNKNOWN", TokenType.UNKNOWN, t2.get(0).getType());
    }

    private static void testTokenPositions() {
        System.out.println("\n=== testTokenPositions ===");
        // "int x = 42;"
        List<Token> tokens = new Lexer("int x = 42;").tokenize();
        assertEquals("INT col=1",        1, tokens.get(0).getColumn());
        assertEquals("IDENT col=5",      5, tokens.get(1).getColumn());
        assertEquals("ASSIGN col=7",     7, tokens.get(2).getColumn());
        assertEquals("NUMBER col=9",     9, tokens.get(3).getColumn());
        assertEquals("SEMICOLON col=11", 11, tokens.get(4).getColumn());
        assertEquals("EOF line=1",       1,  tokens.get(5).getLine());
    }

    private static void testWhitespaceIgnored() {
        System.out.println("\n=== testWhitespaceIgnored ===");
        List<Token> t1 = lex("   int\t  x  ;  ");
        assertEquals("espaces ignores -> 3 tokens", 3, t1.size());

        List<Token> t2 = lex("int\nx\n;");
        assertEquals("newlines ignores -> 3 tokens", 3, t2.size());
    }

    private static void testFullDeclaration() {
        System.out.println("\n=== testFullDeclaration ===");
        List<Token> tokens = lex("int counter = 100;");
        assertEquals("decl complete -> 5 tokens", 5, tokens.size());
        assertEquals("tok0 INT",    TokenType.INT,       tokens.get(0).getType());
        assertEquals("tok1 IDENT",  TokenType.IDENT,     tokens.get(1).getType());
        assertEquals("tok2 ASSIGN", TokenType.ASSIGN,    tokens.get(2).getType());
        assertEquals("tok3 NUMBER", TokenType.NUMBER,    tokens.get(3).getType());
        assertEquals("tok4 SEMI",   TokenType.SEMICOLON, tokens.get(4).getType());
        assertEquals("tok1 lexeme=counter", "counter",   tokens.get(1).getLexeme());
        assertEquals("tok3 lexeme=100",     "100",        tokens.get(3).getLexeme());
    }

    private static void testEOFToken() {
        System.out.println("\n=== testEOFToken ===");
        List<Token> tokens = new Lexer("").tokenize();
        assertEquals("input vide -> 1 token", 1, tokens.size());
        assertEquals("seul token = EOF", TokenType.EOF, tokens.get(0).getType());
    }

    // ── Point d'entree ────────────────────────────────────────────────────────

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║          LEXER TESTS -- TokenParse           ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        testKeywords();
        testIdentifiers();
        testNumbers();
        testNumberPosition();
        testSingleCharOperators();
        testDoubleCharOperators();
        testElseIf();
        testSingleLineComment();
        testBlockComment();
        testUnknownToken();
        testTokenPositions();
        testWhitespaceIgnored();
        testFullDeclaration();
        testEOFToken();

        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.printf ("║  RESULTATS : %3d passes  |  %3d echoues      ║%n", passed, failed);
        System.out.println("╚══════════════════════════════════════════════╝");

        System.exit(failed > 0 ? 1 : 0);
    }
}
