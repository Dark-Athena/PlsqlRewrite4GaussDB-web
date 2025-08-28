# 第一阶段：下载和解压文件
FROM busybox:latest AS downloader

# 设置工作目录
WORKDIR /download

# 下载并解压应用程序
RUN wget https://gitee.com/darkathena/PlsqlRewrite4GaussDB-web/releases/download/1.0.1-beta/PlsqlRewrite4GaussDB-web-v1.0.0-beta.zip \
    && unzip PlsqlRewrite4GaussDB-web-v1.0.1-beta.zip \
    && rm PlsqlRewrite4GaussDB-web-v1.0.1-beta.zip

# 第二阶段：运行环境
FROM eclipse-temurin:17-jre-focal

# 设置工作目录
WORKDIR /app

# 从第一阶段复制解压后的文件
COPY --from=downloader /download/ /app/

# 暴露端口
EXPOSE 8080

# 启动命令

CMD ["java", "-Dloader.path=lib/", "-Dloader.main=com.plsqlrewriter.webapp.WebappApplication", "-jar", "webapp-1.0.0.jar"]
