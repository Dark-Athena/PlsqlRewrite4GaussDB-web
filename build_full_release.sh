#!/bin/bash

# 设置错误时退出
set -e

echo "开始构建完整发布包..."

# 1. 构建核心模块
echo "1. 构建核心模块..."
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "核心模块构建失败"
    exit 1
fi

# 2. 构建前端
echo "2. 构建前端..."
cd frontend
npm install
if [ $? -ne 0 ]; then
    echo "前端依赖安装失败"
    exit 1
fi
npm run build
if [ $? -ne 0 ]; then
    echo "前端构建失败"
    exit 1
fi
cd ..

# 3. 拷贝前端静态文件到后端 resources/static
echo "3. 拷贝前端静态文件..."
rm -rf webapp/src/main/resources/static
mkdir -p webapp/src/main/resources/static
cp -r frontend/dist/* webapp/src/main/resources/static/

# 4. 构建后端（包含前端静态资源）
echo "4. 构建后端..."
cd webapp
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "后端构建失败"
    exit 1
fi
cd ..

# 5. 组装 release 目录
echo "5. 组装 release 目录..."
rm -rf release
mkdir -p release
mkdir -p release/config
mkdir -p release/data
mkdir -p release/lib
mkdir -p release/logs
mkdir -p release/target

cp webapp/target/webapp-1.0.0.jar release/
cp target/plsql-rewriter-1.0-SNAPSHOT.jar release/lib/
cp target/plsql-rewriter-1.0-SNAPSHOT-jar-with-dependencies.jar release/target/
cp -r config/* release/config/
cp start_server.sh release/
cp start_server.bat release/
cp convert.bat release/
cp convert.sh release/
cp batch_convert.bat release/
cp batch_convert.sh release/
cp grun_tree.bat release/
cp grun_tree.sh release/
cp readme.md release/
cp readme_core.md release/
cp LICENSE release/

# 6. 设置执行权限
echo "6. 设置执行权限..."
chmod +x release/*.sh

# 7. 完成
echo "构建完成！"
echo "发布包已生成在 release 目录中" 