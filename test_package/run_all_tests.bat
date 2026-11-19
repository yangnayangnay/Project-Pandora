@echo off
chcp 65001 >nul
title Project Pandora 一键测试

set START_TIME=%DATE% %TIME%
echo ========================================
echo   Project Pandora 一键测试脚本
echo   开始时间: %START_TIME%
echo ========================================

:: ========== 检查必要文件 ==========
echo [检查] 验证测试环境...
cd /d "%~dp0"

if not exist "pandora-backend\src\test\resources\db\h2_schema.sql" (
    echo [警告] 未找到 h2_schema.sql，后端数据库测试可能失败
    echo [提示] 路径: pandora-backend\src\test\resources\db\h2_schema.sql
)

:: 使用完整的 mvn 路径
set MVN_PATH=D:\desktop\文件夹\Tools\apache-maven-3.9.11\bin\mvn

if exist "%MVN_PATH%" (
    echo [OK] Maven 已找到
) else (
    echo [错误] 未找到 mvn，请检查路径: %MVN_PATH%
    pause
    exit /b 1
)

where gradlew >nul 2>nul && (
    echo [OK] Gradle 已安装
) || (
    echo [错误] 未找到 gradlew
    pause
    exit /b 1
)

echo.

:: ========== 第一部分：后端测试 (A档主力) ==========
echo ========================================
echo [1/3] 后端单元测试 - A档主力 (6个文件)
echo ========================================
echo.

cd /d "%~dp0pandora-backend"

:: 1. JwtUtilTest - 认证模块
echo [测试] 1/6 - 认证模块 (JwtUtilTest)
call "%MVN_PATH%" test -Dtest=JwtUtilTest 2>&1 | findstr /C:"Tests run:" /C:"BUILD"
if errorlevel 1 (
    echo [失败] JwtUtilTest 执行出错
) else (
    echo [完成] JwtUtilTest 测试结束
)
echo.

:: 2. TaskStateMachineTest - 状态流转
echo [测试] 2/6 - 状态流转 (TaskStateMachineTest)
call "%MVN_PATH%" test -Dtest=TaskStateMachineTest 2>&1 | findstr /C:"Tests run:" /C:"BUILD"
if errorlevel 1 (
    echo [失败] TaskStateMachineTest 执行出错
) else (
    echo [完成] TaskStateMachineTest 测试结束
)
echo.

:: 3. TaskChangeServiceValidateTest - 业务校验
echo [测试] 3/6 - 业务校验 (TaskChangeServiceValidateTest)
call "%MVN_PATH%" test -Dtest=TaskChangeServiceValidateTest 2>&1 | findstr /C:"Tests run:" /C:"BUILD"
if errorlevel 1 (
    echo [失败] TaskChangeServiceValidateTest 执行出错
) else (
    echo [完成] TaskChangeServiceValidateTest 测试结束
)
echo.

:: 4. TaskChangeServiceTest - 任务变更业务链
echo [测试] 4/6 - 任务变更业务链 (TaskChangeServiceTest)
call "%MVN_PATH%" test -Dtest=TaskChangeServiceTest 2>&1 | findstr /C:"Tests run:" /C:"BUILD"
if errorlevel 1 (
    echo [失败] TaskChangeServiceTest 执行出错 (可能需要数据库)
) else (
    echo [完成] TaskChangeServiceTest 测试结束
)
echo.

:: 5. AuthLoginPolicyTest - 登录策略
echo [测试] 5/6 - 登录策略 (AuthLoginPolicyTest)
call "%MVN_PATH%" test -Dtest=AuthLoginPolicyTest 2>&1 | findstr /C:"Tests run:" /C:"BUILD"
if errorlevel 1 (
    echo [失败] AuthLoginPolicyTest 执行出错 (可能需要数据库)
) else (
    echo [完成] AuthLoginPolicyTest 测试结束
)
echo.

:: 6. TaskTagServiceTest - 任务标签
echo [测试] 6/6 - 任务标签 (TaskTagServiceTest)
call "%MVN_PATH%" test -Dtest=TaskTagServiceTest 2>&1 | findstr /C:"Tests run:" /C:"BUILD"
if errorlevel 1 (
    echo [失败] TaskTagServiceTest 执行出错 (可能需要数据库)
) else (
    echo [完成] TaskTagServiceTest 测试结束
)
echo.

echo [完成] 后端 A 档主力测试结束
echo.

:: ========== 第二部分：后端测试 (争取带上) ==========
echo ========================================
echo [2/3] 后端测试 - 争取带上 (2个文件)
echo ========================================
echo.

:: 7. LogDatePolicyTest - 日志日期策略 (V2.0 D-06)
echo [测试] 7/8 - 日志日期策略 (LogDatePolicyTest) [V2.0 D-06]
call "%MVN_PATH%" test -Dtest=LogDatePolicyTest 2>&1 | findstr /C:"Tests run:" /C:"BUILD"
if errorlevel 1 (
    echo [失败] LogDatePolicyTest 执行出错 (编译不过就放弃)
) else (
    echo [完成] LogDatePolicyTest 测试结束
)
echo.

:: 8. LogContentRulesTest - 日志内容规则 (V2.0 D-05)
echo [测试] 8/8 - 日志内容规则 (LogContentRulesTest) [V2.0 D-05]
call "%MVN_PATH%" test -Dtest=LogContentRulesTest 2>&1 | findstr /C:"Tests run:" /C:"BUILD"
if errorlevel 1 (
    echo [失败] LogContentRulesTest 执行出错 (编译不过就放弃)
) else (
    echo [完成] LogContentRulesTest 测试结束
)
echo.

echo [完成] 后端全部测试结束
echo.

