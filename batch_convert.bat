@echo off
chcp 65001 >nul
setlocal

REM Check if arguments are provided
if "%~2"=="" (
    echo Usage:
    echo   batch_convert.bat ^<input_dir^> ^<output_dir^> [concurrency] [source_encoding] [target_encoding]
    echo Examples:
    echo   batch_convert.bat input_dir output_dir
    echo   batch_convert.bat input_dir output_dir 10
    echo   batch_convert.bat input_dir output_dir 10 UTF-8 GBK
    exit /b 1
)

REM Set default values
set "CONCURRENCY=10"
set "SOURCE_ENCODING=UTF-8"
set "TARGET_ENCODING=UTF-8"

REM Use specified values if provided
if not "%~3"=="" set "CONCURRENCY=%~3"
if not "%~4"=="" set "SOURCE_ENCODING=%~4"
if not "%~5"=="" set "TARGET_ENCODING=%~5"

REM Get script directory
set "SCRIPT_DIR=%~dp0"

REM Set classpath using absolute path
set "CLASSPATH=%SCRIPT_DIR%target\plsql-rewriter-1.0-SNAPSHOT-jar-with-dependencies.jar"

REM Check if JAR exists
if not exist "%CLASSPATH%" (
    echo Error: JAR file not found: %CLASSPATH%
    echo Please run build_offline.bat first
    exit /b 1
)

REM Run Java program
java -cp "%CLASSPATH%" com.plsqlrewriter.util.BatchProcessor "%~1" "%~2" %CONCURRENCY% "%SOURCE_ENCODING%" "%TARGET_ENCODING%" 