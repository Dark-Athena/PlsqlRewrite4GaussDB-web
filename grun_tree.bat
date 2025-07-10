@echo off
setlocal enabledelayedexpansion

set CLASSPATH=.;target\classes;target\test-classes
for %%i in (target\plsql-rewriter-*-jar-with-dependencies.jar) do set CLASSPATH=!CLASSPATH!;%%i

java org.antlr.v4.gui.TestRig com.plsqlrewriter.parser.antlr.generated.PlSql sql_script -tree %*