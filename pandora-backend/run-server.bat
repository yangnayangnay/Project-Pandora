@echo off
chcp 65001 >nul
title Project: Pandora Backend Server

echo ========================================
echo   Project: Pandora Backend Server
echo   Smart Work System v1.0
echo ========================================
echo.

echo [1/4] 检查 Java 环境...
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未找到 Java，请确保已安装 JDK 17+ 并配置 JAVA_HOME
    pause
    exit /b 1
)
echo [OK] Java 环境正常
echo.

echo [2/4] 检查 MySQL 连接...
echo 请确保 MySQL 已启动且数据库 pandora 已创建
echo.

echo [3/4] 检查 Redis 连接...
echo 请确保 Redis 已启动
echo.

echo [4/4] 启动后端服务...
echo.
echo 服务地址: http://localhost:8080/api
echo 按 Ctrl+C 停止服务
echo.

java -jar target/pandora-backend-1.0.0.jar --spring.profiles.active=prod

pause