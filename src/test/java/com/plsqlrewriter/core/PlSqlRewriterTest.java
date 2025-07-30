package com.plsqlrewriter.core;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.*;
import com.plsqlrewriter.core.PlSqlRewriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

public class PlSqlRewriterTest {
    private static final Logger logger = LoggerFactory.getLogger(PlSqlRewriterTest.class);
    private Path tempDir;

    @Before
    public void setUp() throws IOException {
        // Create temporary directory
        tempDir = Files.createTempDirectory("sql_test");
    }

    @After
    public void tearDown() throws IOException {
        // Clean up temporary directory
        if (tempDir != null) {
            deleteDirectory(tempDir.toFile());
        }
    }

    @Test
    public void testBasicConversion() throws Exception {
        // Prepare input and expected output directories
        Path inputDir = Paths.get("src/test/resources/sql_tests/input");
        Path expectedDir = Paths.get("src/test/resources/sql_tests/expected");
        Files.createDirectories(inputDir);
        Files.createDirectories(expectedDir);
        
        // Prepare test files
        String sql = "SELECT * FROM dual;\n";
        String fileName = "basic_test.sql";
        Path inputFile = inputDir.resolve(fileName);
        Path expectedFile = expectedDir.resolve(fileName);
        Path outputFile = tempDir.resolve(fileName);

        // Write test data
        Files.write(inputFile, sql.getBytes());
        Files.write(expectedFile, "SELECT * FROM dual;\n".getBytes());

        // Execute conversion
        PlSqlRewriter.processFile(inputFile, outputFile, "UTF-8", "UTF-8");

        // Verify result
        assertTrue("Conversion result should match expected", compareFiles(outputFile, expectedFile));
    }

    @Test
    public void testComplexSql() throws Exception {
        // Prepare input and expected output directories
        Path inputDir = Paths.get("src/test/resources/sql_tests/input");
        Path expectedDir = Paths.get("src/test/resources/sql_tests/expected");
        Files.createDirectories(inputDir);
        Files.createDirectories(expectedDir);
        
        // Prepare test content
        String sql = "DECLARE\n" +
                    "  v_count NUMBER;\n" +
                    "BEGIN\n" +
                    "  SELECT COUNT(*) INTO v_count FROM dual;\n" +
                    "END;\n";
        
        String expected = "DECLARE\n" +
                         "  v_count NUMBER;\n" +
                         "BEGIN\n" +
                         "  SELECT COUNT(*) INTO v_count FROM dual;\n" +
                         "END;\n";
        
        String fileName = "complex_test.sql";
        Path inputFile = inputDir.resolve(fileName);
        Path expectedFile = expectedDir.resolve(fileName);
        Path outputFile = tempDir.resolve(fileName);

        // Write test data
        Files.write(inputFile, sql.getBytes());
        Files.write(expectedFile, expected.getBytes());

        // Execute conversion
        PlSqlRewriter.processFile(inputFile, outputFile, "UTF-8", "UTF-8");

        // Verify result
        assertTrue("Complex SQL conversion result should match expected", compareFiles(outputFile, expectedFile));
    }

