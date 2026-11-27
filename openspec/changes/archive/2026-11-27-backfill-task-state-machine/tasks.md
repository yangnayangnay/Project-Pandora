# Tasks: TaskStateMachine 规格不一致补录

> 本变更记录 initial-mvp-delivery 阶段 TaskStateMachine 单元测试已补齐，但代码实现与规格文档存在不一致。

## 1. 单元测试补齐

- [x] 创建 TaskStateMachineTest 测试类（`pandora-backend/src/test/java/com/pandora/backend/task/TaskStateMachineTest.java`，29 条用例）
- [x] 覆盖正常流转场景（TC-SM-01, TC-SM-04, TC-SM-07, TC-SM-09, TC-SM-10, TC-SM-11, TSM-103）
- [x] 覆盖异常流转场景（TC-SM-02, TC-SM-03, TC-SM-05, TC-SM-06, TC-SM-06b, TC-SM-08, TC-SM-12, TSM-106, TSM-107, TSM-108）
- [x] 覆盖边界场景（TC-SM-13, TC-SM-14）

## 2. 规格与代码不一致记录

- [x] 记录规格描述：5 态状态流转（PENDING → IN_PROGRESS → COMPLETED → CONFIRMED/REJECTED）
- [x] 记录代码实现：仅 2 态（IN_PROGRESS ↔ COMPLETED）
- [x] 记录 TaskStatus 枚举仅包含 IN_PROGRESS, COMPLETED
- [x] 记录 validateTransition 仅校验 isAssignee/isSuperior，未实现完整角色权限矩阵

## 3. 验收

- [x] 单元测试已补齐（commit `65e7df0`, `567b727`）
- [x] 规格与代码不一致已记录