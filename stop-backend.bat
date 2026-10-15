@echo off
echo Stopping Pandora Backend...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8080" ^| findstr "LISTENING"') do (
    echo Killing PID: %%a
    taskkill /F /PID %%a
)
echo Done.
pause