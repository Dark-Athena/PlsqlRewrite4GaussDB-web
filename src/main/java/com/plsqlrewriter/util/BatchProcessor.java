package com.plsqlrewriter.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.TreeSet;

import com.plsqlrewriter.core.PlSqlRewriter;

public class BatchProcessor {
    private static final Logger logger = LoggerFactory.getLogger(BatchProcessor.class);

    private final String inputDir;
    private final String outputDir;
    private final int concurrency;
    private final String sourceEncoding;
    private final String targetEncoding;
    private final ExecutorService executor;
    private final List<Future<?>> futures = new ArrayList<>();
    private final ConcurrentHashMap<String, String> errorFiles = new ConcurrentHashMap<>();
    private final Set<String> successFiles = ConcurrentHashMap.newKeySet();

    public BatchProcessor(String inputDir, String outputDir, int concurrency, 
                        String sourceEncoding, String targetEncoding) {
        this.inputDir = inputDir;
        this.outputDir = outputDir;
        this.concurrency = concurrency;
        this.sourceEncoding = sourceEncoding;
        this.targetEncoding = targetEncoding;
        this.executor = Executors.newFixedThreadPool(concurrency);
    }

    public void process() {
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        
        File inputDirectory = new File(inputDir);
        File outputDirectory = new File(outputDir);
        
        if (!inputDirectory.exists() || !inputDirectory.isDirectory()) {
            logger.error("Input directory does not exist or is not a directory: {}", inputDir);
            return;
        }
        
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            logger.error("Cannot create output directory: {}", outputDir);
            return;
        }

        logger.info("======================== Start Preloading Configuration ========================");
        // 在处理任何文件前，预加载所有配置
        try {
            com.plsqlrewriter.core.PlSqlRewriter.preloadAllConfigs();
            logger.info("Configuration preloaded successfully, concurrent processing will share the same configuration data");
        } catch (Exception e) {
            logger.error("Configuration preloading failed: {}", e.getMessage());
            logger.error("Terminating process");
            return;
        }
        logger.info("======================== Preloading Completed ========================");

        logger.info("======================== Start Conversion ========================");

        try {
            // Recursively process directory
            processDirectory(inputDirectory, outputDirectory);

            // Wait for all tasks to complete
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("Error occurred while processing file", e);
                }
            }
            
            // 计算总耗时
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            // 格式化为时分秒
            String formattedTime = String.format("%d hours %d minutes %d seconds", 
                    totalTime / (1000 * 60 * 60), 
                    (totalTime % (1000 * 60 * 60)) / (1000 * 60), 
                    (totalTime % (1000 * 60)) / 1000);
            
            // 处理完所有文件后，生成汇总报告
            
            // 1. 打印成功处理的文件列表
            logger.info("======================== Successfully Converted Files Summary ========================");
            logger.info(" {} files converted successfully:", successFiles.size());
            // 将成功文件排序后输出
            List<String> sortedSuccessFiles = new ArrayList<>(successFiles);
            java.util.Collections.sort(sortedSuccessFiles);
            int count = 1;
            for (String file : sortedSuccessFiles) {
                logger.info("{}. {}", count++, file);
            }
            logger.info("=================================================================");
            
            // 2. 如果有解析错误，打印错误文件汇总信息
            if (!errorFiles.isEmpty()) {
                logger.error("======================== Failed Conversion Files Summary ========================");
                logger.error(" {} files failed to convert:", errorFiles.size());
                count = 1;
                for (String file : errorFiles.keySet()) {
                    logger.error("{}. File: {}, Error: {}", count++, file, errorFiles.get(file));
                }
                logger.error("=================================================================");
            }
            
            // 3. 输出总计耗时
            logger.info("======================== Total Time ========================");
            logger.info("Total processed files: {}", successFiles.size() + errorFiles.size());
            logger.info("Total time: {}", formattedTime);
            logger.info("=================================================================");
            
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                    logger.warn("Some tasks did not complete within 1 hour");
                }
            } catch (InterruptedException e) {
                logger.error("Waiting for tasks to complete interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    private void processDirectory(File inputDir, File outputDir) {
        File[] files = inputDir.listFiles();
        if (files == null) {
            logger.error("Cannot access directory: {}", inputDir);
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // Create corresponding output directory for subdirectory
                File newOutputDir = new File(outputDir, file.getName());
                if (!newOutputDir.exists() && !newOutputDir.mkdirs()) {
                    logger.error("Cannot create output subdirectory: {}", newOutputDir);
                    continue;
                }
                // Recursively process subdirectory
                processDirectory(file, newOutputDir);
            } else {
                // Process file
                File outputFile = new File(outputDir, file.getName());
                futures.add(executor.submit(() -> processFile(file, outputFile)));
            }
        }
    }

    private void processFile(File inputFile, File outputFile) {
        try {
            if (!inputFile.exists() || !inputFile.isFile()) {
                logger.error("Input file does not exist or is not a regular file: {}", inputFile.getAbsolutePath());
                return;
            }

            // Get relative path for log display
            String relativePath = new File(inputDir).toURI().relativize(inputFile.toURI()).getPath();
            logger.info("Start processing file: {}", relativePath);
            // 设置MDC上下文
            MDC.put("file", relativePath);
            try {
                // Convert to Path objects
                Path inputPath = inputFile.toPath();
                Path outputPath = outputFile.toPath();
                // Call PlSqlRewriter's process method
                PlSqlRewriter.processFile(inputPath, outputPath, sourceEncoding, targetEncoding);
                logger.info("File processing completed: {}", relativePath);
                // 记录成功处理的文件
                successFiles.add(relativePath);
            } catch (Exception e) {
                // 捕获处理过程中的异常，但不重新抛出，确保流程继续
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.length() > 150) {
                    errorMsg = errorMsg.substring(0, 150) + "...";
                }
                logger.error("process file {} error: {}", relativePath, errorMsg);
                // 记录到错误文件集合中
                errorFiles.put(relativePath, errorMsg);
            } finally {
                MDC.remove("file");
            }
        } catch (Exception e) {
            // 这里捕获的是外层方法的异常（如文件不存在等）
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 150) {
                errorMsg = errorMsg.substring(0, 150) + "...";
            }
            logger.error("Error occurred while processing file {}: {}", inputFile.getName(), errorMsg);
            
            // 获取相对路径用于记录错误
            String relativePath;
            try {
                relativePath = new File(inputDir).toURI().relativize(inputFile.toURI()).getPath();
            } catch (Exception ex) {
                relativePath = inputFile.getAbsolutePath();
            }
            
            errorFiles.put(relativePath, errorMsg);
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java BatchProcessor <input directory> <output directory> [concurrency] [source encoding] [target encoding]");
            return;
        }

        // Normalize paths
        String inputDir = new File(args[0]).getAbsolutePath();
        String outputDir = new File(args[1]).getAbsolutePath();
        
        int concurrency = args.length > 2 ? Integer.parseInt(args[2]) : 10;
        String sourceEncoding = args.length > 3 ? args[3] : "UTF-8";
        String targetEncoding = args.length > 4 ? args[4] : sourceEncoding;

        BatchProcessor processor = new BatchProcessor(inputDir, outputDir, concurrency, 
                                                    sourceEncoding, targetEncoding);
        processor.process();
    }
} 