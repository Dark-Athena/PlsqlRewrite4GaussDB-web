#!/bin/bash

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Check if arguments are provided
if [ $# -lt 2 ]; then
    echo "Usage:"
    echo "  ./batch_convert.sh <input_dir> <output_dir> [concurrency] [source_encoding] [target_encoding]"
    echo "Examples:"
    echo "  ./batch_convert.sh input_dir output_dir"
    echo "  ./batch_convert.sh input_dir output_dir 10"
    echo "  ./batch_convert.sh input_dir output_dir 10 UTF-8 GBK"
    exit 1
fi

# Set default values
CONCURRENCY=${3:-10}
SOURCE_ENCODING=${4:-UTF-8}
TARGET_ENCODING=${5:-UTF-8}

# Set classpath using absolute path
CLASSPATH="${SCRIPT_DIR}/target/plsql-rewriter-1.0-SNAPSHOT-jar-with-dependencies.jar"

# Check if JAR exists
if [ ! -f "$CLASSPATH" ]; then
    echo "Error: JAR file not found: $CLASSPATH"
    echo "Please run build_offline.sh first"
    exit 1
fi

# Run Java program
java -cp "$CLASSPATH" com.plsqlrewriter.util.BatchProcessor "$1" "$2" "$CONCURRENCY" "$SOURCE_ENCODING" "$TARGET_ENCODING" 