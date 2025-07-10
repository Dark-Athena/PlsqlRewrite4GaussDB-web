@echo off
setlocal enabledelayedexpansion

REM 1. 构建核心模块
call mvn clean install -DskipTests
if errorlevel 1 exit /b 1

REM 2. 构建前端
cd frontend
call npm install
if errorlevel 1 exit /b 1
call npm run build
if errorlevel 1 exit /b 1
cd ..

REM 3. 拷贝前端静态文件到后端 resources/static
rmdir /s /q webapp\src\main\resources\static
mkdir webapp\src\main\resources\static
xcopy /E /I /Y frontend\dist\* webapp\src\main\resources\static\

REM 4. 构建后端（包含前端静态资源）
cd webapp
call mvn clean package -DskipTests
if errorlevel 1 exit /b 1
cd ..

REM 5. 组装 release 目录
rmdir /s /q release
mkdir release
mkdir release\config
mkdir release\data
mkdir release\lib
mkdir release\logs
mkdir release\target
copy webapp\target\webapp-1.0.0.jar release\
copy target\plsql-rewriter-1.0-SNAPSHOT.jar release\lib\
copy target\plsql-rewriter-1.0-SNAPSHOT-jar-with-dependencies.jar release\target\
xcopy /E /I /Y config\* release\config\
copy start_server.sh release\
copy start_server.bat release\
copy convert.bat release\
copy convert.sh release\
copy batch_convert.bat release\
copy batch_convert.sh release\
copy grun_tree.bat release\
copy grun_tree.sh release\
copy readme.md release\
copy readme_core.md release\
copy LICENSE release\

REM 6. 完成
echo package finished
pause 