@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
title Project: Pandora - 全部启动

rem ==================== 配置变量 ====================
set "JAR_PATH=D:\AndroidStudio\Project_Pandora\pandora-backend\target\pandora-backend-1.0.0.jar"
set "HEALTH_URL=http://localhost:8080/api/actuator/health"
set "WEB_URL=http://localhost:3000"
set "POLL_INTERVAL=2"

echo ========================================
echo   Project: Pandora 智能掌上工作系统
echo   一键启动后端 + Web管理端
echo ========================================
echo.

rem ==================== 端口冲突检测 ====================
powershell -NoProfile -Command "if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) { exit 1 } else { exit 0 }"
if errorlevel 1 (
    echo [ERROR] 端口 8080 已被占用
    pause
    exit /b 1
)
powershell -NoProfile -Command "if (Get-NetTCPConnection -LocalPort 3000 -State Listen -ErrorAction SilentlyContinue) { exit 1 } else { exit 0 }"
if errorlevel 1 (
    echo [ERROR] 端口 3000 已被占用
    pause
    exit /b 1
)

rem ==================== 记录启动时间 ====================
set "T0=%time%"

rem ==================== JAR存在性判定与启动 ====================
echo [1/4] 启动后端服务 (端口8080)...
if exist "%JAR_PATH%" (
    echo [INFO] 启动方式：预编译启动
    set "STARTUP_MODE=PRECOMPILED"
    start "Pandora Backend" "C:\Program Files\Java\jdk-19\bin\java.exe" -jar "%JAR_PATH%"
) else (
    echo [INFO] 启动方式：实时编译启动
    set "STARTUP_MODE=REALTIME"
    start "Pandora Backend" cmd /c "set JAVA_HOME=C:\Program Files\Java\jdk-19&& D:\maven\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run -q -f D:\AndroidStudio\Project_Pandora\pandora-backend\pom.xml"
)
echo [OK] 后端正在启动...
echo.

rem ==================== 立即打开加载中页面 ====================
start "" "%~dp0loading.html"
echo [OK] 浏览器已打开加载中页面
echo.

rem ==================== 立即启动Web端 ====================
echo [2/4] 启动Web管理端 (端口3000)...
start "Pandora Web" /D "D:\AndroidStudio\Project_Pandora\pandora-web" cmd /c "npm run dev"
echo [OK] Web管理端正在启动...
echo.

rem ==================== 统一轮询循环（无超时） ====================
echo [3/4] 等待服务就绪...
set "BACKEND_READY=0"
set "WEB_READY=0"
set "WAIT_COUNT=0"

:poll_loop
timeout /t %POLL_INTERVAL% /nobreak >nul
set /a WAIT_COUNT+=1

rem 检测后端
if "!BACKEND_READY!"=="0" (
    curl -s "%HEALTH_URL%" >nul 2>&1
    if !errorlevel! equ 0 (
        set "BACKEND_READY=1"
        echo [OK] 后端已就绪
    )
)

rem 检测Web端
if "!WEB_READY!"=="0" (
    curl -s "%WEB_URL%" >nul 2>&1
    if !errorlevel! equ 0 (
        set "WEB_READY=1"
        echo [OK] Web端已就绪
    )
)

rem 检查退出条件
if "!BACKEND_READY!"=="1" (
    if "!WEB_READY!"=="1" goto all_ready
)

goto poll_loop

:all_ready
echo.
echo [4/4] 系统启动完成
echo ========================================
echo   系统已启动！
if "!STARTUP_MODE!"=="PRECOMPILED" (
    echo   启动方式: 预编译启动
) else (
    echo   启动方式: 实时编译启动
)
echo   后端: http://localhost:8080/api
echo   Web端: http://localhost:3000
echo   管理员账号: admin / admin123
echo   H2控制台: http://localhost:8080/api/h2-console
echo   浏览器将自动跳转至登录页
echo ========================================
echo.
echo 按任意键关闭此窗口（后端和Web端会继续运行）
pause >nul
goto :eof