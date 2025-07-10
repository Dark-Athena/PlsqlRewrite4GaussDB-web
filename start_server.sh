#!/bin/bash

cd "$(dirname "$0")/"

# 启动后端
nohup java -Dloader.path=lib/ -Dloader.main=com.plsqlrewriter.webapp.WebappApplication -jar webapp-1.0.0.jar > logs/backend.log 2>&1 &

echo "SERVER STARTED" 