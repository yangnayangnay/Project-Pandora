@echo off
if "%~1"=="" (
    echo Usage: check-port.bat port_number
    exit /b 2
)
netstat -aon | findstr ":%~1 " | findstr "LISTENING" >nul 2>&1
if errorlevel 1 (
    exit /b 0
) else (
    exit /b 1
)