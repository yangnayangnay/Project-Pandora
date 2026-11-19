@echo off
setlocal EnableExtensions
title Project Pandora Test Runner

rem ================= locate Maven =================
set "MVN="
for /d %%I in ("D:\desktop\*") do for /d %%J in ("%%~I\Tools\apache-maven-*") do if exist "%%~J\bin\mvn.cmd" set "MVN=%%~J\bin\mvn.cmd"
if not defined MVN ( where mvn >nul 2>nul && set "MVN=mvn" )
if not defined MVN (
    echo [ERROR] mvn not found. Probed: D:\desktop\*\Tools\apache-maven-*\bin\mvn.cmd and PATH
    pause
    exit /b 1
)
echo [OK] Maven = %MVN%

set "ROOT=D:\AndroidStudio\Project_Pandora"
cd /d "%ROOT%" || (echo [ERROR] project dir missing: %ROOT% & pause & exit /b 1)
if not exist gradlew.bat (echo [ERROR] gradlew.bat not found in %ROOT% & pause & exit /b 1)
if not exist "pandora-backend\src\test\resources\db\h2_schema.sql" echo [WARN] h2_schema.sql missing - DB tests may fail

rem ==== [CHANGED 1/2] 清掉旧的覆盖率数据,保证报告只反映本次运行 ====
if exist "pandora-backend\target\jacoco.exec" del /q "pandora-backend\target\jacoco.exec"
if exist "pandora-backend\target\site\jacoco" rmdir /s /q "pandora-backend\target\site\jacoco"

set "START=%date% %time%"
set /a PASSED=0, FAILED=0

echo ========================================================
echo [1/3] Backend unit tests - A tier (6 files)
echo ========================================================
call :run_mvn JwtUtilTest "1/6 auth - JWT util"
call :run_mvn TaskStateMachineTest "2/6 task - state machine"
call :run_mvn TaskChangeServiceValidateTest "3/6 task - change validation"
call :run_mvn TaskChangeServiceTest "4/6 task - change service chain"
call :run_mvn AuthLoginPolicyTest "5/6 auth - login policy"
call :run_mvn TaskTagServiceTest "6/6 task - tag service"

echo ========================================================
echo [2/3] Backend tests - B tier (2 files)
echo ========================================================
call :run_mvn LogDatePolicyTest "7/8 log - date policy [V2.0 D-06]"
call :run_mvn LogContentRulesTest "8/8 log - content rules [V2.0 D-05]"

rem ==== [CHANGED 2/2] 用累积的 exec 数据生成 HTML 覆盖率报告 ====
echo [JaCoCo] generating coverage report...
cd /d "%ROOT%\pandora-backend"
call "%MVN%" jacoco:report > "%TEMP%\pandora_jacoco.log" 2>&1
if exist "%ROOT%\pandora-backend\target\site\jacoco\index.html" (
    echo [OK] JaCoCo report generated
) else (
    echo [WARN] JaCoCo report NOT generated - log: %TEMP%\pandora_jacoco.log
)
echo.

echo ========================================================
echo [3/3] Android unit tests (9 files)
echo ========================================================
cd /d "%ROOT%"
call :run_gradle com.example.project_pandora.core.divination.DivinationEngineTest "9/17 divination engine"
call :run_gradle com.example.project_pandora.core.util.NumberCompatTest "10/17 number compat"
call :run_gradle com.example.project_pandora.core.util.TaskDateTimesTest "11/17 task date times"
call :run_gradle com.example.project_pandora.core.util.LogDateWindowTest "12/17 log date window"
call :run_gradle com.example.project_pandora.core.util.AvatarUploadPolicyTest "13/17 avatar upload policy"
call :run_gradle com.example.project_pandora.core.util.TaskAssigneesTest "14/17 task assignees"
call :run_gradle com.example.project_pandora.core.network.AiEndpointTest "15/17 AI endpoint"
call :run_gradle com.example.project_pandora.core.util.LogEntryRulesTest "16/17 log entry rules"
call :run_gradle com.example.project_pandora.core.util.SignatureRulesTest "17/17 signature rules"

echo ========================================================
echo Done. Start: %START%
echo End:   %date% %time%
echo PASSED: %PASSED%   FAILED: %FAILED%
echo Backend detail: pandora-backend\target\surefire-reports\
echo Backend cover : pandora-backend\target\site\jacoco\index.html
echo Android report: app\build\reports\tests\testDebugUnitTest\index.html
echo ========================================================
pause
exit /b 0

:run_mvn
cd /d "%ROOT%\pandora-backend"
echo [TEST] %~2 (%~1)
call "%MVN%" test -Dtest=%~1 > "%TEMP%\pandora_last.log" 2>&1
findstr /C:"Tests run:" /C:"BUILD" "%TEMP%\pandora_last.log"
findstr /C:"BUILD SUCCESS" "%TEMP%\pandora_last.log" >nul && (set /a PASSED+=1 & echo [PASS] %~2) || (set /a FAILED+=1 & echo [FAIL] %~2 - full log: %TEMP%\pandora_last.log)
echo.
goto :eof

:run_gradle
echo [TEST] %~2 (%~1)
call gradlew :app:testDebugUnitTest --tests "%~1" --console=plain > "%TEMP%\pandora_last.log" 2>&1
findstr /C:"BUILD" "%TEMP%\pandora_last.log"
findstr /C:"BUILD SUCCESSFUL" "%TEMP%\pandora_last.log" >nul && (set /a PASSED+=1 & echo [PASS] %~2) || (set /a FAILED+=1 & echo [FAIL] %~2 - full log: %TEMP%\pandora_last.log)
echo.
goto :eof
