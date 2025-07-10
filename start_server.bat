@echo off
cd /d %~dp0

java -Dloader.path=lib\ -Dloader.main=com.plsqlrewriter.webapp.WebappApplication -jar webapp-1.0.0.jar > logs\backend.log 2>&1
echo SERVER EXIT
pause 