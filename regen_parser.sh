#!/bin/bash

# 设置绝对路径
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANTLR_JAR="${SCRIPT_DIR}/lib/antlr-4.13.2-complete.jar"
CLASSPATH="${ANTLR_JAR}"

# 创建输出目录
OUTPUT_DIR="${SCRIPT_DIR}/target/generated-sources/antlr4/com/plsqlrewriter/parser/antlr/generated"
mkdir -p "${OUTPUT_DIR}"

# 切换到目标目录
pushd "${OUTPUT_DIR}" > /dev/null

# 复制语法文件到当前目录
cp "${SCRIPT_DIR}/src/main/resources/antlr4/PlSqlLexer.g4" .
cp "${SCRIPT_DIR}/src/main/resources/antlr4/PlSqlParser.g4" .

# 生成词法分析器
java -cp "${CLASSPATH}" org.antlr.v4.Tool -Dlanguage=Java \
    -package com.plsqlrewriter.parser.antlr.generated \
    -lib "${SCRIPT_DIR}/src/main/java/com/plsqlrewriter/parser/antlr" \
    -listener -visitor \
    PlSqlLexer.g4

# 生成语法分析器
java -cp "${CLASSPATH}" org.antlr.v4.Tool -Dlanguage=Java \
    -package com.plsqlrewriter.parser.antlr.generated \
    -lib "${SCRIPT_DIR}/src/main/java/com/plsqlrewriter/parser/antlr" \
    -listener -visitor \
    PlSqlParser.g4

# 清理语法文件
rm -f PlSqlLexer.g4
rm -f PlSqlParser.g4

# 返回原目录
popd > /dev/null

echo "Parser regeneration completed."
