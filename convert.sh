#!/bin/bash

# 检测操作系统类型并设置正确的路径分隔符
case "$(uname -s)" in
    Linux*)     machine=Linux;;
    Darwin*)    machine=Mac;;
    CYGWIN*|MINGW*) machine=Windows;;
    *)          machine="UNKNOWN"
esac

if [ "$machine" = "Windows" ]; then
    JAR_PATH="target\\plsql-rewriter-1.0-SNAPSHOT-jar-with-dependencies.jar"
else
    JAR_PATH="target/plsql-rewriter-1.0-SNAPSHOT-jar-with-dependencies.jar"
fi

# 如果参数不足，显示用法说明
if [ $# -lt 2 ]; then
    echo "用法:"
    echo "  ./convert.sh <输入文件> <输出文件> [源编码] [目标编码]"
    echo "示例:"
    echo "  ./convert.sh input.sql output.sql"
    echo "  ./convert.sh input.sql output.sql UTF-8 GBK"
    exit 1
fi

# 设置默认编码
SOURCE_ENCODING=${3:-UTF-8}
TARGET_ENCODING=${4:-$SOURCE_ENCODING}

# 设置类路径
export CLASSPATH="$JAR_PATH"

# 运行 Java 程序
java com.plsqlrewriter.core.PlSqlRewriter "$1" "$2" "$SOURCE_ENCODING" "$TARGET_ENCODING" 