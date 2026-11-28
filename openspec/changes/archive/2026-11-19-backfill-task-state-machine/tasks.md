# Tasks: TaskStateMachine 规格不一致补录

> 本变更记录 initial-mvp-delivery 阶段 TaskStateMachine 单元测试已补齐，但代码实现与规格文档存在不一致。

## 1. 单元测试补齐

- [x] [2026-11-19] 分层测试套件 17/17 全绿，后端150用例，LogContentRulesTest 扩至11条 (567b727)
- [x] [2026-11-26] 补齐后端单测并恢复全绿（全量25类354条）(65e7df0)

## 2. 规格与代码不一致记录

- [x] 记录规格描述：5 态状态流转（PENDING → IN_PROGRESS → COMPLETED → CONFIRMED/REJECTED）
- [x] 记录代码实现：仅 2 态（IN_PROGRESS ↔ COMPLETED）
- [x] 记录 TaskStatus 枚举仅包含 IN_PROGRESS, COMPLETED
- [x] 记录 validateTransition 仅校验 isAssignee/isSuperior，未实现完整角色权限矩阵

## 3. 验收

- [x] 单元测试已补齐
- [x] 规格与代码不一致已记录