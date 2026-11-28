# Proposal: TaskStateMachine 规格不一致补录

## 变更性质
**事后补录** — 本变更记录 initial-mvp-delivery 阶段 TaskStateMachine 单元测试已补齐，但代码实现与规格文档存在不一致。

## 变更范围

### 已实现内容
1. **TaskStateMachine 单元测试** — `pandora-backend/src/test/java/com/pandora/backend/task/TaskStateMachineTest.java`
   - 29 条测试用例
   - 覆盖正常流转、非法流转、权限校验、枚举功能等场景

### 规格与代码不一致
**规格文档（specs/task-management/spec.md）描述：**
- 5 态状态流转：PENDING → IN_PROGRESS → COMPLETED → CONFIRMED/REJECTED
- 含 REJECTED 回流

**代码实际实现（TaskStateMachine.java）：**
- 仅 2 态：IN_PROGRESS ↔ COMPLETED
- 不含 PENDING/CONFIRMED/REJECTED 状态
- `TaskStatus` 枚举仅包含 IN_PROGRESS, COMPLETED

### Non-goals
- 不修改状态机实现
- 不新增状态
- 不展望未来功能

## 覆盖模块
- pandora-backend (task)

## Evidence

| 哈希 | 日期 | 主题 |
|------|------|------|
| 567b727 | 2026-11-19 | test: 分层测试套件 17/17 全绿,后端150用例,LogContentRulesTest 扩至11条 |
| 65e7df0 | 2026-11-26 | test: 补齐后端单测并恢复全绿(全量25类354条) |

## 验收标准
- [x] 单元测试已补齐
- [x] 规格与代码不一致已记录