package com.plsqlrewriter.core;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.misc.Interval;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.Path;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import org.yaml.snakeyaml.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import com.plsqlrewriter.parser.antlr.generated.PlSqlLexer;
import com.plsqlrewriter.parser.antlr.generated.PlSqlParser;
import com.plsqlrewriter.parser.antlr.generated.PlSqlParserBaseVisitor;
import com.plsqlrewriter.util.BatchProcessor;

public class PlSqlRewriter {
    private static final Logger logger = LoggerFactory.getLogger(PlSqlRewriter.class);
    
    public static String removeComment(String input) {
        if (input.length() <= 4) {
            return input;
        }
        String prefix = input.substring(0, 1);
        String suffix = input.substring(input.length() - 1);
        String middle = input.substring(1, input.length() - 1);
        middle = middle.replaceAll("/\\*", "\\*");
        return prefix + middle + suffix;
    }
    
    static Map<String, String> loadconfigMapping(String filePath,String caseOption) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getConfigInputStream(filePath)) {
            if (input == null) {
                throw new IOException("Configuration file not found: " + filePath);
            }
            properties.load(input);
        }
        Map<String, String> mapping = new HashMap<>();
        if (!filePath.contains("patterns.properties")){
            for (String key : properties.stringPropertyNames()) {
                if (caseOption.toUpperCase().equals("U")){ 
                mapping.put(key.toUpperCase(), properties.getProperty(key));
                }
                else if (caseOption.toUpperCase().equals("L")){
                mapping.put(key.toLowerCase(), properties.getProperty(key));
                } else {
                mapping.put(key, properties.getProperty(key));
                }
            }
        } else {
            for (String key : properties.stringPropertyNames()) {
                if (key.startsWith("pattern")) {
                    String replacementKey = "replacement" + key.substring(7);
                    mapping.put(properties.getProperty(key), properties.getProperty(replacementKey));
                }
            }
        }
        return mapping;
    }

    //static Map<String, String> patternsReplacements = new HashMap<>();
    static List<Map<String, String>> regexpRules ;
    static List<Map<String, String>> dynamicSqlRules;
    static Map<String, String> exceptionMapping;
    static Map<String, String> generalElementMapping;
    static Map<String, String> config;
    static Map<String, String> tokenReplaceMapping;
    static Map<String, String> regularIdMapping;
    static Map<String, String> datetypeMapping;
    static Map<String, String> atomMapping;
    static Map<String, String> queryBloackMapping;
    static Set<String> udtlist;
    static List<Pattern> udtPatterns;

    // 添加配置加载状态标志
    private static volatile boolean configsLoaded = false;
    
    private static class DynamicSqlResult {
        private final String sql;
        private final boolean shouldRemove;
        private final String matchedPattern;

        public DynamicSqlResult(String sql, boolean shouldRemove, String matchedPattern) {
            this.sql = sql;
            this.shouldRemove = shouldRemove;
            this.matchedPattern = matchedPattern;
        }

        public String getSql() {
            return sql;
        }

        public boolean shouldRemove() {
            return shouldRemove;
        }
        
        public String getMatchedPattern() {
            return matchedPattern;
        }
    }
    
    public static String regexpReplace(String inputSql) {
       // 提取注释部分
        StringBuilder codePart = new StringBuilder();
        StringBuilder commentPart = new StringBuilder();
        String[] lines = inputSql.split("\n");
        for (String line : lines) {
            int commentIndex = line.indexOf("--");
            if (commentIndex != -1) {
                codePart.append(line.substring(0, commentIndex)).append("\n");
                commentPart.append(line.substring(commentIndex)).append("\n");
            } else {
                codePart.append(line).append("\n");
                commentPart.append("\n");
            }
        }

        // 对代码部分进行正则匹配和替换
        String code = codePart.toString();
        /*for (Map.Entry<String, String> entry : patternsReplacements.entrySet()) {
            Pattern pattern = Pattern.compile(entry.getKey(), Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(code);
            System.out.println("pattern: " + pattern);
            code = matcher.replaceAll(entry.getValue());
        }*/
        for (Map<String, String> rule : regexpRules) {
            String pattern = rule.get("pattern");
            String replacement = rule.get("replacement");
            if (replacement == null) {
                replacement = "";
            }
            Pattern compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = compiledPattern.matcher(code);
            code = matcher.replaceAll(replacement);
        }

        // 将注释部分加回去
        StringBuilder result = new StringBuilder();
        String[] codeLines = code.split("\n");
        String[] commentLines = commentPart.toString().split("\n");
        for (int i = 0; i < codeLines.length; i++) {
            result.append(codeLines[i]);
            if (i < commentLines.length && !commentLines[i].isEmpty()) {
                result.append(commentLines[i]);
            }
            result.append("\n");
        }

        return result.toString();
    }

    private static void validateConfigs(String jarPath) throws IOException {
        String[] requiredConfigs = {
            "config/exception_mapping.properties",
            "config/general_element_mapping.properties", 
            "config/config.properties",
            "config/token_mapping.properties",
            "config/regular_id_mapping.properties",
            "config/datatype_mapping.properties",
            "config/atom_mapping.properties",
            "config/query_block_mapping.properties",
            "config/patterns.yaml",
            "config/dynamic_sql.yaml",
            "config/udtlist.properties"
        };
        
        for (String config : requiredConfigs) {
            // First check external config
            File externalConfig = new File(config);
            if (!externalConfig.exists()) {
                // If external config not found, check in jar
                if (PlSqlRewriter.class.getClassLoader().getResourceAsStream(config) == null) {
                    throw new IOException("Required configuration file missing: " + config);
                }
            }
        }
    }

    /**
     * 预加载所有配置文件，避免多线程并发读取同一文件造成问题
     */
    public static synchronized void preloadAllConfigs() throws IOException {
        if (configsLoaded) {
            logger.info("Configuration already preloaded, skipping loading process");
            return; // 已加载过，直接返回
        }
        
        logger.info("Starting to preload all configuration files...");
        
        // 验证配置
        validateConfigs("config");
        
        // 加载所有配置映射
        exceptionMapping = loadconfigMapping("config/exception_mapping.properties", "L");
        generalElementMapping = loadconfigMapping("config/general_element_mapping.properties", "L");
        config = loadconfigMapping("config/config.properties", "");
        tokenReplaceMapping = loadconfigMapping("config/token_mapping.properties", "L");
        regularIdMapping = loadconfigMapping("config/regular_id_mapping.properties", "L");
        datetypeMapping = loadconfigMapping("config/datatype_mapping.properties", "L");
        atomMapping = loadconfigMapping("config/atom_mapping.properties", "L");
        queryBloackMapping = loadconfigMapping("config/query_block_mapping.properties", "");
        
        // 预加载正则和动态SQL规则
        preloadPatternRules();
        preloadDynamicSqlRules();
        
        // 预加载UDT列表
        preloadUdtList();
        
        configsLoaded = true;
        logger.info("All configuration files preloaded successfully");
    }

    /**
     * 预加载UDT列表
     */
    private static void preloadUdtList() {
        udtlist = new HashSet<>();
        udtPatterns = new ArrayList<>();
        try (InputStream input = getConfigInputStream("config/udtlist.properties")) {
            if (input == null) {
                throw new IOException("Configuration file not found: config/udtlist.properties");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                loadUdtListFromReader(reader);
                logger.info("UDT list preloaded from config (external or jar)");
            }
        } catch (IOException e) {
            logger.error("Error loading UDT list from config: {}", e.getMessage());
            throw new RuntimeException("UDT list loading failed: " + e.getMessage());
        }
    }

    /**
     * 从Reader中加载UDT列表
     */
    private static void loadUdtListFromReader(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue; // 跳过空行和注释行
            }
            
            // 检查是否包含通配符
            if (line.contains("*") || line.contains("?")) {
                // 转换为正则表达式
                String regex = line.toUpperCase()
                                   .replace(".", "\\.")
                                   .replace("*", ".*")
                                   .replace("?", ".");
                udtPatterns.add(Pattern.compile("^" + regex + "$"));
                logger.debug("Added UDT wildcard pattern: {} -> regex: {}", line, regex);
            } else {
                // 普通精确匹配
                udtlist.add(line.toUpperCase());
            }
        }
    }

    /**
     * 预加载正则表达式规则
     */
    public static synchronized void preloadPatternRules() {
        if (regexpRules != null && !regexpRules.isEmpty()) {
            logger.debug("Regexp rules already preloaded, skipping loading process");
            return; // 已加载过，直接返回
        }
        regexpRules = new ArrayList<>();
        Yaml yaml = new Yaml();
        try (InputStream input = getConfigInputStream("config/patterns.yaml")) {
            if (input == null) {
                throw new IOException("Configuration file not found: config/patterns.yaml");
            }
            Map<String, List<Map<String, String>>> patternsConfig = yaml.load(input);
            if (patternsConfig != null && patternsConfig.containsKey("rules")) {
                regexpRules = patternsConfig.get("rules");
                logger.info("Preloaded {} regexp rules (from config)", regexpRules.size());
            } else {
                logger.warn("Empty rule set found when preloading regexp rules");
            }
        } catch (IOException e) {
            logger.error("Error loading regexp rules from config: {}", e.getMessage());
            throw new RuntimeException("Regexp rules loading failed: " + e.getMessage());
        }
    }

    /**
     * 预加载动态SQL规则
     */
    public static synchronized void preloadDynamicSqlRules() {
        if (dynamicSqlRules != null && !dynamicSqlRules.isEmpty()) {
            logger.debug("Dynamic SQL rules already preloaded, skipping loading process");
            return; // 已加载过，直接返回
        }
        dynamicSqlRules = new ArrayList<>();
        Yaml yaml = new Yaml();
        try (InputStream input = getConfigInputStream("config/dynamic_sql.yaml")) {
            if (input == null) {
                throw new IOException("Configuration file not found: config/dynamic_sql.yaml");
            }
            Map<String, List<Map<String, String>>> config = yaml.load(input);
            if (config != null && config.containsKey("rules")) {
                dynamicSqlRules = config.get("rules");
                logger.info("Preloaded {} dynamic SQL rules (from config)", dynamicSqlRules.size());
            } else {
                logger.warn("Empty rule set found when preloading dynamic SQL rules");
            }
        } catch (IOException e) {
            logger.error("Error loading dynamic SQL rules from config: {}", e.getMessage());
            throw new RuntimeException("Dynamic SQL rules loading failed: " + e.getMessage());
        }
    }

    // 修改原有的loadPatternRules方法，使用预加载
    private static void loadPatternRules() {
        if (!configsLoaded) {
            try {
                preloadPatternRules();
            } catch (RuntimeException e) {
                logger.error("Loading regexp rules failed: {}", e.getMessage());
            }
        }
    }

    // 修改原有的loadDynamicSqlRules方法，使用预加载
    private static void loadDynamicSqlRules() {
        if (!configsLoaded) {
            try {
                preloadDynamicSqlRules();
            } catch (RuntimeException e) {
                logger.error("Loading dynamic SQL rules failed: {}", e.getMessage());
            }
        }
    }

    public static void processFile(Path inputFile, Path outputFile, String sourceEncoding, String targetEncoding) throws Exception {
        logger.info("Start processing file: {}", inputFile);
        // 确保配置已加载
        if (!configsLoaded) {
            try {
                preloadAllConfigs();
            } catch (IOException e) {
                logger.error("Configuration file error: {}", e.getMessage());
                logger.error("Please ensure all required configuration files exist in the config directory");
                throw e;
            }
        }
        // 从文件中读取 PL/SQL 代码
        String input = new String(Files.readAllBytes(inputFile),sourceEncoding);
        // 先按正则处理一次
        input=regexpReplace(input);
        // 创建词法分析器和解析器
        PlSqlLexer lexer = new PlSqlLexer(CharStreams.fromString(input));
        // 创建自定义错误监听器替代默认的ConsoleErrorListener
        com.plsqlrewriter.util.CustomErrorListener lexerErrorListener = new com.plsqlrewriter.util.CustomErrorListener(inputFile);
        lexer.removeErrorListeners(); // 移除默认的错误监听器
        lexer.addErrorListener(lexerErrorListener);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlSqlParser parser = new PlSqlParser(tokens);
        // 为解析器也添加自定义错误监听器
        com.plsqlrewriter.util.CustomErrorListener parserErrorListener = new com.plsqlrewriter.util.CustomErrorListener(inputFile);
        parser.removeErrorListeners(); // 移除默认的错误监听器
        parser.addErrorListener(parserErrorListener);
        // 获取语法树
        ParseTree tree = parser.sql_script();
        // 检查是否有解析错误
        boolean hasError = lexerErrorListener.hasError() || parserErrorListener.hasError();
        // 创建 TokenStreamRewriter
        TokenStreamRewriter rewriter = new TokenStreamRewriter(tokens);
        // 创建自定义的 Visitor
        PlSqlParserBaseVisitor<Void> visitor = getVisitor(inputFile, sourceEncoding, targetEncoding, rewriter, tokens);
        visitor.visit(tree);
        // Token级别处理
        processTokensForRewrite(tokens, rewriter, inputFile);
        // 将改写后的代码输出到文件
        String output = rewriter.getText();
        // 只有在没有解析错误的情况下才写入输出文件
        if (!hasError) {
            Files.write(outputFile, output.getBytes(targetEncoding), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            logger.info("output file: {}", outputFile);
        } else {
            logger.warn("Due to parsing errors, skipped generating output file: {}", outputFile);
            throw new Exception("File parsing error, conversion executed but output file not generated"); // Throw exception for BatchProcessor to catch
        }
    }

    // 抽取Visitor构造逻辑，便于字符串和文件两种方式共用
    private static PlSqlParserBaseVisitor<Void> getVisitor(Path inputFile, String sourceEncoding, String targetEncoding, TokenStreamRewriter rewriter, CommonTokenStream tokens) {
        // 迁移自processFile的Visitor匿名类，参数化inputFile/sourceEncoding/targetEncoding/rewriter/tokens
        // 由于内容较长，直接复用原有匿名类内容
        return new PlSqlParserBaseVisitor<Void>() {

            @Override 
            public Void visitTable_collection_expression(PlSqlParser.Table_collection_expressionContext ctx){
                if (ctx.getText().toUpperCase().startsWith("TABLE")) {
                    String argument = ctx.expression().getText();
                    String newText = "(select * from "+config.get("tablefunctionreplace")+"(" + argument + ") column_value)";
                    rewriter.replace(ctx.start.getTokenIndex(), ctx.stop.getTokenIndex(), newText);
                }
                return null;
            }
            
            // 非保留关键字作为字段名自动加as
            @Override 
            public Void visitColumn_alias(PlSqlParser.Column_aliasContext ctx) {
                // 检查是否使用了非保留关键字且没有带 AS
                if (ctx.identifier().id_expression().regular_id() != null && ctx.AS() == null) {
                    if (ctx.identifier().id_expression().regular_id().non_reserved_keywords_in_gaussdb() != null ||
                        ctx.identifier().id_expression().regular_id().non_reserved_keywords_pre12c() != null ||
                        ctx.identifier().id_expression().regular_id().non_reserved_keywords_in_12c() != null ||
                        ctx.identifier().id_expression().regular_id().non_reserved_keywords_in_18c() != null ) {
                        // 自动补充 AS
                        rewriter.insertBefore(ctx.identifier().getStart(), "AS ");
                        int line = ctx.start.getLine();
                        int charPositionInLine = ctx.start.getCharPositionInLine();
                        logger.info("add 'AS ' before non-reserved keyword,file:{} at line: {}, position: {}, contentText: {}", inputFile, line, charPositionInLine, ctx.getText());
                    }
                }
                return visitChildren(ctx);
            }
            
            // NEW type构造器去掉NEW并且加上括号
            @Override public Void visitVariable_declaration(PlSqlParser.Variable_declarationContext ctx) { 
                // 原有逻辑保留
                if (ctx.type_spec().type_name() !=null && 
                    ctx.default_value_part() != null &&
                    ctx.default_value_part().expression() !=null &&
                    ctx.type_spec().type_name().getText().toUpperCase().equals(ctx.default_value_part().expression().getText().toUpperCase())){
                    rewriter.insertAfter(ctx.default_value_part().expression().getStop(), "()");
                    logger.debug("add '()' after type name: file: {}, line: {}, position: {}, {}", inputFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.default_value_part().expression().getText());
                }
                
                // 添加新的逻辑：检查 char 或 varchar2 类型且赋值为纯字符串的变量声明
                if (ctx.type_spec() != null && ctx.type_spec().datatype() != null && 
                    ctx.type_spec().datatype().native_datatype_element() != null && 
                    ctx.default_value_part() != null && ctx.default_value_part().expression() != null) {
                    
                    // 获取数据类型
                    String dataTypeName = ctx.type_spec().datatype().native_datatype_element().getText().toLowerCase();
                    
                    // 检查是否为 char 或 varchar2 类型
                    if ("char".equals(dataTypeName) || "varchar2".equals(dataTypeName)) {
                        
                        // 获取声明的长度
                        int declaredLength = 0;
                        if (ctx.type_spec().datatype().precision_part() != null && 
                            ctx.type_spec().datatype().precision_part().numeric(0) != null) {
                            declaredLength = Integer.parseInt(ctx.type_spec().datatype().precision_part().numeric(0).getText());
                        }
                        
                        // 使用简化的方法查找字符串常量
                        String stringLiteral = null;
                        String expressionText = ctx.default_value_part().expression().getText();
                        
                        // 检查是否是引号括起来的字符串常量
                        if (expressionText.startsWith("'") && expressionText.endsWith("'")) {
                            // 去掉引号获取实际字符串
                            stringLiteral = expressionText.substring(1, expressionText.length() - 1);
                        }
                        
                        // 如果找到了字符串字面量，计算其字节长度
                        if (stringLiteral != null && declaredLength > 0) {
                            try {
                                // 使用指定的目标编码计算字符串的字节长度
                                int actualByteLength = stringLiteral.getBytes(targetEncoding).length;
                                
                                // 如果实际字节长度超过声明长度，则调整声明长度
                                if (actualByteLength > declaredLength) {
                                    // 获取数值部分
                                    Token lengthToken = ctx.type_spec().datatype().precision_part().start;
                                    
                                    // 替换整个长度部分
                                    rewriter.replace(
                                        ctx.type_spec().datatype().precision_part().start, 
                                        ctx.type_spec().datatype().precision_part().stop, 
                                        "(" + actualByteLength + ")"
                                    );
                                    
                                    logger.info("Adjusted {} length from {} to {} for Variable {} string '{}' at file: {}, line: {}, position: {}", 
                                        dataTypeName, declaredLength, actualByteLength, ctx.identifier().getText(), 
                                        stringLiteral, inputFile, lengthToken.getLine(), lengthToken.getCharPositionInLine());
                                }
                            } catch (UnsupportedEncodingException e) {
                                logger.error("Unsupported encoding: {}", targetEncoding, e);
                            }
                        }
                    }
                }
                
                return visitChildren(ctx); 
            }
    
            @Override
            public Void visitUnary_expression(PlSqlParser.Unary_expressionContext ctx) {
                // 删掉new
                if (ctx.getChild(0).getText().toUpperCase().equals("NEW")) {
                    TerminalNode node = (TerminalNode) ctx.getChild(0);
                    rewriter.delete(
                        TokenStreamRewriter.DEFAULT_PROGRAM_NAME,
                        node.getSymbol().getTokenIndex(),
                        node.getSymbol().getTokenIndex()
                    );
                    logger.debug("Deleted 'NEW' at file: {}, line: {}, position: {}, {}", inputFile, node.getSymbol().getLine(), node.getSymbol().getCharPositionInLine(), node.getSymbol().getText());
        
                    // 检查是否有 function_argument，如果没有则补上 '()'
                    ParseTree generalElement = ctx.getChild(1).getChild(0).getChild(0);
                    boolean hasFunctionArgument = false;
                    for (int i = 0; i < generalElement.getChildCount(); i++) {
                        ParseTree generalElementPart = generalElement.getChild(i);
                        for (int j = 0; j < generalElementPart.getChildCount(); j++) {
                            if (generalElementPart.getChild(j) instanceof PlSqlParser.Function_argumentContext) {
                                hasFunctionArgument = true;
                                break;
                            }
                        }
                        if (hasFunctionArgument) {
                            break;
                        }
                    }
        
                    //System.out.println("Has function argument: " + hasFunctionArgument);
        
                    if (!hasFunctionArgument) {
                        // 添加 function_argument '()'
                        PlSqlParser.Regular_idContext regularId = (PlSqlParser.Regular_idContext) generalElement.getChild(generalElement.getChildCount() - 1).getChild(generalElement.getChildCount() - 1).getChild(generalElement.getChildCount() - 1);
                        Token lastToken = ((TerminalNode) regularId.getChild(regularId.getChildCount() - 1)).getSymbol();
                        int lastTokenIndex = lastToken.getTokenIndex();
                        rewriter.insertAfter(lastTokenIndex, "()");
                        logger.debug("Inserted '()' after token index: file: {}, line: {}, position: {}, {}", inputFile, lastToken.getLine(), lastToken.getCharPositionInLine(), lastToken.getText());
                    }
                } 
                return visitChildren(ctx);
            }
    
            //移除宏编译判断 不启用，此种方式处理会导致和其他规则冲突，相关处理已移动到toekn_mapping内
            // @Override 
            // public Void visitSelection_directive(PlSqlParser.Selection_directiveContext ctx) {
    
            //     StringBuilder allText = new StringBuilder();
    
            //     for (PlSqlParser.Selection_directive_bodyContext body : ctx.selection_directive_body()) {
            //         int startTokenIndex = body.getStart().getTokenIndex();
            //         int stopTokenIndex = body.getStop().getTokenIndex();
            //         String originalText = rewriter.getTokenStream().getText(Interval.of(startTokenIndex, stopTokenIndex));
            //         allText.append(originalText).append("\n");
            //     }
            
            //     rewriter.replace(ctx.start.getTokenIndex(), ctx.stop.getTokenIndex(), allText.toString());
            //     System.out.println("warning: '$IF|$THEN|$ELSE|$END' is removed ");
            //     return visitChildren(ctx); 
            // }
            //移除重复声明的变量 存储过程自己的变量声明
            @Override
            public Void visitSeq_of_declare_specs(PlSqlParser.Seq_of_declare_specsContext ctx) {
                Map<String, PlSqlParser.Declare_specContext> lastOccurrence = new HashMap<>();
                List<PlSqlParser.Declare_specContext> declareSpecs = ctx.declare_spec();
    
                // 记录每个变量名的最后一个声明
                for (PlSqlParser.Declare_specContext declareSpec : declareSpecs) {
                    if (declareSpec.variable_declaration() != null) {
                        String varName = declareSpec.variable_declaration().identifier().id_expression().regular_id().getText();
                        lastOccurrence.put(varName, declareSpec);
                    }
                }
    
                // 删除所有重复的声明，保留最后一个
                for (PlSqlParser.Declare_specContext declareSpec : declareSpecs) {
                    if (declareSpec.variable_declaration() != null) {
                        String varName = declareSpec.variable_declaration().identifier().id_expression().regular_id().getText();
                        if (lastOccurrence.get(varName) != declareSpec) {
                            rewriter.delete(declareSpec.getStart(), declareSpec.getStop());
                        }
                    }
                }
    
                return visitChildren(ctx);
            }
            //移除重复声明的变量 存储过程内部匿名块的变量声明
            @Override
            public Void visitBlock(PlSqlParser.BlockContext ctx) {
                Map<String, PlSqlParser.Declare_specContext> lastOccurrence = new HashMap<>();
                List<PlSqlParser.Declare_specContext> declareSpecs = ctx.declare_spec();
    
                // 记录每个变量名的最后一个声明
                for (PlSqlParser.Declare_specContext declareSpec : declareSpecs) {
                    if (declareSpec.variable_declaration() != null) {
                        String varName = declareSpec.variable_declaration().identifier().id_expression().regular_id().getText();
                        lastOccurrence.put(varName, declareSpec);
                    }
                }
    
                // 删除所有重复的声明，保留最后一个
                for (PlSqlParser.Declare_specContext declareSpec : declareSpecs) {
                    if (declareSpec.variable_declaration() != null) {
                        String varName = declareSpec.variable_declaration().identifier().id_expression().regular_id().getText();
                        if (lastOccurrence.get(varName) != declareSpec) {
                            rewriter.delete(declareSpec.getStart(), declareSpec.getStop());
                            logger.warn("Deleted duplicate variable declaration: file: {}, line: {}, position: {}, {}", inputFile, declareSpec.getStart().getLine(), declareSpec.getStart().getCharPositionInLine(), declareSpec.getText());
                        }
                    }
                }
    
                return visitChildren(ctx);
            }
    
            //对于lag/lead函数，处理第三个参数的类型
            @Override
            public Void visitStandard_function(PlSqlParser.Standard_functionContext ctx) {
                if (ctx.other_function() != null ){
                    String functionName = ctx.other_function().getChild(0).getText().toLowerCase();
                    if (functionName.toLowerCase().equals("lag") || functionName.toLowerCase().equals("lead")) {
                        PlSqlParser.Function_argument_analyticContext argsCtx = ctx.other_function().function_argument_analytic();
                        if (argsCtx.argument().size() == 3) {
                            // 检查第三个参数是否是数字常量
                            if (argsCtx.argument(2).getText().matches("\\d+")) {
                            // // 检查第一个参数的表达式是否包含 `/` 除号
                            // if (argsCtx.argument(0).getText().contains("/") && !config.get("rownum_type_compat").equals("on")) {
                            //         // 在第三个参数后面接上 `/1`
                            //         rewriter.insertAfter(argsCtx.argument(2).getStop(), "/1");
                            //         logger.warn("Inserted '/1' after token index: file: {}, line: {}, position: {}, {}", inputFile, argsCtx.argument(2).getStop().getLine(), argsCtx.argument(2).getStop().getCharPositionInLine(), argsCtx.argument(2).getText());
                            //     } else {
                            //         rewriter.insertAfter(argsCtx.argument(2).getStop(), ".0");
                            //         logger.warn("Inserted '.0' after token index: file: {}, line: {}, position: {}, {}", inputFile, argsCtx.argument(2).getStop().getLine(), argsCtx.argument(2).getStop().getCharPositionInLine(), argsCtx.argument(2).getText());
                            //     }
                                rewriter.replace(argsCtx.argument(2).start,argsCtx.argument(2).stop,"'"+argsCtx.argument(2).getText()+"'");
                                logger.warn("add '' on third argument in lead/lag function : file: {}, line: {}, position: {}, {}", inputFile, argsCtx.argument(2).start.getLine(), argsCtx.argument(2).start.getCharPositionInLine(), argsCtx.argument(2).getText());
                            }
                        }
                    }
                }
                return visitChildren(ctx);
            }
    
            //移除参数中的nocopy
            @Override
            public Void visitParameter(PlSqlParser.ParameterContext ctx) {
                if (ctx.NOCOPY() != null) {
                    // 删除 nocopy
                    for (TerminalNode nocopyNode : ctx.NOCOPY()) {
                        rewriter.delete(nocopyNode.getSymbol());
                        logger.debug("Deleted 'NOCOPY' at file: {}, line: {}, position: {}, {}", inputFile, nocopyNode.getSymbol().getLine(), nocopyNode.getSymbol().getCharPositionInLine(), nocopyNode.getSymbol().getText());
                    }
                }
                return visitChildren(ctx);
            }
    
            @Override
            public Void visitQuery_block(PlSqlParser.Query_blockContext ctx) {
                //将 unique 改为 distinct
                if (ctx.UNIQUE() != null) {
                    // 将 unique 改为 distinct
                    rewriter.replace(ctx.UNIQUE().getSymbol(), "distinct");
                    logger.debug("Replaced 'UNIQUE' with 'DISTINCT' at file: {}, line: {}, position: {}, {}", inputFile, ctx.UNIQUE().getSymbol().getLine(), ctx.UNIQUE().getSymbol().getCharPositionInLine(), ctx.UNIQUE().getSymbol().getText());
                }
                String queryBlockText = ctx.getText();
                //query_block 替换
                if (queryBloackMapping.containsKey(queryBlockText)){
                    rewriter.replace(ctx.start,ctx.stop,queryBloackMapping.get(ctx.getText()));
                    logger.warn("Replaced query block with file: {}, line: {}, position: {}, {}", inputFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.getText());
                    return null;//基于块的替换不再visit子节点
                }
                //基于query_block的hash值替换
                String hashCode = String.valueOf(queryBlockText.hashCode());
                try (InputStream input = getConfigInputStream("config/query_block_hash_map/" + hashCode)) {
                    if (input != null) {
                        // 使用更兼容的方式读取InputStream
                        byte[] bytes = new byte[input.available()];
                        input.read(bytes);
                        //String newText = new String(bytes, sourceEncoding);
                        // 使用UTF-8编码读取哈希映射文件内容，因为这些文件统一使用UTF-8编码存储
                        String newText = new String(bytes, "UTF-8");
                        rewriter.replace(ctx.start, ctx.stop, newText);
                        logger.warn("Replaced query block with file: {}, hash code: {}", inputFile, hashCode);
                        return null;//基于块的替换不再visit子节点
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
    
                return visitChildren(ctx);
            }
    
            //替换数据类型
            @Override
            public Void visitNative_datatype_element(PlSqlParser.Native_datatype_elementContext ctx) {
                String text = ctx.getText().toLowerCase();
                if (datetypeMapping.containsKey(text)) {
                    String newText = datetypeMapping.get(text);
                    rewriter.replace(ctx.start, ctx.stop, newText);
                    logger.debug("Replaced native datatype element with file: {}, line: {}, position: {}, {}", inputFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.getText());
                }
                return visitChildren(ctx);
            }
    
            // 将member of 改成 =any
            @Override
            public Void visitMultiset_expression(PlSqlParser.Multiset_expressionContext ctx) {
                String newExpr;
                if (ctx.MEMBER() != null && ctx.OF() != null) {
                    // 获取左侧和右侧表达式
                    String leftExpr = ctx.getChild(0).getText();
                    String rightExpr = ctx.getChild(ctx.getChildCount() - 1).getText();
                    // 构建新的表达式
                    if (ctx.NOT() != null ){
                     newExpr = "not (" + leftExpr + " = any(" + rightExpr + "))";
                    } else {
                     newExpr = " (" + leftExpr + " = any(" + rightExpr + "))";
                    }
                    // 替换原有表达式
                    rewriter.replace(ctx.getStart(), ctx.getStop(), newExpr);
                    logger.warn("Replaced multiset expression with file: {}, line: {}, position: {}, {}", inputFile, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), ctx.getText());
                }
                return visitChildren(ctx);
            }
    
            //将group by 前面的having 移动到group by后
            @Override
            public Void visitGroup_by_clause(PlSqlParser.Group_by_clauseContext ctx) {
                PlSqlParser.Having_clauseContext havingClause = null;
                Integer groupByFound =0;
                // 查找 having_clause
                for (ParseTree child : ctx.children) {
                    if (child instanceof PlSqlParser.Group_by_elementsContext){
                        groupByFound=1;
                    }
                    if (child instanceof PlSqlParser.Having_clauseContext && groupByFound!=1) {
                        havingClause = (PlSqlParser.Having_clauseContext) child;
                        break;
                    }
                }
    
                // 调试信息
                // System.out.println("Found having clause: " + (havingClause != null));
                // if (ctx.group_by_elements() != null) {
                //     for (PlSqlParser.Group_by_elementsContext element : ctx.group_by_elements()) {
                //         System.out.println("Group by element: " + element.getText());
                //     }
                // }
    
                // 如果存在 having_clause 且存在 group_by_elements
                if (havingClause != null && ctx.group_by_elements() != null && !ctx.group_by_elements().isEmpty()) {
                    // 删除原有的 having_clause
                    rewriter.delete(havingClause.getStart(), havingClause.getStop());
    
                    // 获取原始的having_clause文本
                    TokenStream tokens = rewriter.getTokenStream();
                    String havingText = tokens.getText(havingClause.getSourceInterval());
                    rewriter.insertAfter(ctx.group_by_elements(ctx.group_by_elements().size() - 1).getStop(), " " + havingText);
                    logger.warn("Moved having clause to group by clause: file: {}, line: {}, position: {}, {}", inputFile, ctx.group_by_elements(ctx.group_by_elements().size() - 1).getStop().getLine(), ctx.group_by_elements(ctx.group_by_elements().size() - 1).getStop().getCharPositionInLine(), havingText);
                }
                return visitChildren(ctx);
            }
    
            //将result_cahce子句替换成immutable
            @Override 
            public Void visitResult_cache_clause(PlSqlParser.Result_cache_clauseContext ctx) { 
                rewriter.replace(ctx.getStart(), ctx.getStop(), "IMMUTABLE");
                logger.debug("Replaced result_cache clause with file: {}, line: {}, position: {}, {}", inputFile, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), ctx.getText());
                return visitChildren(ctx); 
            }
    
            //将static_returning_clause中的RETURN替换成RETURNING
            @Override
            public Void visitStatic_returning_clause(PlSqlParser.Static_returning_clauseContext ctx) {
                // 寻找第一个子节点，它应该是RETURN或RETURNING关键字
                for (int i = 0; i < ctx.getChildCount(); i++) {
                    ParseTree child = ctx.getChild(i);
                    if (child instanceof TerminalNode) {
                        TerminalNode node = (TerminalNode) child;
                        if (node.getText().equalsIgnoreCase("RETURN")) {
                            // 将RETURN替换为RETURNING
                            rewriter.replace(node.getSymbol(), "RETURNING");
                            logger.debug("Replaced 'RETURN' with 'RETURNING' at file: {}, line: {}, position: {}", 
                                inputFile, node.getSymbol().getLine(), node.getSymbol().getCharPositionInLine());
                            break;
                        }
                    }
                }
                return visitChildren(ctx);
            }
    
            //将deterministic替换成immutable
            @Override
            public Void visitCreate_function_body(PlSqlParser.Create_function_bodyContext ctx) {
                for (ParseTree child : ctx.children) {
                    if (child.getText().toLowerCase().equals("deterministic")) {
                        rewriter.replace(child.getSourceInterval().a, child.getSourceInterval().b, "immutable");
                        if (child instanceof TerminalNode) {
                            Token token = ((TerminalNode) child).getSymbol();
                            logger.debug("Replaced deterministic with immutable at file: {}, line: {}, position: {}, {}", 
                                inputFile, token.getLine(), token.getCharPositionInLine(), child.getText());
                        } else {
                            logger.debug("Replaced deterministic with immutable at file: {}", inputFile);
                        }
                    }
                }
                return visitChildren(ctx);
            }
            //将deterministic替换成immutable
            @Override
            public Void visitFunction_body(PlSqlParser.Function_bodyContext ctx) {
                // 原有的deterministic替换逻辑
                for (ParseTree child : ctx.children) {
                    if (child.getText().toLowerCase().equals("deterministic")) {
                        rewriter.replace(child.getSourceInterval().a, child.getSourceInterval().b, "immutable");
                        logger.debug("Replaced deterministic with immutable at file: {}", inputFile);
                    }
                }
    
                // 新增的替换函数体的逻辑
                if (currentPackageName != null && ctx.identifier() != null) {
                    String functionName = ctx.identifier().getText().toLowerCase();
                    boolean replaced = replaceSubprogram(currentPackageName, functionName, ctx);
                    if (replaced) {
                        return null; // 如果替换成功，不再访问子节点
                    }
                }
                return visitChildren(ctx);
            }
            //将deterministic替换成immutable
            @Override 
            public Void visitFunction_spec(PlSqlParser.Function_specContext ctx) { 
                for (ParseTree child : ctx.children) {
                    if (child.getText().toLowerCase().equals("deterministic") |
                        child.getText().toLowerCase().equals("result_cache") ) {
                        rewriter.replace(child.getSourceInterval().a, child.getSourceInterval().b, "immutable");
                        if (child instanceof TerminalNode) {
                            Token token = ((TerminalNode) child).getSymbol();
                            logger.debug("Replaced deterministic with immutable at file: {}, line: {}, position: {}, {}", 
                                inputFile, token.getLine(), token.getCharPositionInLine(), child.getText());
                        } else {
                            logger.debug("Replaced deterministic with immutable at file: {}", inputFile);
                        }
                    }
                }
                return visitChildren(ctx); 
            }
    
            //替换异常名称
            @Override
            public Void visitException_name(PlSqlParser.Exception_nameContext ctx) {
                String text = ctx.getText().toLowerCase();
                //System.out.println("Visiting exception_name: " + text);
                if (exceptionMapping.containsKey(text)) {
                    String newText = exceptionMapping.get(text);
                    //System.out.println("Replacing " + text + " with " + newText);
                    rewriter.replace(ctx.start, ctx.stop, newText);
                    logger.debug("Replaced exception name with file: {}, line: {}, position: {}, {}", inputFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.getText());
                }
                return visitChildren(ctx);
            }
    
            //替换 call procedure
            @Override
            public Void visitCall_statement(PlSqlParser.Call_statementContext ctx) {
                String functionName = ctx.routine_name().get(0).getText();
                //替换raise_application_error
                if (functionName.equalsIgnoreCase("raise_application_error")) {
                    // Replace function name
                    rewriter.replace(ctx.routine_name().get(0).start, ctx.routine_name().get(0).stop, "report_application_error");
                    logger.debug("Replaced raise_application_error with file: {}, line: {}, position: {}, {}", 
                        inputFile, ctx.routine_name().get(0).start.getLine(), 
                        ctx.routine_name().get(0).start.getCharPositionInLine(), ctx.getText());
                    // Swap the first and second arguments
                    PlSqlParser.Function_argumentContext args = ctx.function_argument().get(0);
                    if (args.argument().size() == 2) {
                        String firstArg = args.argument(0).getText();
                        String secondArg = args.argument(1).getText();
                        rewriter.replace(args.argument(0).start, args.argument(0).stop, secondArg);
                        rewriter.replace(args.argument(1).start, args.argument(1).stop, firstArg);
                        logger.debug("Replaced raise_application_error arguments with file: {}, line: {}, position: {}, {}", 
                            inputFile, args.start.getLine(), args.start.getCharPositionInLine(), args.getText());
                    }
                }
    
                // 替换dbms_obfuscation_toolkit.md5(input raw,checksum raw)
                if (functionName.equalsIgnoreCase("dbms_obfuscation_toolkit.md5")){
                    PlSqlParser.Function_argumentContext args = ctx.function_argument().get(0);
                    if (args.argument(0).identifier().getText().equalsIgnoreCase("input") && args.argument(1).identifier().getText().equalsIgnoreCase("checksum")) {
                        rewriter.replace(ctx.start,ctx.stop,args.argument(1).expression().getText() 
                        + " := hextoraw(md5(dbe_raw.cast_to_varchar2(" 
                        + args.argument(0).expression().getText() + ")))");
                        logger.warn("Replaced dbms_obfuscation_toolkit.md5 with file: {}, line: {}, position: {}, {}", inputFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.getText());
                    }
                }
           
                return visitChildren(ctx);
            }
    
            //替换函数表达式
            @Override
            public Void visitGeneral_element(PlSqlParser.General_elementContext ctx) {
                //替换固定的函数表达式
                String text = ctx.getText().toLowerCase();
                if (generalElementMapping.containsKey(text)) {
                    rewriter.replace(ctx.start, ctx.stop, generalElementMapping.get(text));
                    logger.warn("Replaced general element with file: {}, line: {}, position: {}, {}", inputFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.getText());
                }
    
                //替换dbms_obfuscation_toolkit.md5(input raw)
                if (ctx.general_element() != null &&
                    ctx.general_element().getText().equalsIgnoreCase("dbms_obfuscation_toolkit") &&
                    ctx.general_element_part(0).id_expression().getText().equalsIgnoreCase("md5") &&
                    ctx.general_element_part(0).function_argument(0).argument(0).identifier().getText().equalsIgnoreCase("input")){
                        rewriter.replace(ctx.start,ctx.stop,"hextoraw(md5(dbe_raw.cast_to_varchar2("
                        +ctx.general_element_part(0).function_argument(0).argument(0).expression().getText()
                        +")))");
                        logger.warn("Replaced dbms_obfuscation_toolkit.md5(input raw) with file: {}, line: {}, position: {}, {}", inputFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.getText());
                }
    
                //替换grouping_id
                if (ctx.general_element_part(0) != null &&
                    ctx.general_element_part(0).id_expression().regular_id() != null &&
                    ctx.general_element_part(0).id_expression().regular_id().non_reserved_keywords_pre12c() != null &&
                    ctx.general_element_part(0).id_expression().regular_id().non_reserved_keywords_pre12c().getText().equalsIgnoreCase("grouping_id")){
                    rewriter.replace(ctx.general_element_part(0).id_expression().regular_id().non_reserved_keywords_pre12c().start,
                                     ctx.general_element_part(0).id_expression().regular_id().non_reserved_keywords_pre12c().stop, "grouping");
                    logger.debug("Replaced grouping_id with file: {}, line: {}, position: {}, {}", inputFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.getText());
                }
                 // 检查是否是replace函数
                if (ctx.general_element_part(0) != null &&
                ctx.general_element_part(0).id_expression().regular_id() != null &&
                ctx.general_element_part(0).id_expression().regular_id().non_reserved_keywords_pre12c() != null &&
                ctx.general_element_part(0).id_expression().regular_id().non_reserved_keywords_pre12c().getText().equalsIgnoreCase("replace")) {
                    // 获取参数列表
                    List<PlSqlParser.ArgumentContext> arguments = ctx.general_element_part(0).function_argument(0).argument();
                    if (arguments.size() == 3 && arguments.get(0).identifier() != null && arguments.get(1).identifier() != null && arguments.get(2).identifier() != null) {
                        String srcstrValue = null;
                        String oldsubValue = null;
                        String newsubValue = null;
                        // 遍历参数列表，匹配参数名称并获取对应的值
                        for (PlSqlParser.ArgumentContext argument : arguments) {
    
                            String argName = argument.identifier().id_expression().regular_id().getText().toUpperCase();
                            String argValue = argument.expression().getText();
                
                            switch (argName) {
                                case "SRCSTR":
                                    srcstrValue = argValue;
                                    break;
                                case "OLDSUB":
                                    oldsubValue = argValue;
                                    break;
                                case "NEWSUB":
                                    newsubValue = argValue;
                                    break;
                            }
                        }
                
                        // 检查是否所有参数都已匹配
                        if (srcstrValue != null && oldsubValue != null && newsubValue != null) {
                            rewriter.replace(ctx.start, ctx.stop, "replace(" + srcstrValue + ", " + oldsubValue + ", " + newsubValue + ")");
                            logger.warn("Replaced replace function with file: {}, line: {}, position: {}, {}", inputFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.getText());
                            return null;
                        }
                    }
                }
                //检查是否存在于udtlist中,且参数个数大于0
                if (ctx.general_element_part(0) != null &&
                    ctx.general_element_part(0).id_expression().regular_id() != null &&
                    (udtlist.contains(ctx.general_element_part(0).id_expression().regular_id().getText().toUpperCase()) ||
                     matchUdtPattern(ctx.general_element_part(0).id_expression().regular_id().getText().toUpperCase(), udtPatterns)) &&
                     ctx.general_element_part(0).function_argument() != null &&
                     ctx.general_element_part(0).function_argument(0) != null &&
                     ctx.general_element_part(0).function_argument(0).argument() != null &&
                    ctx.general_element_part(0).function_argument(0).argument().size() > 0 &&
                    isInSelectStatement(ctx)) { // 添加判断是否在SELECT语句中的条件
                      String udtName = ctx.general_element_part(0).id_expression().regular_id().getText();
                      PlSqlParser.Function_argumentContext args = ctx.general_element_part(0).function_argument(0);
                    if (args != null && !args.getText().contains("=>")){
                        TokenStream tokens = rewriter.getTokenStream();
                        String argsText = tokens.getText(args.getSourceInterval());
                        //String argsText = args.getText();
                        rewriter.replace(ctx.start, ctx.stop, "cast(" + argsText + " as " + udtName + ")");
                        logger.debug("UDT type converted in SELECT statement: file: {},  {} -> cast({} as {})", inputFile, ctx.getText(), argsText, udtName);
                    }
                }
                return visitChildren(ctx);
            }
    
            // 工具方法：检查节点是否在SELECT语句中
            private boolean isInSelectStatement(ParseTree ctx) {
                ParseTree parent = ctx.getParent();
                while (parent != null) {
                    if (parent instanceof PlSqlParser.Select_statementContext ||
                        parent instanceof PlSqlParser.Subquery_basic_elementsContext ||
                        parent instanceof PlSqlParser.Query_blockContext) {
                        return true;
                    }
                    parent = parent.getParent();
                }
                return false;
            }
    
            // 工具方法：检查字符串是否匹配任何通配符模式
            private boolean matchUdtPattern(String text, List<Pattern> patterns) {
                for (Pattern pattern : patterns) {
                    if (pattern.matcher(text).matches()) {
                        return true;
                    }
                }
                return false;
            }
    
            // join 单表名带括号时去掉括号
            @Override
            public Void visitTable_ref_aux_internal_two(PlSqlParser.Table_ref_aux_internal_twoContext ctx) {
                // 检查是否只有一个表
                if (ctx.getChildCount() == 3 && ctx.getChild(0).getText().equals("(") && ctx.getChild(2).getText().equals(")")) {
                    
                    if (ctx.getChild(1) instanceof PlSqlParser.Table_refContext) {
                        PlSqlParser.Table_refContext tableRefCtx = (PlSqlParser.Table_refContext) ctx.getChild(1);
                        if (tableRefCtx.table_ref_aux() != null && tableRefCtx.table_ref_aux().table_ref_aux_internal() != null) {
                            PlSqlParser.Table_ref_aux_internalContext internalCtx = tableRefCtx.table_ref_aux().table_ref_aux_internal();
                            if (internalCtx.getChildCount() == 1 && internalCtx.getChild(0) instanceof PlSqlParser.Dml_table_expression_clauseContext) {
                                PlSqlParser.Dml_table_expression_clauseContext dmlCtx = (PlSqlParser.Dml_table_expression_clauseContext) internalCtx.getChild(0);
                                if (dmlCtx.tableview_name() != null && dmlCtx.tableview_name().identifier() != null) {
                                    
                                    // 获取中间的表名
                                    String tableName = dmlCtx.tableview_name().identifier().getText();
                                    // 确保只替换单个表的情况
                                    if (ctx.getParent() instanceof PlSqlParser.Table_ref_auxContext) {
                                        //PlSqlParser.Table_ref_auxContext parentCtx = (PlSqlParser.Table_ref_auxContext) ctx.getParent();
                                        //System.out.println("********* "+ctx.getText()+"  ************"+ctx.getChildCount());
                                        //System.out.println("********* "+parentCtx.getText()+"  ************"+parentCtx.getChildCount());
                                        //System.out.println("********* "+parentCtx.table_ref_aux_internal().getText()+"  ************");
                                        
                                        if (!ctx.getText().contains(",") && !ctx.getText().toLowerCase().contains("join") ){
                                            rewriter.replace(ctx.start, ctx.stop, " " + tableName + " ");
                                            logger.warn("Replaced table name with file: {}, line: {}, position: {}, {}", inputFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.getText());
                                        }
                                        // if (parentCtx.getChildCount() == 1) {
                                        //     // 替换括号并前后补空格
                                        //     rewriter.replace(ctx.start, ctx.stop, " " + tableName + " ");
                                        // }
                                    }
                                }
                            }
                        }
                    }
                }
                return visitChildren(ctx);
            }
    
            //移除xmlparse中的wellformed
            @Override
            public Void visitOther_function(PlSqlParser.Other_functionContext ctx) {
                // 检查是否为 xmlparse 函数
                //System.out.println("Function name: " + ctx.getChild(0).getText());
                if (ctx.getChild(0).getText().equalsIgnoreCase("xmlparse")) {
                    //System.out.println("Found xmlparse function");
                    // 遍历子节点，查找 wellformed 并删除
                    for (int i = 0; i < ctx.getChildCount(); i++) {
                        ParseTree child = ctx.getChild(i);
                        //System.out.println("Child node type: " + child.getClass().getSimpleName() + ", text: " + child.getText());
                        if (child instanceof TerminalNode) {
                            TerminalNode terminalNode = (TerminalNode) child;
                            if (terminalNode.getText().equalsIgnoreCase("wellformed")) {
                                //System.out.println("Found wellformed, deleting...");
                                rewriter.delete(terminalNode.getSymbol());
                                logger.warn("Deleted wellformed at file: {}, line: {}, position: {}, {}", inputFile, terminalNode.getSymbol().getLine(), terminalNode.getSymbol().getCharPositionInLine(), terminalNode.getSymbol().getText());
                            }
                        }
                    }
                }
                return visitChildren(ctx);
            }
    
            private boolean isToDateFunction(PlSqlParser.ConcatenationContext ctx) {
                if (ctx.model_expression() != null &&
                    ctx.model_expression().unary_expression() != null &&
                    ctx.model_expression().unary_expression().standard_function() != null &&
                    ctx.model_expression().unary_expression().standard_function().string_function() != null &&
                    ctx.model_expression().unary_expression().standard_function().string_function().TO_DATE() != null) {
                    return true;
                }
                return false;
            }
            //两个TO_DATE函数相减，套一层intervaltonum函数
            @Override
            public Void visitConcatenation(PlSqlParser.ConcatenationContext ctx) {
                if (ctx.concatenation().size() == 2 &&
                    isToDateFunction(ctx.concatenation(0)) &&
                    isToDateFunction(ctx.concatenation(1))) {
                    
                    // 在 ctx.start 前插入 "intervaltonum("
                    rewriter.insertBefore(ctx.start, "intervaltonum(");
                    
                    // 在 ctx.stop 后插入 ")"
                    rewriter.insertAfter(ctx.stop, ")");
                    logger.warn("Replaced TO_DATE function - TO_DATE function with intervaltonum at file: {}, line: {}, position: {}, {}", inputFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.getText());
                }
                return visitChildren(ctx);
            }
            // 替换字段名、变量名
            @Override 
            public Void visitRegular_id(PlSqlParser.Regular_idContext ctx) {
                String text = ctx.getText().toLowerCase();
                if (regularIdMapping.containsKey(text)) {
                    String newText = regularIdMapping.get(text);
                    rewriter.replace(ctx.start, ctx.stop, newText);
                    logger.warn("Replaced regular_id with file: {}, line: {}, position: {}, {}", inputFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.getText());
                }
                return visitChildren(ctx); 
            }
    
            // 处理数据字典字段名转大写
            private boolean isInSelectListEle(ParseTree ctx) {
                ParseTree parent = ctx.getParent();
                while (parent != null) {
                    if (parent instanceof PlSqlParser.Select_list_elementsContext) {
                        PlSqlParser.Select_list_elementsContext SelectListEleCtx = (PlSqlParser.Select_list_elementsContext) parent;
                        if (SelectListEleCtx.column_alias() ==null) {
                            return true;
                        } else {
                            return false;  
                        }
                    }
                    parent = parent.getParent();
                }
                return false;
            }
            private boolean isInUpperOrLower(ParseTree ctx){
                ParseTree parent = ctx.getParent();
                while (parent != null) {
                    if (parent instanceof PlSqlParser.General_element_partContext) {
                        PlSqlParser.General_element_partContext GeneralElementPartCtx = (PlSqlParser.General_element_partContext) parent;
                        if (GeneralElementPartCtx.id_expression()!=null&&(
                              GeneralElementPartCtx.id_expression().getText().equalsIgnoreCase("upper") ||
                              GeneralElementPartCtx.id_expression().getText().equalsIgnoreCase("lower") )
                           ) {
                            return true;
                        } else {
                            return false;  
                        }
                    }
                    parent = parent.getParent();
                }
                return false;
            }
            @Override 
            public Void visitAtom(PlSqlParser.AtomContext ctx) {
                String keyname  ; 
                if (ctx.general_element() !=null &&
                    ctx.general_element().general_element_part(0)!=null &&
                    ctx.general_element().general_element_part(0).id_expression().regular_id()!=null ){
                     //处理onwer需要转成其他字段的情况
                     keyname=ctx.general_element().general_element_part(0).id_expression().regular_id().getText().toLowerCase();
                     if (keyname.equals("owner") && config.get("ownerconvert") != null){
                        rewriter.replace(ctx.general_element().general_element_part(0).id_expression().regular_id().start,
                                         ctx.general_element().general_element_part(0).id_expression().regular_id().stop,
                                         config.get("ownerconvert"));
                        logger.warn("Replaced owner with file: {}, line: {}, position: {}, {}", inputFile, ctx.general_element().general_element_part(0).id_expression().regular_id().start.getLine(), ctx.general_element().general_element_part(0).id_expression().regular_id().start.getCharPositionInLine(), ctx.general_element().general_element_part(0).id_expression().regular_id().getText());
                     }
                     // 处理regexp_substr第二个参数不合规的情况
                     if (keyname.equals("regexp_substr")){
                        PlSqlParser.ArgumentContext arg =ctx.general_element().general_element_part(0).function_argument(0).argument(1);
                        String processAfteString=processRegexpSubstr(arg.getText());
                        if (!arg.getText().equals(processAfteString)){
                            rewriter.replace(arg.start,arg.stop,processAfteString);
                            int line = arg.start.getLine();
                            int charPositionInLine = arg.start.getCharPositionInLine();
                            logger.warn("Converting regexp_substr second argument, file: {}, Line: {}, Position: {}, contentText: {}, convertTo: {}", 
                                inputFile, line, charPositionInLine, arg.getText(), processAfteString);
                            return null;
                        }
                     }
                     //处理字段需要用函数处理的情况
                     if (atomMapping.containsKey(keyname) && !isInUpperOrLower(ctx)){
                        if (isInSelectListEle(ctx)) {
                            rewriter.replace(ctx.start, ctx.stop, atomMapping.get(keyname)+"("+ctx.getText().toLowerCase().replace("owner",config.get("ownerconvert"))+") as "+ keyname);
                        } else {
                            rewriter.replace(ctx.start, ctx.stop, atomMapping.get(keyname)+"("+ctx.getText().toLowerCase().replace("owner",config.get("ownerconvert"))+")");
                        }
                        logger.warn("Replaced atom with file: {}, line: {}, position: {}, {}", inputFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.getText());
                }
            }
                
                // 处理 outer_join_sign (+) 如果存在且在特定情况下可以移除
                if (ctx.general_element() != null && ctx.outer_join_sign() != null) {
                    // 检查是否在 join_on_part 中
                    ParseTree current = ctx;
                    PlSqlParser.Join_on_partContext joinOnPart = null;
                    PlSqlParser.Join_clauseContext joinClause = null;
                    
                    // 向上查找父节点
                    while (current.getParent() != null) {
                        current = current.getParent();
                        if (current instanceof PlSqlParser.Join_on_partContext) {
                            joinOnPart = (PlSqlParser.Join_on_partContext) current;
                        } else if (current instanceof PlSqlParser.Join_clauseContext) {
                            joinClause = (PlSqlParser.Join_clauseContext) current;
                            break;
                        }
                    }
                    
                    // 如果找到了 join_on_part 和 join_clause
                    if (joinOnPart != null && joinClause != null) {
                        // 检查是否有 outer_join_type（LEFT 或 RIGHT JOIN）
                        if (joinClause.outer_join_type() != null) {
                            String outerJoinType = joinClause.outer_join_type().getText().toUpperCase();
                            
                            // 获取表名信息（包括别名）
                            String leftTableAlias = "";
                            String rightTableAlias = "";
                            
                            // 获取左表信息（join_clause 的父节点的子节点中的第一个表）
                            if (joinClause.getParent() instanceof PlSqlParser.Table_ref_listContext) {
                                PlSqlParser.Table_ref_listContext tableRefList = (PlSqlParser.Table_ref_listContext) joinClause.getParent();
                                if (tableRefList.table_ref(0) != null) {
                                    leftTableAlias = getTableAlias(tableRefList.table_ref(0));
                                }
                            }
                            
                            // 获取右表信息（join_clause 中的表）
                            if (joinClause.table_ref_aux() != null) {
                                rightTableAlias = getTableAlias(joinClause.table_ref_aux());
                            }
                            
                            // 确定当前 general_element 属于哪个表
                            String currentElementTable = "";
                            if (ctx.general_element().general_element() != null) {
                                // 如果有表前缀（如 t2.id），获取表前缀
                                currentElementTable = ctx.general_element().general_element().getText();
                            }
                            
                            // 增加日志输出帮助调试
                            //logger.info("JOIN Info - leftTableAlias: {}, rightTableAlias: {}, currentElementTable: {}, joinType: {}, at line: {}",
                            //    leftTableAlias, rightTableAlias, currentElementTable, outerJoinType, 
                            //    ctx.outer_join_sign().start.getLine());
                            
                            // 根据外连接类型和表位置决定是否移除 outer_join_sign
                            boolean shouldRemove = false;
                            
                            if (outerJoinType.contains("LEFT")) {
                                // 在 LEFT JOIN 中，如果 (+) 在右表的字段上，可以删除
                                if (currentElementTable.equals(rightTableAlias)) {
                                    shouldRemove = true;
                                    //logger.info("Removing (+) from right table field in LEFT JOIN");
                                //} else {
                                    // 在 LEFT JOIN 中，如果 (+) 在左表字段上，不应该删除
                                    //logger.info("Keeping (+) on left table field in LEFT JOIN at line: {}", 
                                    //    ctx.outer_join_sign().start.getLine());
                                }
                            } else if (outerJoinType.contains("RIGHT")) {
                                // 在 RIGHT JOIN 中，如果 (+) 在左表的字段上，可以删除
                                
                                // 如果currentElementTable与leftTableAlias匹配，或者是左表的引用
                                // 注：对于RIGHT JOIN，左表的字段上有(+)的情况总是可以删除的
                                if (currentElementTable.equals(leftTableAlias) || 
                                    (currentElementTable.length() > 0 && !currentElementTable.equals(rightTableAlias))) {
                                    shouldRemove = true;
                                    //logger.info("Removing (+) from left table field in RIGHT JOIN");
                                //} else {
                                    // 在 RIGHT JOIN 中，如果 (+) 在右表字段上，不应该删除
                                    //logger.info("Keeping (+) on right table field in RIGHT JOIN at line: {}", 
                                    //    ctx.outer_join_sign().start.getLine());
                                }
                            }
                            
                            if (shouldRemove) {
                                // 移除 outer_join_sign
                                rewriter.delete(ctx.outer_join_sign().start, ctx.outer_join_sign().stop);
                                logger.warn("Removed outer join sign (+) at file: {}, line: {}, position: {}", 
                                    inputFile, ctx.outer_join_sign().start.getLine(), 
                                    ctx.outer_join_sign().start.getCharPositionInLine());
                            }
                        }
                    }
                }
                if (config.get("convert_table_of_ele_to_select").equals("true")){
                // 处理 id_expression(id_expression).id_expression 形式，转换成 (select xxx)
                // 1. 检查是否为 id_expression(id_expression).id_expression 结构，且内层有function_argument，外层没有
                if (ctx.general_element() != null && ctx.general_element().general_element() != null) {
                    PlSqlParser.General_elementContext ge = ctx.general_element();
                    if (ge.general_element() != null && ge.general_element_part().size() == 1) {
                        PlSqlParser.General_elementContext innerGe = ge.general_element();
                        if (innerGe.general_element_part().size() == 1) {
                            if (innerGe.general_element_part(0).function_argument().size() > 0 &&
                                ge.general_element_part(0).function_argument().size() == 0) {
            
                                boolean shouldWrap = false;
                                // Rule 1: in select_statement with FROM
                                PlSqlParser.Select_statementContext selectCtx = getParentOfType(ctx, PlSqlParser.Select_statementContext.class);
                                if (selectCtx != null) {
                                    try {
                                        if (selectCtx.select_only_statement().subquery().subquery_basic_elements().query_block().from_clause() != null) {
                                            shouldWrap = true;
                                        }
                                    } catch (Exception e) { /* ignore */ }
                                }
            
                                // Rule 2: in UPDATE or DELETE
                                if (!shouldWrap) {
                                    if (getParentOfType(ctx, PlSqlParser.Update_statementContext.class) != null ||
                                        getParentOfType(ctx, PlSqlParser.Delete_statementContext.class) != null) {
                                        shouldWrap = true;
                                    }
                                }
            
                                if (shouldWrap) {
                                    // 执行包裹
                                    String atomText = ctx.getText();
                                    rewriter.replace(ctx.start, ctx.stop, "(select " + atomText + ")");
                                    logger.warn("Rewrite id_expression(id_expression).id_expression to (select ...), file: {}, line: {}, position: {}, {}", inputFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), atomText);
                                }
                            }
                        }
                    }
                }
                }
                
                return visitChildren(ctx); 
            }
    
            // 辅助方法：获取表别名或表名
            private String getTableAlias(ParseTree tableNode) {
                String alias = "";
                
                // 处理 table_ref 节点
                if (tableNode instanceof PlSqlParser.Table_refContext) {
                    PlSqlParser.Table_refContext tableRef = (PlSqlParser.Table_refContext) tableNode;
                    
                    // 表别名在 table_ref_aux 中
                    if (tableRef.table_ref_aux() != null) {
                        if (tableRef.table_ref_aux().table_alias() != null) {
                            return tableRef.table_ref_aux().table_alias().getText();
                        }
                        
                        // 如果没有别名，尝试获取表名
                        return getTableAlias(tableRef.table_ref_aux());
                    }
                }
                // 处理 table_ref_aux 节点
                else if (tableNode instanceof PlSqlParser.Table_ref_auxContext) {
                    PlSqlParser.Table_ref_auxContext tableRefAux = (PlSqlParser.Table_ref_auxContext) tableNode;
                    
                    // 首先检查是否有表别名
                    if (tableRefAux.table_alias() != null) {
                        return tableRefAux.table_alias().getText();
                    }
                    
                    // 如果没有别名，尝试获取表名
                    if (tableRefAux.table_ref_aux_internal() != null) {
                        if (tableRefAux.table_ref_aux_internal() instanceof PlSqlParser.Table_ref_aux_internal_oneContext) {
                            PlSqlParser.Table_ref_aux_internal_oneContext internalOne = 
                                (PlSqlParser.Table_ref_aux_internal_oneContext) tableRefAux.table_ref_aux_internal();
                            
                            if (internalOne.dml_table_expression_clause() != null &&
                                internalOne.dml_table_expression_clause().tableview_name() != null) {
                                
                                return internalOne.dml_table_expression_clause().tableview_name().getText();
                            }
                        }
                    }
                }
                
                return alias;
            }
    
            // 替换for update of 字段名
            @Override
            public Void visitSelect_statement(PlSqlParser.Select_statementContext ctx) {
                // 查找是否有 for_update_clause 子句
                if (ctx.for_update_clause() != null && !ctx.for_update_clause().isEmpty()) {
                    for (PlSqlParser.For_update_clauseContext forUpdateClause : ctx.for_update_clause()) {
                        if (forUpdateClause.for_update_of_part() != null) {
                            PlSqlParser.For_update_of_partContext forUpdateOfPart = forUpdateClause.for_update_of_part();
                            for (PlSqlParser.Column_nameContext columnNameCtx : forUpdateOfPart.column_list().column_name()) {
                                String identifier = columnNameCtx.identifier().getText();
                                if (columnNameCtx.id_expression()!=null  && !columnNameCtx.id_expression().isEmpty()){
                                    rewriter.replace(columnNameCtx.start, columnNameCtx.stop, identifier);
                                    logger.warn("Replaced for update of column name with file: {}, line: {}, position: {}, {}", inputFile, columnNameCtx.start.getLine(), columnNameCtx.start.getCharPositionInLine(), columnNameCtx.getText());
                                    continue;//return visitChildren(ctx); //可能有多个
                                }
                                // 遍历 selected_list，查找是否有 identifier 前面有表名或别名
                                String newIdentifier = findIdentifierForColumn(ctx.select_only_statement().subquery().subquery_basic_elements().query_block().selected_list(), identifier);
                                if (newIdentifier == null) {
                                    // 如果 select 列表中没有找到，检查 from 子句
                                    newIdentifier = findIdentifierInFromClause(ctx.select_only_statement().subquery().subquery_basic_elements().query_block().from_clause(), identifier);
                                }
                                if (newIdentifier != null) {
                                    rewriter.replace(columnNameCtx.start, columnNameCtx.stop, newIdentifier);
                                    logger.warn("Replaced for update of column name with file: {}, line: {}, position: {}, {}", inputFile, columnNameCtx.start.getLine(), columnNameCtx.start.getCharPositionInLine(), columnNameCtx.getText());
                                } else {
                                    // 如果没有找到对应的表名或别名，则保留原字段名
                                    rewriter.replace(columnNameCtx.start, columnNameCtx.stop, identifier);
                                    logger.warn("Replaced for update of column name with file: {}, line: {}, position: {}, {}", inputFile, columnNameCtx.start.getLine(), columnNameCtx.start.getCharPositionInLine(), columnNameCtx.getText());
                                }
                            }
                        }
                    }
            
                }
        
                return visitChildren(ctx);
            }
        
            private String findIdentifierForColumn(PlSqlParser.Selected_listContext selectedList, String columnName) {
                for (PlSqlParser.Select_list_elementsContext selectElement : selectedList.select_list_elements()) {
                    if (selectElement.expression() != null) {
                        String identifier = findIdentifierInExpression(selectElement.expression(), columnName);
                        if (identifier != null) {
                            return identifier;
                        }
                    }
                }
                return null;
            }
        
            private String findIdentifierInExpression(PlSqlParser.ExpressionContext ctx, String columnName) {
                if (ctx == null) return null;
        
                for (int i = 0; i < ctx.getChildCount(); i++) {
                    ParseTree child = ctx.getChild(i);
                    String identifier = findIdentifierInNode(child, columnName);
                    if (identifier != null) {
                        return identifier;
                    }
                }
                return null;
            }
        
            private String findIdentifierInNode(ParseTree node, String columnName) {
                if (node instanceof PlSqlParser.General_elementContext) {
                    PlSqlParser.General_elementContext generalElement = (PlSqlParser.General_elementContext) node;
                    if (generalElement.general_element() != null && generalElement.general_element_part().size() == 1) {
                        String identifier = generalElement.general_element().getText();
                        String fieldName = generalElement.general_element_part(0).id_expression().getText();
                        if (fieldName.equalsIgnoreCase(columnName)) {
                            return identifier;
                        }
                    }
                } else if (node instanceof PlSqlParser.ExpressionContext) {
                    String identifier = findIdentifierInExpression((PlSqlParser.ExpressionContext) node, columnName);
                    if (identifier != null) {
                        return identifier;
                    }
                }
        
                for (int i = 0; i < node.getChildCount(); i++) {
                    ParseTree child = node.getChild(i);
                    String identifier = findIdentifierInNode(child, columnName);
                    if (identifier != null) {
                        return identifier;
                    }
                }
        
                return null;
            }
        
            private String findIdentifierInFromClause(PlSqlParser.From_clauseContext fromClause, String columnName) {
                for (PlSqlParser.Table_refContext tableRef : fromClause.table_ref_list().table_ref()) {
                    PlSqlParser.Table_ref_aux_internalContext internalCtx = tableRef.table_ref_aux().table_ref_aux_internal();
                    if (internalCtx.getChildCount() == 1 && internalCtx.getChild(0) instanceof PlSqlParser.Dml_table_expression_clauseContext) {
                        PlSqlParser.Dml_table_expression_clauseContext dmlCtx = (PlSqlParser.Dml_table_expression_clauseContext) internalCtx.getChild(0);
                        if (dmlCtx.tableview_name() != null) {
                            if (tableRef.table_ref_aux().table_alias() != null) {
                                return tableRef.table_ref_aux().table_alias().getText() ;
                            } else {
                                return dmlCtx.tableview_name().getText() ;
                            }
                        }
                    }
                }
                return null;
            }
            //移除游标定义中的into子句
            @Override public Void visitCursor_declaration(PlSqlParser.Cursor_declarationContext ctx) {
                if (ctx.select_statement().select_only_statement().subquery().subquery_basic_elements().query_block().into_clause() !=null){
                    PlSqlParser.Into_clauseContext intoCTX =ctx.select_statement().select_only_statement().subquery().subquery_basic_elements().query_block().into_clause();
                    rewriter.delete(intoCTX.start,intoCTX.stop);
                    int line = intoCTX.start.getLine();
                    int charPositionInLine = intoCTX.start.getCharPositionInLine();
                    logger.warn("Removing INTO clause from cursor declaration, file: {}, Line: {}, Position: {}, Content: {}", 
                        inputFile, line, charPositionInLine, intoCTX.getText());
                }
                return visitChildren(ctx); 
            }
            //移除open游标中的into子句
            @Override public Void visitOpen_for_statement(PlSqlParser.Open_for_statementContext ctx) {
                if (ctx.select_statement()!=null && ctx.select_statement().select_only_statement().subquery().subquery_basic_elements().query_block().into_clause() !=null){
                    PlSqlParser.Into_clauseContext intoCTX =ctx.select_statement().select_only_statement().subquery().subquery_basic_elements().query_block().into_clause();
                    rewriter.delete(intoCTX.start,intoCTX.stop);
                    int line = intoCTX.start.getLine();
                    int charPositionInLine = intoCTX.start.getCharPositionInLine();
                    logger.warn("Removing INTO clause from Open_for_statement, file: {}, Line: {}, Position: {}, Content: {}", 
                        inputFile, line, charPositionInLine, intoCTX.getText());
                }
                return visitChildren(ctx); 
            }
    
            //对于重复的表别名弹警告
            @Override
            public Void visitTable_ref_list(PlSqlParser.Table_ref_listContext ctx) {
                 Set<String> tableAliases = new HashSet<>();
                 List<String> duplicateAliases = new ArrayList<>();
                for (PlSqlParser.Table_refContext tableRef : ctx.table_ref()) {
                    if (tableRef.table_ref_aux().table_alias() != null){
                      String alias = tableRef.table_ref_aux().table_alias().identifier().getText();
                      if (!tableAliases.add(alias)) {
                          duplicateAliases.add(alias);
                      }
                   }
                }
                if (!duplicateAliases.isEmpty()) {
                    int line = ctx.start.getLine();
                    int charPositionInLine = ctx.start.getCharPositionInLine();
                    logger.warn("Found duplicate table aliases: {}, file: {}, Line: {}, Position: {}, hashCode: {}", 
                        duplicateAliases, inputFile, line, charPositionInLine, 
                        ctx.getParent().getParent().getText().hashCode());
                    logger.warn("Source query block: {}", rewriter.getTokenStream().getText(ctx.getParent().getParent().getSourceInterval()));
                } 
                return visitChildren(ctx);
            }
    
            //处理regexp_substr第二个参数不合规的
            public String processRegexpSubstr(String input) {
                      // 正则表达式匹配所有的 {}，并且没有被转义的 {}
                      StringBuilder sb = new StringBuilder();
                      Stack<Integer> stack = new Stack<>();
                      boolean inEscape = false;
              
                      for (int i = 0; i < input.length(); i++) {
                          char c = input.charAt(i);
              
                          if (c == '\\' && !inEscape) {
                              inEscape = true;
                              sb.append(c);
                          } else if (c == '{' && !inEscape) {
                              stack.push(sb.length());
                              sb.append(c);
                          } else if (c == '}' && !inEscape) {
                              if (!stack.isEmpty()) {
                                  int start = stack.pop();
                                  String content = sb.substring(start + 1);
                                  if (!content.matches("\\d+(,\\d*)?")) {
                                      sb.insert(start, '\\');
                                      sb.append('\\');
                                  }
                              }
                              sb.append(c);
                          } else {
                              inEscape = false;
                              sb.append(c);
                          }
                      }
              
                      return sb.toString();
            }
    
            private DynamicSqlResult applyDynamicSqlRules(String sql) {
                if (dynamicSqlRules == null || dynamicSqlRules.isEmpty()) {
                    return new DynamicSqlResult(sql, false, null);
                }
                
                String result = sql;
                boolean shouldRemove = false;
                String matchedPattern = null;
                
                for (Map<String, String> rule : dynamicSqlRules) {
                    String pattern = rule.get("pattern");
                    String action = rule.get("action");
                    String target = rule.getOrDefault("target", "");
                    
                    if (pattern == null || action == null) {
                        continue;
                    }
                    
                    try {
                        Pattern compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                        Matcher matcher = compiledPattern.matcher(result);
                        
                        if (matcher.find()) {
                            if ("remove".equalsIgnoreCase(action)) {
                                // 对于remove操作，标记为需要移除但不修改SQL
                                shouldRemove = true;
                                matchedPattern = pattern;
                                logger.debug("Found dynamic SQL 'remove' rule match: {}", pattern);
                                break;
                            } else if ("replace".equalsIgnoreCase(action)) {
                                String newResult = matcher.replaceAll(target);
                                if (!newResult.equals(result)) {
                                    logger.debug("Applied dynamic SQL 'replace' rule: {} -> {}", pattern, target);
                                    result = newResult;
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Error applying dynamic SQL rule {}: {}", pattern, e.getMessage());
                    }
                }
                
                return new DynamicSqlResult(result, shouldRemove, matchedPattern);
            }
            
            @Override public Void visitExecute_immediate(PlSqlParser.Execute_immediateContext ctx) { 
                // 获取EXECUTE IMMEDIATE后的表达式
                if (ctx.expression() != null) {
                    // 检查该表达式是否为字符串字面量
                    ParseTree expressionTree = ctx.expression();
                    if (expressionTree.getChildCount() > 0) {
                        // 尝试获取表达式中的第一个标记，检查它是否为字符串
                        Token firstToken = null;
                        
                        // 递归查找表达式中的第一个字符串标记
                        for (int i = 0; i < expressionTree.getChildCount(); i++) {
                            ParseTree child = expressionTree.getChild(i);
                            if (child instanceof TerminalNode) {
                                Token token = ((TerminalNode) child).getSymbol();
                                if (token.getText().startsWith("'") || token.getText().startsWith("\"")) {
                                    firstToken = token;
                                    break;
                                }
                            } else if (child instanceof ParserRuleContext && ((ParserRuleContext) child).start != null) {
                                // 如果子节点是另一个解析规则，检查它的第一个标记
                                firstToken = ((ParserRuleContext) child).start;
                                if (firstToken.getText().startsWith("'") || firstToken.getText().startsWith("\"")) {
                                    break;
                                }
                            }
                        }
                        
                        // 如果找到了字符串标记，对其应用规则
                        if (firstToken != null && (firstToken.getText().startsWith("'") || firstToken.getText().startsWith("\""))) {
                            String originalSql = firstToken.getText();
                            
                            // 移除引号（如果存在）
                            if (originalSql.startsWith("'") && originalSql.endsWith("'")) {
                                originalSql = originalSql.substring(1, originalSql.length() - 1);
                            } else if (originalSql.startsWith("\"") && originalSql.endsWith("\"")) {
                                originalSql = originalSql.substring(1, originalSql.length() - 1);
                            }
                            
                            // 应用动态SQL规则
                            DynamicSqlResult result = applyDynamicSqlRules(originalSql);
                            
                            if (result.shouldRemove()) {
                                // 如果应该移除，将整个execute_immediate节点注释掉
                                // 查找语句后面的分号
                                TokenStream tokens = rewriter.getTokenStream();
                                int stopTokenIndex = ctx.getStop().getTokenIndex();
                                int endIndex = stopTokenIndex;
                                
                                // 寻找语句后的分号
                                for (int i = stopTokenIndex + 1; i < tokens.size(); i++) {
                                    Token token = tokens.get(i);
                                    if (token.getType() == PlSqlLexer.SEMICOLON) {
                                        endIndex = i;
                                        break;
                                    } else if (token.getChannel() != Token.HIDDEN_CHANNEL) {
                                        // 遇到非隐藏通道的非分号标记时停止搜索
                                        break;
                                    }
                                }
                                
                                // 获取原始文本，包括分号
                                String originalNodeText = tokens.getText(new Interval(ctx.getStart().getTokenIndex(), endIndex));
                                
                                String commentedText = "/* Commented out execute_immediate due to matching remove rule: " + result.getMatchedPattern() + "\n" + 
                                                      originalNodeText + " */";
                                
                                // 替换文本，包括分号
                                if (endIndex > stopTokenIndex) {
                                    rewriter.replace(ctx.getStart(), tokens.get(endIndex), commentedText);
                                    logger.warn("Commented out execute_immediate with semicolon at line {} due to matching remove rule: {}", 
                                              ctx.getStart().getLine(), result.getMatchedPattern());
                                } else {
                                    rewriter.replace(ctx.getStart(), ctx.getStop(), commentedText);
                                    logger.warn("Commented out execute_immediate at line {} due to matching remove rule: {}", 
                                              ctx.getStart().getLine(), result.getMatchedPattern());
                                }
                                return null;
                            } else if (!result.getSql().equals(originalSql)) {
                                // 如果SQL被修改，则只更新字符串标记
                                String processedSql = result.getSql();
                                
                                // 重新添加引号
                                if (firstToken.getText().startsWith("'")) {
                                    processedSql = "'" + processedSql + "'";
                                } else if (firstToken.getText().startsWith("\"")) {
                                    processedSql = "\"" + processedSql + "\"";
                                }
                                
                                // 替换原始标记
                                rewriter.replace(firstToken, processedSql);
                                logger.info("Processed dynamic SQL at line {}: {}", firstToken.getLine(), processedSql);
                                return null;
                            }
                        }
                    }
                }
                
                return visitChildren(ctx); 
            }
            
            /**
             * 处理Oracle的q转义字符串，将其转换为普通的SQL字符串
             */
            @Override
            public Void visitQuoted_string(PlSqlParser.Quoted_stringContext ctx) {
                String text = ctx.getText();
                
                // 检查是否是q转义字符串
                if (text.startsWith("q'") || text.toLowerCase().startsWith("q'")) {
                    // 提取实际的字符串内容和分隔符
                    char delimiter = text.charAt(2);
                    char endDelimiter;
                    
                    // 根据开始分隔符确定结束分隔符
                    switch (delimiter) {
                        case '(': endDelimiter = ')'; break;
                        case '{': endDelimiter = '}'; break;
                        case '[': endDelimiter = ']'; break;
                        case '<': endDelimiter = '>'; break;
                        case '!':
                        case '#':
                        case '\'':
                        case '"':
                        case '~':
                        case '/':
                        case '\\': endDelimiter = delimiter; break;
                        default: return visitChildren(ctx); // 不是有效的q转义字符串，按常规处理
                    }
                    
                    // 寻找结束分隔符的位置
                    int endPos = text.lastIndexOf(endDelimiter + "'");
                    if (endPos < 3) {
                        return visitChildren(ctx); // 格式不正确，按常规处理
                    }
                    
                    // 提取字符串内容
                    String content = text.substring(3, endPos);
                    
                    // 将内容中的单引号转义（将每个单引号替换为两个单引号）
                    content = content.replace("'", "''");
                    
                    // 构造新的普通SQL字符串
                    String newText = "'" + content + "'";
                    
                    // 替换原始文本
                    rewriter.replace(ctx.getStart(), ctx.getStop(), newText);
                    logger.debug("Converted q-quoted string to standard SQL string at line {}: {} -> {}", 
                                ctx.getStart().getLine(), text, newText);
                }
                
                return visitChildren(ctx);
            }
    
            @Override
            public Void visitAssignment_statement(PlSqlParser.Assignment_statementContext ctx) {
                // 打印语法树节点结构，帮助调试
                logger.debug("Assignment statement: {}", ctx.getText());
                
                try {
                    if (ctx.expression() != null) {
                        // 获取整个表达式内容
                        String exprText = ctx.expression().getText();
                        logger.debug("Expression text: {}", exprText);
                        
                        // 如果表达式结构简单，可能是一个单独的标识符
                        if (!exprText.contains("(") && !exprText.contains(")") && !exprText.contains(".")) {
                            // 检查变量名是否符合udtlist规则
                            if (udtlist.contains(exprText.toUpperCase()) || 
                                matchUdtPattern(exprText.toUpperCase(), udtPatterns)) {
                                
                                // 在变量名后添加括号
                                rewriter.insertAfter(ctx.expression().stop, "()");
                                logger.debug("Added '()' after UDT variable in assignment: file: {}, line: {}, position: {}, {}", 
                                            inputFile, ctx.start.getLine(), ctx.start.getCharPositionInLine(), exprText);
                            }
                        }
                    }
                } catch (Exception e) {
                    // 捕获和记录任何异常，防止处理过程中断
                    logger.error("Error processing assignment statement: {}", e.getMessage(), e);
                }
                
                return visitChildren(ctx);
            }
    
            @Override
            public Void visitCreate_package(PlSqlParser.Create_packageContext ctx) {
                // 获取package名称
                if (ctx.package_name() != null && !ctx.package_name().isEmpty() && 
                    ctx.package_name().get(0).identifier() != null) {
                    String packageName = ctx.package_name().get(0).identifier().getText().toLowerCase();
                    // 查找是否存在对应的配置文件
                    String configFilePath = "config/add_func_into_pkg/spec/" + packageName;
                    try (InputStream input = getConfigInputStream(configFilePath)) {
                        if (input != null) {
                            // 读取配置文件内容
                            byte[] bytes = new byte[input.available()];
                            input.read(bytes);
                            String additionalContent = new String(bytes, "UTF-8");
                            // 寻找package的结束位置（end关键字）
                            if (ctx.END() != null) {
                                // 在END之前插入额外的内容
                                rewriter.insertBefore(ctx.END().getSymbol(), additionalContent + "\n");
                                logger.warn("Added additional functions to package '{}' from file: {}", packageName, configFilePath);
                            }
                        }
                    } catch (IOException e) {
                        logger.error("Error reading config file for package {}: {}", packageName, e.getMessage());
                    }
                }
                return visitChildren(ctx);
            }
    
            @Override
            public Void visitCreate_package_body(PlSqlParser.Create_package_bodyContext ctx) {
                // 获取package名称
                if (ctx.package_name() != null && !ctx.package_name().isEmpty() && 
                    ctx.package_name().get(0).identifier() != null) {
                    String packageName = ctx.package_name().get(0).identifier().getText().toLowerCase();
                    // 查找是否存在对应的配置文件
                    String configFilePath = "config/add_func_into_pkg/body/" + packageName;
                    try (InputStream input = getConfigInputStream(configFilePath)) {
                        if (input != null) {
                            // 读取配置文件内容
                            byte[] bytes = new byte[input.available()];
                            input.read(bytes);
                            String additionalContent = new String(bytes, "UTF-8");
                            // 查找最后一个END关键字
                            if (ctx.END() != null) {
                                // package body可能有多个END，我们需要找到最后一个
                                // 在ANTLR4生成的解析器中，通常能够直接访问最后一个匹配的标记
                                // 在END之前插入额外的内容
                                rewriter.insertBefore(ctx.END().getSymbol(), additionalContent + "\n");
                                logger.warn("Added additional functions to package body '{}' from file: {}", packageName, configFilePath);
                            }
                        }
                    } catch (IOException e) {
                        logger.error("Error reading config file for package body {}: {}", packageName, e.getMessage());
                    }
                }
                return visitChildren(ctx);
            }
    
            // 存储当前正在访问的包名
            private String currentPackageName = null;
    
            @Override
            public Void visitPackage_obj_body(PlSqlParser.Package_obj_bodyContext ctx) {
                // 向上查找包名
                PlSqlParser.Create_package_bodyContext packageBodyCtx = getParentOfType(ctx, PlSqlParser.Create_package_bodyContext.class);
                if (packageBodyCtx != null && packageBodyCtx.package_name() != null && !packageBodyCtx.package_name().isEmpty() &&
                    packageBodyCtx.package_name().get(0).identifier() != null) {
                    currentPackageName = packageBodyCtx.package_name().get(0).identifier().getText().toLowerCase();
                }
                
                Void result = visitChildren(ctx);
                
                // 重置当前包名
                currentPackageName = null;
                
                return result;
            }
    
            @Override
            public Void visitProcedure_body(PlSqlParser.Procedure_bodyContext ctx) {
                if (currentPackageName != null && ctx.identifier() != null) {
                    String procedureName = ctx.identifier().getText().toLowerCase();
                    boolean replaced = replaceSubprogram(currentPackageName, procedureName, ctx);
                    if (replaced) {
                        return null; // 如果替换成功，不再访问子节点
                    }
                }
                return visitChildren(ctx);
            }
    
            private <T extends ParserRuleContext> T getParentOfType(ParseTree node, Class<T> parentType) {
                ParseTree parent = node.getParent();
                while (parent != null) {
                    if (parentType.isInstance(parent)) {
                        return parentType.cast(parent);
                    }
                    parent = parent.getParent();
                }
                return null;
            }
    
            private boolean replaceSubprogram(String packageName, String subprogramName, ParserRuleContext ctx) {
                String configFilePath = "config/replace_func_into_pkg/" + packageName + "." + subprogramName;
                try (InputStream input = getConfigInputStream(configFilePath)) {
                    if (input != null) {
                        // 读取配置文件内容
                        byte[] bytes = new byte[input.available()];
                        input.read(bytes);
                        String replacementContent = new String(bytes, "UTF-8");
                        // 替换整个函数或存储过程
                        rewriter.replace(ctx.start, ctx.stop, replacementContent);
                        logger.warn("Replaced {}.{} with content from file: {}", 
                                   packageName, subprogramName, configFilePath);
                        return true; // 替换成功
                    }
                } catch (IOException e) {
                    logger.error("Error reading replacement file for {}.{}: {}", 
                                packageName, subprogramName, e.getMessage());
                }
                return false; // 没有替换
            }
        
        };
    }

    // 抽取Token级别处理逻辑，便于字符串和文件两种方式共用
    private static void processTokensForRewrite(CommonTokenStream tokens, TokenStreamRewriter rewriter, Path inputFile) {
        Token previousToken = null;
        Token prePreviousToken = null;
        for (Token token : tokens.getTokens()) {
            // 处理多行注释
            if (token.getType() == PlSqlLexer.MULTI_LINE_COMMENT) {
                String comment = token.getText();
                if (comment.contains("/*+")) {
                    logger.info("find sql-hint: {}",comment);
                }
                // 去除注释内部的`/`
                String modifiedComment = removeComment(comment);
                if (!comment.equals(modifiedComment)) { // 注释有变化才改写
                    rewriter.replace(token, modifiedComment);
                    logger.debug("Replaced multi-line comment with file: {}, line: {}, position: {}, {}", inputFile, token.getLine(), token.getCharPositionInLine(), token.getText());
                }
            }
            // if ((token.getType() == PlSqlLexer.MULTI_LINE_COMMENT | token.getType() == PlSqlLexer.SINGLE_LINE_COMMENT ) && previousToken != null && previousToken.getText().equals(">")) {
            //     String comment = token.getText();
            //     // 补空格
            //     rewriter.replace(token, " "+comment);
            // }

            // 处理全角逗号
            if (token.getType() == PlSqlLexer.WIDTH_COMMA) {
                rewriter.replace(token, ',');
                logger.debug("Replaced full-width comma with file: {}, line: {}, position: {}, {}", inputFile, token.getLine(), token.getCharPositionInLine(), token.getText());
            }
            // 处理全角左括号
            if (token.getType() == PlSqlLexer.WIDTH_LEFT_PAREN) {
                rewriter.replace(token, '(');
                logger.debug("Replaced full-width left parenthesis with file: {}, line: {}, position: {}, {}", inputFile, token.getLine(), token.getCharPositionInLine(), token.getText());
            }
            // 处理全角右括号
            if (token.getType() == PlSqlLexer.WIDTH_RIGHT_PAREN) {
                rewriter.replace(token, ')');
                logger.debug("Replaced full-width right parenthesis with file: {}, line: {}, position: {}, {}", inputFile, token.getLine(), token.getCharPositionInLine(), token.getText());
            }
             // 处理全角空格
            if (token.getType() == PlSqlLexer.SPACES && token.getText().equals("\u3000")) {
                rewriter.replace(token, ' ');
                logger.debug("Replaced full-width space with file: {}, line: {}, position: {}, {}", inputFile, token.getLine(), token.getCharPositionInLine(), token.getText());
            }
            // 处理 `=>-`  `=>+` `=>--` `=>/* */`
            if (prePreviousToken != null && prePreviousToken.getText().equals("=") && 
                previousToken != null && previousToken.getText().equals(">") &&
                (token.getText().equals("-") || token.getText().equals("+")||
                 token.getType() == PlSqlLexer.MULTI_LINE_COMMENT | token.getType() == PlSqlLexer.SINGLE_LINE_COMMENT)) {
                rewriter.insertBefore(token, " ");
                logger.debug("Replaced =>- or =>+ or =>-- or =>/* */ with file: {}, line: {}, position: {}, {}", inputFile, token.getLine(), token.getCharPositionInLine(), token.getText());
            }
            // 自定义token替换
            if (tokenReplaceMapping != null && tokenReplaceMapping.containsKey(token.getText().toLowerCase())){
                rewriter.replace(token,tokenReplaceMapping.get(token.getText().toLowerCase()));
                if (token.getText().toLowerCase().startsWith("$")){
                    int line = token.getLine();
                    int charPositionInLine = token.getCharPositionInLine();
                    logger.warn("remove $IF|$ELSE|$THEN$END, file: {}, Line: {}, Position: {}", inputFile, line, charPositionInLine);
                }
            }
            prePreviousToken = previousToken;
            previousToken = token;
        }

        // 替换.getclobval() 和 .getclobval
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getText().equals(".")) {
                Token nextToken = tokens.get(i + 1);
                if (nextToken != null && nextToken.getText().toLowerCase().equals("getclobval")) {
                    Token nextNextToken = tokens.get(i + 2);
                    if (nextNextToken != null && nextNextToken.getText().equals("(")) {
                        Token nextNextNextToken = tokens.get(i + 3);
                        if (nextNextNextToken != null && nextNextNextToken.getText().equals(")")) {
                            // 替换 .getclobval()
                            rewriter.replace(token, nextNextNextToken, "::clob");
                            logger.warn("Replaced .getclobval() with file: {}, line: {}, position: {}", inputFile, token.getLine(), token.getCharPositionInLine());
                            i += 3; // 跳过已处理的 token
                        } else {
                            // 替换 .getclobval
                            rewriter.replace(token, nextToken, "::clob");
                            logger.warn("Replaced .getclobval with file: {}, line: {}, position: {}", inputFile, token.getLine(), token.getCharPositionInLine());
                            i += 1; // 跳过已处理的 token
                        }
                    } else {
                        // 替换 .getclobval
                        rewriter.replace(token, nextToken, "::clob");
                        logger.warn("Replaced .getclobval with file: {}, line: {}, position: {}", inputFile, token.getLine(), token.getCharPositionInLine());
                        i += 1; // 跳过已处理的 token
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            logger.error("Usage: java PlSqlRewriter <inputFile> <outputFile> [sourceEncoding [targetEncoding]]");
            System.exit(1);
        }

        Path inputFile = Paths.get(args[0]);
        Path outputFile = Paths.get(args[1]);
        String sourceEncoding;
        String targetEncoding;
        if (args.length>2){
             sourceEncoding = args[2];
             if (args.length>3) {
             targetEncoding = args[3];
             } else {
                targetEncoding = sourceEncoding;
             }
        } else {
            sourceEncoding = "UTF-8";
            targetEncoding = "UTF-8";
        }
        
        processFile(inputFile, outputFile,sourceEncoding,targetEncoding);
    }

    /**
     * 输入SQL字符串，返回转换后的SQL字符串，便于集成调用
     */
    public static String rewriteSql(String inputSql, String sourceEncoding, String targetEncoding) throws Exception {
        return rewriteSql(inputSql, sourceEncoding, targetEncoding, null);
    }

    /**
     * 输入SQL字符串，返回转换后的SQL字符串，便于集成调用
     * @param inputSql SQL内容
     * @param sourceEncoding 源编码
     * @param targetEncoding 目标编码
     * @param fileName 文件名或逻辑名
     */
    public static String rewriteSql(String inputSql, String sourceEncoding, String targetEncoding, String fileName) throws Exception {
        logger.info("Start processing SQL string, file: {}", fileName);
        // 确保配置已加载
        if (!configsLoaded) {
            try {
                preloadAllConfigs();
            } catch (IOException e) {
                logger.error("Configuration file error: {}", e.getMessage());
                throw e;
            }
        }
        // 先按正则处理一次
        String input = regexpReplace(inputSql);
        // 创建词法分析器和解析器
        PlSqlLexer lexer = new PlSqlLexer(CharStreams.fromString(input));
        // 创建自定义错误监听器
        com.plsqlrewriter.util.CustomErrorListener lexerErrorListener = new com.plsqlrewriter.util.CustomErrorListener(null);
        lexer.removeErrorListeners();
        lexer.addErrorListener(lexerErrorListener);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlSqlParser parser = new PlSqlParser(tokens);
        com.plsqlrewriter.util.CustomErrorListener parserErrorListener = new com.plsqlrewriter.util.CustomErrorListener(null);
        parser.removeErrorListeners();
        parser.addErrorListener(parserErrorListener);
        ParseTree tree = parser.sql_script();
        boolean hasError = lexerErrorListener.hasError() || parserErrorListener.hasError();
        TokenStreamRewriter rewriter = new TokenStreamRewriter(tokens);
        // 复用原有Visitor逻辑
        PlSqlParserBaseVisitor<Void> visitor = getVisitor(fileName != null ? Paths.get(fileName) : null, sourceEncoding, targetEncoding, rewriter, tokens);
        visitor.visit(tree);
        // Token级别处理
        processTokensForRewrite(tokens, rewriter, fileName != null ? Paths.get(fileName) : null);
        String output = rewriter.getText();
        if (hasError) {
            StringBuilder errorMsg = new StringBuilder("SQL解析错误，转换未完成\n");
            if (lexerErrorListener instanceof com.plsqlrewriter.util.CustomErrorListener) {
                for (String err : ((com.plsqlrewriter.util.CustomErrorListener)lexerErrorListener).getErrorMessages()) {
                    errorMsg.append(err).append("\n");
                }
            }
            if (parserErrorListener instanceof com.plsqlrewriter.util.CustomErrorListener) {
                for (String err : ((com.plsqlrewriter.util.CustomErrorListener)parserErrorListener).getErrorMessages()) {
                    errorMsg.append(err).append("\n");
                }
            }
            logger.warn(errorMsg.toString());
            throw new Exception(errorMsg.toString());
        }
        return output;
    }

    /**
     * 工具方法：优先从外部config目录读取文件，不存在则从jar包读取
     */
    private static InputStream getConfigInputStream(String configFilePath) throws IOException {
        File externalFile = new File(configFilePath);
        if (externalFile.exists()) {
            return new FileInputStream(externalFile);
        } else {
            InputStream in = PlSqlRewriter.class.getClassLoader().getResourceAsStream(configFilePath);
            return in;
        }
    }
}