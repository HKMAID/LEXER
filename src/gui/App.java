package gui;

import lexer.Lexer;
import lexer.Token;
import parser.Parser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;

public final class App {

    private App() {}

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            setNiceLookAndFeel();

            JFrame frame = new JFrame("TokenParse — Lexer + Parser");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            // Editor
            JTextArea codeArea = new JTextArea(18, 90);
            codeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
            codeArea.setTabSize(4);
            codeArea.setText(sampleProgram());

            // Output
            JTextArea outArea = new JTextArea(14, 90);
            outArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            outArea.setEditable(false);

            JCheckBox showTokens = new JCheckBox("Show tokens", true);
            JButton runBtn = new JButton("Run");
            JButton clearBtn = new JButton("Clear output");

            // Templates dropdown
            JComboBox<Template> templateCombo = new JComboBox<>(Template.values());
            templateCombo.setSelectedItem(Template.FULL_PROGRAM);

            JButton pasteBtn = new JButton("Paste template");
            pasteBtn.addActionListener(e -> {
                Template t = (Template) templateCombo.getSelectedItem();
                if (t != null) {
                    codeArea.setText(t.code);
                    codeArea.setCaretPosition(0);
                }
            });

            runBtn.addActionListener(e -> run(codeArea, outArea, showTokens.isSelected(), frame));
            clearBtn.addActionListener(e -> outArea.setText(""));

            // Top bar
            JPanel top = new JPanel(new GridBagLayout());
            top.setBorder(new EmptyBorder(8, 8, 8, 8));
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = 0;
            c.insets = new Insets(0, 0, 0, 8);
            c.anchor = GridBagConstraints.WEST;

            c.gridx = 0; top.add(runBtn, c);
            c.gridx = 1; top.add(clearBtn, c);
            c.gridx = 2; top.add(showTokens, c);

            c.gridx = 3; top.add(new JLabel("Tests:"), c);
            c.gridx = 4; top.add(templateCombo, c);
            c.gridx = 5; top.add(pasteBtn, c);

            // Split pane
            JSplitPane split = new JSplitPane(
                    JSplitPane.VERTICAL_SPLIT,
                    wrapWithTitledScroll("Code", codeArea),
                    wrapWithTitledScroll("Output", outArea)
            );
            split.setResizeWeight(0.60);

            // Root
            JPanel root = new JPanel(new BorderLayout(10, 10));
            root.setBorder(new EmptyBorder(10, 10, 10, 10));
            root.add(top, BorderLayout.NORTH);
            root.add(split, BorderLayout.CENTER);

            frame.setJMenuBar(buildMenuBar(codeArea, outArea, showTokens, frame));
            frame.setContentPane(root);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static JMenuBar buildMenuBar(JTextArea codeArea, JTextArea outArea, JCheckBox showTokens, JFrame frame) {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        JMenuItem clearOut = new JMenuItem("Clear output");
        clearOut.addActionListener(e -> outArea.setText(""));
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> frame.dispose());
        file.add(clearOut);
        file.addSeparator();
        file.add(exit);

        JMenu tests = new JMenu("Predefined tests");
        for (Template t : Template.values()) {
            JMenuItem item = new JMenuItem(t.label);
            item.addActionListener(e -> {
                codeArea.setText(t.code);
                codeArea.setCaretPosition(0);
                outArea.setText("");
                outArea.append("// Loaded template: " + t.label + "\n");
            });
            tests.add(item);
        }

        JMenu run = new JMenu("Run");
        JMenuItem runNow = new JMenuItem("Run (Ctrl+Enter)");
        runNow.addActionListener(e -> run(codeArea, outArea, showTokens.isSelected(), frame));
        run.add(runNow);

        bar.add(file);
        bar.add(run);
        bar.add(tests);

