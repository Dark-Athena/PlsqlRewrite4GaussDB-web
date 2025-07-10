#!/bin/bash
#     usage:
#       ./grun_tree.sh <文件名>
#       ./grun_tree.sh -tokens <文件名>
#       ./grun_tree.sh -gui <文件名>"
#       ./grun_tree.sh -tokens -gui <文件名>
#       "select 1 from dual;" | ./grun_tree.sh

# 检测操作系统类型并设置正确的路径分隔符
case "$(uname -s)" in
    Linux*)     machine=Linux;;
    Darwin*)    machine=Mac;;
    CYGWIN*|MINGW*) machine=Windows;;
    *)          machine="UNKNOWN"
esac

if [ "$machine" = "Windows" ]; then
    CLASSPATH_SEPARATOR=";"
else
    CLASSPATH_SEPARATOR=":"
fi

# 设置类路径
CLASSPATH=".${CLASSPATH_SEPARATOR}target/classes${CLASSPATH_SEPARATOR}target/test-classes"

for jar in target/plsql-rewriter-*-jar-with-dependencies.jar; do
    CLASSPATH="${CLASSPATH}${CLASSPATH_SEPARATOR}${jar}"
done

# 运行TestRig
java -cp "${CLASSPATH}" org.antlr.v4.gui.TestRig com.plsqlrewriter.parser.antlr.generated.PlSql sql_script -tree "$@"