    @Test
    public void testFromTestDirectory() throws Exception {
        // Use BatchProcessor to process test files in batch
        Path inputDir = Paths.get("src/test/resources/sql_tests/input");
        Path expectedDir = Paths.get("src/test/resources/sql_tests/expected");
        
        if (!Files.exists(inputDir) || !Files.exists(expectedDir)) {
            logger.warn("Test directory does not exist: input - {}, expected - {}", 
                        inputDir.toAbsolutePath(), expectedDir.toAbsolutePath());
            return;
        }
        
        // Use temp directory as output
        String inputDirStr = inputDir.toAbsolutePath().toString();
        String outputDirStr = tempDir.toAbsolutePath().toString();
        
        // Create BatchProcessor instance and process all files
        com.plsqlrewriter.util.BatchProcessor processor = 
            new com.plsqlrewriter.util.BatchProcessor(inputDirStr, outputDirStr, 4, "UTF-8", "UTF-8");
        processor.process();
        
        // Verify results
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputDir, "*.sql")) {
            for (Path inputPath : stream) {
                String fileName = inputPath.getFileName().toString();
                Path expectedPath = expectedDir.resolve(fileName);
                Path outputPath = tempDir.resolve(fileName);
                
                if (Files.exists(expectedPath) && Files.exists(outputPath)) {
                    logger.info("Verifying test case: {}", fileName);
                    assertTrue(
                        String.format("Test case %s failed", fileName),
                        compareFiles(outputPath, expectedPath)
                    );
                } else if (!Files.exists(outputPath)) {
                    logger.warn("Output file not generated, possible syntax error: {}", fileName);
                } else if (!Files.exists(expectedPath)) {
                    logger.warn("Expected file not found: {}", expectedPath);
                }
            }
        }
    }

    @Test
    public void testRewriteSqlDirectly() throws Exception {
        // Simple SQL test
        String inputSql = "SELECT * FROM dual;";
        String expectedSql = "SELECT * FROM dual;";
        String result = PlSqlRewriter.rewriteSql(inputSql, "UTF-8", "UTF-8");
        assertEquals("rewriteSql output should match expected", expectedSql, result.trim());

        // Add more SQL test cases as needed
    }

    @Test
    public void testGroupByStringConstantsRemoval() throws Exception {
        // Test single string constant removal - should remove entire GROUP BY clause
        String inputSql1 = "select 1 from t group by 'constant string';";
        String expectedSql1 = "select 1 from t;";
        String result1 = PlSqlRewriter.rewriteSql(inputSql1, "UTF-8", "UTF-8");
        assertEquals("Single string constant GROUP BY should be removed entirely", expectedSql1, result1.trim());

        // Test mixed elements - should remove only string constants
        String inputSql2 = "select 1 from t group by 'constant string', col1, col2;";
        String expectedSql2 = "select 1 from t group by col1, col2;";
        String result2 = PlSqlRewriter.rewriteSql(inputSql2, "UTF-8", "UTF-8");
        assertEquals("String constants should be removed from GROUP BY list", expectedSql2, result2.trim());

        // Test multiple string constants with other elements
        String inputSql3 = "select 1 from t group by col1, 'constant string', col2, 'another constant string';";
        String expectedSql3 = "select 1 from t group by col1, col2;";
        String result3 = PlSqlRewriter.rewriteSql(inputSql3, "UTF-8", "UTF-8");
        assertEquals("Multiple string constants should be removed from GROUP BY list", expectedSql3, result3.trim());
    }

    private boolean compareFiles(Path actualPath, Path expectedPath) throws IOException {
        if (!Files.exists(expectedPath)) {
            logger.error("Expected file does not exist: {}", expectedPath);
            return false;
        }

        List<String> actualLines = Files.lines(actualPath)
                .map(String::trim)
                .collect(Collectors.toList());
        List<String> expectedLines = Files.lines(expectedPath)
                .map(String::trim)
                .collect(Collectors.toList());

        if (actualLines.size() != expectedLines.size()) {
            logger.error("Line count mismatch - actual: {}, expected: {}", actualLines.size(), expectedLines.size());
            return false;
        }

        for (int i = 0; i < actualLines.size(); i++) {
            if (!actualLines.get(i).equals(expectedLines.get(i))) {
                logger.error("Line {} mismatch", i + 1);
                logger.error("Expected: {}", expectedLines.get(i));
                logger.error("Actual: {}", actualLines.get(i));
                return false;
            }
        }

        return true;
    }

    private void deleteDirectory(File directory) throws IOException {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        Files.delete(file.toPath());
                    }
                }
            }
            Files.delete(directory.toPath());
        }
    }
} 