:: ========== 第三部分：Android 测试 ==========
echo ========================================
echo [3/3] Android 单元测试 (9个文件)
echo ========================================
echo.

cd /d "%~dp0"

:: 9. DivinationEngineTest - 占卜引擎
echo [测试] 9/17 - 占卜引擎 (DivinationEngineTest)
call gradlew :app:testDebugUnitTest --tests "com.example.project_pandora.core.divination.DivinationEngineTest" --console=plain 2>&1 | findstr /C:"BUILD"
if errorlevel 1 (
    echo [失败] DivinationEngineTest 执行出错
) else (
    echo [完成] DivinationEngineTest 测试结束
)
echo.

:: 10. NumberCompatTest - 数字兼容性
echo [测试] 10/17 - 数字兼容性 (NumberCompatTest)
call gradlew :app:testDebugUnitTest --tests "com.example.project_pandora.core.util.NumberCompatTest" --console=plain 2>&1 | findstr /C:"BUILD"
if errorlevel 1 (
    echo [失败] NumberCompatTest 执行出错
) else (
    echo [完成] NumberCompatTest 测试结束
)
echo.

:: 11. TaskDateTimesTest - 任务日期时间
echo [测试] 11/17 - 任务日期时间 (TaskDateTimesTest)
call gradlew :app:testDebugUnitTest --tests "com.example.project_pandora.core.util.TaskDateTimesTest" --console=plain 2>&1 | findstr /C:"BUILD"
if errorlevel 1 (
    echo [失败] TaskDateTimesTest 执行出错
) else (
    echo [完成] TaskDateTimesTest 测试结束
)
echo.

:: 12. LogDateWindowTest - 日志日期窗口
echo [测试] 12/17 - 日志日期窗口 (LogDateWindowTest)
call gradlew :app:testDebugUnitTest --tests "com.example.project_pandora.core.util.LogDateWindowTest" --console=plain 2>&1 | findstr /C:"BUILD"
if errorlevel 1 (
    echo [失败] LogDateWindowTest 执行出错
) else (
    echo [完成] LogDateWindowTest 测试结束
)
echo.

:: 13. AvatarUploadPolicyTest - 头像上传策略
echo [测试] 13/17 - 头像上传策略 (AvatarUploadPolicyTest)
call gradlew :app:testDebugUnitTest --tests "com.example.project_pandora.core.util.AvatarUploadPolicyTest" --console=plain 2>&1 | findstr /C:"BUILD"
if errorlevel 1 (
    echo [失败] AvatarUploadPolicyTest 执行出错
) else (
    echo [完成] AvatarUploadPolicyTest 测试结束
)
echo.

:: 14. TaskAssigneesTest - 任务负责人
echo [测试] 14/17 - 任务负责人 (TaskAssigneesTest)
call gradlew :app:testDebugUnitTest --tests "com.example.project_pandora.core.util.TaskAssigneesTest" --console=plain 2>&1 | findstr /C:"BUILD"
if errorlevel 1 (
    echo [失败] TaskAssigneesTest 执行出错
) else (
    echo [完成] TaskAssigneesTest 测试结束
)
echo.

:: 15. AiEndpointTest - AI端点
echo [测试] 15/17 - AI端点 (AiEndpointTest)
call gradlew :app:testDebugUnitTest --tests "com.example.project_pandora.core.network.AiEndpointTest" --console=plain 2>&1 | findstr /C:"BUILD"
if errorlevel 1 (
    echo [失败] AiEndpointTest 执行出错
) else (
    echo [完成] AiEndpointTest 测试结束
)
echo.

:: 16. LogEntryRulesTest - 日志条目规则
echo [测试] 16/17 - 日志条目规则 (LogEntryRulesTest)
call gradlew :app:testDebugUnitTest --tests "com.example.project_pandora.core.util.LogEntryRulesTest" --console=plain 2>&1 | findstr /C:"BUILD"
if errorlevel 1 (
    echo [失败] LogEntryRulesTest 执行出错
) else (
    echo [完成] LogEntryRulesTest 测试结束
)
echo.

:: 17. SignatureRulesTest - 签名规则
echo [测试] 17/17 - 签名规则 (SignatureRulesTest)
call gradlew :app:testDebugUnitTest --tests "com.example.project_pandora.core.util.SignatureRulesTest" --console=plain 2>&1 | findstr /C:"BUILD"
if errorlevel 1 (
    echo [失败] SignatureRulesTest 执行出错
) else (
    echo [完成] SignatureRulesTest 测试结束
)
echo.

echo [完成] Android 全部测试结束
echo.

:: ========== 输出结果 ==========
echo ========================================
echo   测试执行完成
echo ========================================
echo.
echo 开始时间: %START_TIME%
echo 结束时间: %DATE% %TIME%
echo.
echo 测试报告位置:
echo   后端测试明细: pandora-backend\target\surefire-reports\
echo   后端覆盖率: pandora-backend\target\site\jacoco\index.html
echo   Android报告: app\build\reports\tests\testDebugUnitTest\index.html
echo.
echo 测试文件统计:
echo   后端 A 档: 6 个 (JwtUtilTest, TaskStateMachineTest, TaskChangeServiceValidateTest, TaskChangeServiceTest, AuthLoginPolicyTest, TaskTagServiceTest)
echo   后端 B 档: 2 个 (LogDatePolicyTest, LogContentRulesTest)
echo   Android: 9 个 (DivinationEngineTest, NumberCompatTest, TaskDateTimesTest, LogDateWindowTest, AvatarUploadPolicyTest, TaskAssigneesTest, AiEndpointTest, LogEntryRulesTest, SignatureRulesTest)
echo   总计: 17 个测试文件
echo.
echo 真实用例数以 mvn/gradle 输出的 "Tests run: X" 为准
echo.
echo ========================================

pause