        // Keybinding: Ctrl+Enter to run
        KeyStroke ks = KeyStroke.getKeyStroke("control ENTER");
        codeArea.getInputMap(JComponent.WHEN_FOCUSED).put(ks, "RUN");
        codeArea.getActionMap().put("RUN", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                run(codeArea, outArea, showTokens.isSelected(), frame);
            }
        });

        return bar;
    }

    private static void run(JTextArea codeArea, JTextArea outArea, boolean showTokens, JFrame frame) {
        outArea.setText("");
        String input = codeArea.getText();

        try {
            Lexer lexer = new Lexer(input);
            List<Token> tokens = lexer.tokenize();

            if (showTokens) {
                outArea.append("TOKENS:\n");
                for (Token t : tokens) {
                    outArea.append(t.toString());
                    outArea.append("\n");
                }
                outArea.append("\n");
            }

            Parser parser = new Parser(tokens);
            parser.parse();

            outArea.append("OK: parsed successfully.\n");
            outArea.append("// " + LocalDateTime.now() + "\n");

        } catch (RuntimeException ex) {
            // Convert to C-style error
            String cStyle = toCStyleError(ex.getMessage());
            outArea.append(cStyle + "\n");
            JOptionPane.showMessageDialog(frame, cStyle, "Compile error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            outArea.append("internal: error: " + ex + "\n");
            JOptionPane.showMessageDialog(frame, ex.toString(), "Internal error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String toCStyleError(String msg) {
        // Expected format from your parser: "message at token TOKEN('x') at L:C"
        int atIdx = msg.lastIndexOf(" at ");
        if (atIdx >= 0) {
            String left = msg.substring(0, atIdx).trim();
            String right = msg.substring(atIdx + 4).trim(); // token ... at L:C

            // find last " at " inside right => line:col
            int at2 = right.lastIndexOf(" at ");
            if (at2 >= 0 && at2 + 4 < right.length()) {
                String loc = right.substring(at2 + 4).trim(); // like "4:9"
                // C style: <stdin>:line:col: error: message
                return "<stdin>:" + loc + ": error: " + left;
            }
        }
        return "<stdin>:1:1: error: " + msg;
    }

    private static JScrollPane wrapWithTitledScroll(String title, JTextArea area) {
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(BorderFactory.createTitledBorder(title));
        return sp;
    }

    private static void setNiceLookAndFeel() {
        try {
            // Nimbus 
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    private static String sampleProgram() {
        return """
                // Ctrl+Enter to run
                int x = 5;
                x = x + 2;

                while (x == 7) {
                    x++;
                }
                """;
    }

    // Predefined test templates
    private enum Template {
        FULL_PROGRAM("Full program", """
                int x = 5;
                x = x + 2;

                while (x == 7) {
                    x++;
                }

                int y;
                y = (3 + 4) * 2;
                """),

        DECLARATION("Declaration", """
                int x;
                int y = 10;
                """),

        ASSIGNMENT("Assignment", """
                int x = 0;
                x = 5;
                x = x + 2;
                """),

        EXPRESSIONS("Expressions + parentheses", """
                int x = (5 + 2) * 3;
                x = x / 2 + 7;
                """),

        WHILE_LOOP("While loop", """
                int x = 0;
                while (x < 3) {
                    x++;
                }
                """),

        IF_STATEMENT("If statement", """
                int x = 5;
                if (x == 5) {
                    x++;
                }
                """),

        FOR_LOOP("For loop", """
                for (int i = 0; i < 3; i = i + 1) {
                    x = x + 1;
                }
                """),

        SWITCH_STATEMENT("Switch", """
                switch (x) {
                    case 1:
                        x = x + 1;
                        break;
                    default:
                        x = 0;
                        break;
                }
                """),

        DO_WHILE("Do-While loop", """
                int i = 0;
                do {
                    i++;
                } while (i < 5);
                """),

        IF_ELSE_IF("If / Else-If / Else", """
                int x = 3;
                if (x == 1) {
                    x = 10;
                } else if (x == 2) {
                    x = 20;
                } else if (x == 3) {
                    x = 30;
                } else {
                    x = 0;
                }
                """),

        NESTED_LOOPS("Nested loops", """
                int i = 0;
                int j = 0;
                for (int k = 0; k < 5; k++) {
                    while (j < k) {
                        j = j + 1;
                    }
                    i = i + j;
                }
                """),

        COMMENTS("Comments (// and /* */)", """
                // This is a single-line comment
                int x = 42; // trailing comment
                /*
                   Multi-line comment
                   spanning three lines
                */
                x = x + 1;
                """),

        ERROR_MISSING_SEMICOLON("ERROR — Missing semicolon", """
                int x = 5
                x = x + 1;
                """),

        ERROR_UNKNOWN_TOKEN("ERROR — Unknown token (@)", """
                int x = 5;
                x = x @ 2;
                """);

        final String label;
        final String code;

        Template(String label, String code) {
            this.label = label;
            this.code = code;
        }

        @Override public String toString() {
            return label;
        }
    }
}