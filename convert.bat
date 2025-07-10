@echo off
REM Set code page to UTF-8
chcp 65001 >nul

REM Get script directory
set "SCRIPT_DIR=%~dp0"

REM Check if arguments are provided
if "%~2"=="" (
    echo Usage:
    echo   convert.bat ^<input_file^> ^<output_file^> [source_encoding] [target_encoding]
    echo Examples:
    echo   convert.bat input.sql output.sql
    echo   convert.bat input.sql output.sql UTF-8 GBK
    exit /b 1
)

REM Set default encodings
set "SOURCE_ENCODING=UTF-8"
set "TARGET_ENCODING=UTF-8"

REM Use specified encodings if provided
if not "%~3"=="" set "SOURCE_ENCODING=%~3"
if not "%~4"=="" set "TARGET_ENCODING=%~4"

REM Set classpath using absolute path
set "CLASSPATH=%SCRIPT_DIR%target\plsql-rewriter-1.0-SNAPSHOT-jar-with-dependencies.jar"

REM Check if JAR exists
if not exist "%CLASSPATH%" (
    echo Error: JAR file not found: %CLASSPATH%
    echo Please run build_offline.bat first
    exit /b 1
)

REM Run Java program
java -cp "%CLASSPATH%" com.plsqlrewriter.core.PlSqlRewriter "%~1" "%~2" "%SOURCE_ENCODING%" "%TARGET_ENCODING%" 