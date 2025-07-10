#!/bin/bash

# Detect OS type and set path separator
case "$(uname -s)" in
    CYGWIN*|MINGW*|MSYS*)
        PATHSEP=";"
        ;;
    *)
        PATHSEP=":"
        ;;
esac

# Set environment variables
CLASSPATH=".${PATHSEP}"\
"lib/antlr-4.13.2-complete.jar${PATHSEP}"\
"lib/snakeyaml-2.3.jar${PATHSEP}"\
"lib/slf4j-api-2.0.7.jar${PATHSEP}"\
"lib/logback-core-1.4.14.jar${PATHSEP}"\
"lib/logback-classic-1.4.14.jar"

# Clean and create output directories
echo "[INFO] Cleaning and creating directories..."
rm -rf target
mkdir -p target/classes
mkdir -p target/generated-sources/antlr4/com/plsqlrewriter/parser/antlr/generated

# Generate ANTLR Parser files
echo "[INFO] Generating ANTLR parser files..."
./regen_parser.sh
if [ $? -ne 0 ]; then
    echo "[ERROR] Failed to generate ANTLR parser files"
    exit 1
fi

# Compile Java source files
echo "[INFO] Compiling Java source files..."
javac -encoding UTF-8 -d target/classes \
    -cp "$CLASSPATH" \
    src/main/java/com/plsqlrewriter/parser/antlr/*.java \
    target/generated-sources/antlr4/com/plsqlrewriter/parser/antlr/generated/*.java \
    src/main/java/com/plsqlrewriter/core/*.java \
    src/main/java/com/plsqlrewriter/util/*.java
if [ $? -ne 0 ]; then
    echo "[ERROR] Failed to compile Java source files"
    exit 1
fi

# Copy configuration files and resources
echo "[INFO] Copying configuration files..."
cp -r config target/classes/
if [ -d src/main/resources ]; then
    cp -r src/main/resources/* target/classes/
fi

# Create JAR file
echo "[INFO] Creating JAR file..."
jar cf target/plsql-rewriter-1.0-SNAPSHOT.jar -C target/classes .

# Create temp directory for JAR with dependencies
echo "[INFO] Creating JAR file with dependencies..."
rm -rf target/temp
mkdir -p target/temp
cp -r target/classes/* target/temp/

# Extract dependency JARs
cd target/temp
for jar in ../../lib/*.jar; do
    jar xf "$jar"
done
cd ../..

# Remove META-INF files from dependencies
rm -f target/temp/META-INF/MANIFEST.MF
rm -f target/temp/META-INF/*.SF
rm -f target/temp/META-INF/*.DSA
rm -f target/temp/META-INF/*.RSA

# Create final JAR with all dependencies
jar cfm target/plsql-rewriter-1.0-SNAPSHOT-jar-with-dependencies.jar META-INF/MANIFEST.MF -C target/temp .

# Clean up temp directory
rm -rf target/temp

echo "[INFO] Build completed!"
echo "[INFO] JAR file location: target/plsql-rewriter-1.0-SNAPSHOT-jar-with-dependencies.jar" 