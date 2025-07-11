@echo off
setlocal enabledelayedexpansion


REM 0. clean previous build
if exist "target" rmdir /s /q target
if exist "webapp\target" rmdir /s /q webapp\target
if exist "webapp\src\main\resources\static" rmdir /s /q webapp\src\main\resources\static
if exist "frontend\dist" rmdir /s /q frontend\dist
if exist "release" rmdir /s /q release

REM 1. build core module
call mvn clean install -DskipTests
if errorlevel 1 exit /b 1

REM 2. build frontend
cd frontend
call npm install
if errorlevel 1 exit /b 1
call npm run build
if errorlevel 1 exit /b 1
cd ..

REM 3. copy frontend static files to backend resources/static
mkdir webapp\src\main\resources\static
xcopy /E /I /Y frontend\dist\* webapp\src\main\resources\static\

REM 4. build backend (include frontend static resources)
cd webapp
call mvn clean package -DskipTests
if errorlevel 1 exit /b 1
cd ..

REM 5. assemble release directory
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

REM 6. done
echo package finished
pause 