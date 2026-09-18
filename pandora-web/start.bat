@echo off
chcp 65001 >nul
title Project: Pandora Web Management

echo ========================================
echo   Project: Pandora Web Management
echo   Smart Work System v1.0
echo ========================================
echo.

echo [1/3] 检查 Node.js 环境...
node -v >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未找到 Node.js，请先安装 Node.js 18+
    pause
    exit /b 1
)
echo [OK] Node.js 环境正常
echo.

echo [2/3] 检查依赖安装...
if not exist node_modules (
    echo 首次运行，正在安装依赖...
    call npm install
    if errorlevel 1 (
        echo [ERROR] 依赖安装失败
        pause
        exit /b 1
    )
    echo [OK] 依赖安装完成
) else (
    echo [OK] 依赖已安装
)
echo.

echo [3/3] 启动 Web 管理端...
echo.
echo 访问地址: http://localhost:3000
echo 浏览器将自动打开，按 Ctrl+C 停止服务
echo.

start "" cmd /c "timeout /t 3 /nobreak >nul & start http://localhost:3000"
call npm run dev

pause