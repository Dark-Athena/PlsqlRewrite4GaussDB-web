package com.plsqlrewriter.util;

import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import com.plsqlrewriter.parser.antlr.generated.PlSqlLexer;
import com.plsqlrewriter.parser.antlr.generated.PlSqlParser;

public class PlSqlTreeViewer {
    private static final Logger logger = LoggerFactory.getLogger(PlSqlTreeViewer.class);

    private boolean showTokens = false;
    private boolean showGui = false;
    private String input = null;
    private String inputFile = null;

    public PlSqlTreeViewer() {
    }

    public void processArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-tokens":
                    showTokens = true;
                    break;
                case "-gui":
                    showGui = true;
                    break;
                default:
                    if (inputFile == null) {
                        inputFile = args[i];
                    }
                    break;
            }
        }
    }

    public void process() {
        try {
            // 如果没有输入文件，则从标准输入读取
            if (inputFile == null) {
                input = readFromStdin();
            } else {
                input = new String(Files.readAllBytes(Paths.get(inputFile)), StandardCharsets.UTF_8);
            }

            // 创建词法分析器和解析器
            PlSqlLexer lexer = new PlSqlLexer(CharStreams.fromString(input));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            PlSqlParser parser = new PlSqlParser(tokens);

            // 如果需要显示词法标记
            if (showTokens) {
                tokens.fill();
                for (Token token : tokens.getTokens()) {
                    System.out.printf("%-20s '%s'%n",
                            PlSqlLexer.VOCABULARY.getSymbolicName(token.getType()),
                            token.getText());
                }
            }

            // 解析并获取语法树
            ParseTree tree = parser.sql_script();

            // 打印树形结构
            System.out.println(tree.toStringTree(parser));

            // 如果需要显示GUI
            if (showGui) {
                showTreeGui(parser, tree);
            }

        } catch (IOException e) {
            logger.error("处理输入时发生错误", e);
        }
    }

    private String readFromStdin() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private void showTreeGui(PlSqlParser parser, ParseTree tree) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("PlSQL 语法树查看器");
            TreeViewer viewer = new TreeViewer(Arrays.asList(parser.getRuleNames()), tree);
            frame.add(viewer);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
        });
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("usage:");
            System.out.println("  java PlSqlTreeViewer <filename>");
            System.out.println("  java PlSqlTreeViewer -tokens <filename>");
            System.out.println("  java PlSqlTreeViewer -gui <filename>");
            System.out.println("  java PlSqlTreeViewer -tokens -gui <filename>");
            System.out.println("  echo \"select 1 from dual;\" | java PlSqlTreeViewer");
            return;
        }

        PlSqlTreeViewer viewer = new PlSqlTreeViewer();
        viewer.processArgs(args);
        viewer.process();
    }
} 