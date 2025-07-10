package com.plsqlrewriter.util;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义错误监听器，用于处理ANTLR解析错误
 */
public class CustomErrorListener extends BaseErrorListener {
    private static final Logger logger = LoggerFactory.getLogger(CustomErrorListener.class);
    private final Path inputFile;
    private final List<String> errorMessages = new ArrayList<>();

    public CustomErrorListener(Path inputFile) {
        this.inputFile = inputFile;
    }

    @Override
    public void syntaxError(
            Recognizer<?, ?> recognizer,
            Object offendingSymbol,
            int line,
            int charPositionInLine,
            String msg,
            RecognitionException e) {
        
        String fileName = (inputFile != null) ? inputFile.toString() : "unknown";
        //  内容截取100字符
        String shortMsg = msg.length() > 100 ? msg.substring(0, 100) + "..." : msg;
        String errorMsg = String.format("文件: %s, 行: %d, 列: %d, 错误: %s", fileName, line, charPositionInLine, shortMsg);
        errorMessages.add(errorMsg);
        logger.error("Syntax error - File: {}, Line: {}, Position: {}, Error: {}", fileName, line, charPositionInLine, shortMsg);
    }

    /**
     * 判断是否检测到错误
     */
    public boolean hasError() {
        return !errorMessages.isEmpty();
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }
} 