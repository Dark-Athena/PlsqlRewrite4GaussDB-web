@echo off
REM Set absolute paths
set "SCRIPT_DIR=%~dp0"
set "ANTLR_JAR=%SCRIPT_DIR%lib\antlr-4.13.2-complete.jar"
set "CLASSPATH=%ANTLR_JAR%"

REM Create output directory
set "OUTPUT_DIR=%SCRIPT_DIR%target\generated-sources\antlr4\com\plsqlrewriter\parser\antlr\generated"
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

REM Change to target directory
pushd "%OUTPUT_DIR%"

REM Copy grammar files to current directory
copy "%SCRIPT_DIR%src\main\resources\antlr4\PlSqlLexer.g4" . >nul
copy "%SCRIPT_DIR%src\main\resources\antlr4\PlSqlParser.g4" . >nul

REM Generate Lexer
java -cp "%CLASSPATH%" org.antlr.v4.Tool -Dlanguage=Java ^
    -package com.plsqlrewriter.parser.antlr.generated ^
    -lib "%SCRIPT_DIR%src\main\java\com\plsqlrewriter\parser\antlr" ^
    -listener -visitor ^
    PlSqlLexer.g4

REM Generate Parser
java -cp "%CLASSPATH%" org.antlr.v4.Tool -Dlanguage=Java ^
    -package com.plsqlrewriter.parser.antlr.generated ^
    -lib "%SCRIPT_DIR%src\main\java\com\plsqlrewriter\parser\antlr" ^
    -listener -visitor ^
    PlSqlParser.g4

REM Clean up grammar files
del PlSqlLexer.g4 >nul 2>&1
del PlSqlParser.g4 >nul 2>&1

REM Return to original directory
popd 
