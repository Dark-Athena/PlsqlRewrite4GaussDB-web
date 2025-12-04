@echo off

REM Clean and create output directories
powershell Write-Host "[INFO] Cleaning and creating directories..."
if exist target rmdir /S /Q target >nul 2>&1
mkdir target\classes >nul 2>&1
mkdir target\generated-sources\antlr4\com\plsqlrewriter\parser\antlr\generated >nul 2>&1

REM Generate ANTLR Parser files
powershell Write-Host "[INFO] Generating ANTLR parser files..."
call regen_parser.bat
if errorlevel 1 (
    powershell Write-Host "[ERROR] Failed to generate ANTLR parser files"
    exit /b 1
)

REM Set environment variables
set CLASSPATH=.;^
lib\antlr-4.13.2-complete.jar;^
lib\snakeyaml-2.3.jar;^
lib\slf4j-api-2.0.15.jar;^
lib\logback-core-1.5.19.jar
lib\logback-classic-1.5.19.jar;^


REM Compile Java source files
powershell Write-Host "[INFO] Compiling Java source files..."
javac -encoding UTF-8 -d target\classes ^
    -cp %CLASSPATH% ^
    src\main\java\com\plsqlrewriter\parser\antlr\*.java ^
    target\generated-sources\antlr4\com\plsqlrewriter\parser\antlr\generated\*.java ^
    src\main\java\com\plsqlrewriter\core\*.java ^
    src\main\java\com\plsqlrewriter\util\*.java
if errorlevel 1 (
    powershell Write-Host "[ERROR] Failed to compile Java source files"
    exit /b 1
)

REM Copy configuration files and resources
powershell Write-Host "[INFO] Copying configuration files..."
xcopy /Y /E /I config target\classes\config >nul 2>&1
if exist src\main\resources xcopy /Y /E /I src\main\resources target\classes >nul 2>&1

REM Create JAR file
powershell Write-Host "[INFO] Creating JAR file..."
jar cf target\plsql-rewriter-1.0-SNAPSHOT.jar -C target\classes . >nul 2>&1

REM Create temp directory for JAR with dependencies
powershell Write-Host "[INFO] Creating JAR file with dependencies..."
if exist target\temp rmdir /S /Q target\temp
mkdir target\temp >nul 2>&1
xcopy /Y /E /I target\classes\* target\temp\ >nul 2>&1

REM Extract dependency JARs
for %%f in (lib\*.jar) do (
    pushd target\temp
    jar xf ..\..\%%f
    popd
)

REM Remove META-INF files from dependencies
if exist target\temp\META-INF\MANIFEST.MF del target\temp\META-INF\MANIFEST.MF
if exist target\temp\META-INF\*.SF del target\temp\META-INF\*.SF
if exist target\temp\META-INF\*.DSA del target\temp\META-INF\*.DSA
if exist target\temp\META-INF\*.RSA del target\temp\META-INF\*.RSA

REM Create final JAR with all dependencies
jar cfm target\plsql-rewriter-1.0-SNAPSHOT-jar-with-dependencies.jar META-INF\MANIFEST.MF -C target\temp . >nul 2>&1

REM Clean up temp directory
rmdir /S /Q target\temp

powershell Write-Host "[INFO] Build completed!"
powershell Write-Host "[INFO] JAR file location: target\plsql-rewriter-1.0-SNAPSHOT-jar-with-dependencies.jar" 