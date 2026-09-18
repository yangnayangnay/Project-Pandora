@echo off
chcp 65001 >nul
title Project: Pandora - 全部启动

echo ========================================
echo   Project: Pandora 智能掌上工作系统
echo   一键启动后端 + Web管理端
echo ========================================
echo.

echo [1/4] 启动后端服务 (端口8080)...
start "Pandora Backend" cmd /c "set JAVA_HOME=C:\Program Files\Java\jdk-19&& D:\maven\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run -q -f D:\AndroidStudio\Project_Pandora\pandora-backend\pom.xml"
echo [OK] 后端正在启动...
echo.

echo [2/4] 等待后端就绪...
:wait_backend
timeout /t 2 /nobreak >nul
curl -s http://localhost:8080/api/actuator/health >nul 2>&1
if errorlevel 1 goto wait_backend
echo [OK] 后端已就绪
echo.

echo [3/4] 启动Web管理端 (端口3000)...
start "Pandora Web" cmd /c "cd /d D:\AndroidStudio\Project_Pandora\pandora-web && npm run dev"
echo [OK] Web管理端正在启动...
echo.

echo [4/4] 打开浏览器...
timeout /t 3 /nobreak >nul
start http://localhost:3000
echo [OK] 浏览器已打开
echo.

echo ========================================
echo   系统已启动！
echo   后端: http://localhost:8080/api
echo   Web端: http://localhost:3000
echo   管理员账号: admin / admin123
echo   H2控制台: http://localhost:8080/api/h2-console
echo ========================================
echo.
echo 按任意键关闭此窗口（后端和Web端会继续运行）
pause >nul