# Proposal: 测试恢复与 Schema 统一

## Why
2026-11-26 完成测试恢复、口径修正、Schema 统一。

## What Changes

### 1. 测试恢复
- 补齐后端单测并恢复全绿
- 跑批脚本纳入8个新后端测试类
- 补充 AI 测试工作底稿

### 2. 口径修正
- 测试报告口径修正为官方回归 25 类（后端16类+Android9类）
- 附录B按16类277条重写
- Android用例数取实测值
- 修正脚本 schema 检查路径

### 3. Schema 统一
- 删除测试遗留的重复 H2 schema
- 统一使用主 schema 单一来源
- 修复编译错误
- 更新测试报告

## Impact
- 测试套件恢复全绿
- 测试口径统一为 25 类 354 条
- Schema 统一

## Evidence

| 哈希 | 日期 | 主题 |
|------|------|------|
| 140d53a | 2026-11-26 | build: 跑批脚本纳入8个新后端测试类(16后端+9安卓=25类),加gradlew检查 |
| f4395b8 | 2026-11-26 | docs: 补充AI测试工作底稿(未覆盖函数清单/结构化用例清单) |
| ae19d32 | 2026-11-26 | chore: 删除测试遗留的重复 H2 schema，统一使用主 schema 单一来源 |
| 3e608a6 | 2026-11-26 | fix: 修复编译错误 + 更新测试报告至 25类354条 |
| 5d8a02b | 2026-11-26 | docs: 测试报告口径修正为官方回归 25 类(后端16+Android9) |
| 4ef0ca1 | 2026-11-26 | docs: 附录B按16类277条重写,Android用例数取实测值,修正脚本schema检查路径 |

> 注：提交 `65e7df0` 已在 backfill-task-state-machine 归档中覆盖。

### 文件证据（摘自 git_history_files.txt）
- `140d53a`: run_all_tests.bat, pandora-backend/src/test/java/...
- `ae19d32`: pandora-backend/src/test/resources/db/h2_schema.sql (deleted), schema_diag.bat
- `5d8a02b`: update_test_report_official.py, docx/docx_generate/测试报告摘要.md