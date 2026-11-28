# 提案:correct-task-state-spec——重构主规格的任务状态需求

## Why

主规格 specs/task-management/spec.md 的 Task Status Transition 描述五态模型(PENDING/IN_PROGRESS/COMPLETED/CONFIRMED/REJECTED)并引用 TaskListAdapter.getStatusDisplayName、TaskListFragment.setupTabs 等锚点。经 2026-11-27 全链路代码核验,该描述对应 2026-11-06 迁移之前的旧模型,此后未随迁移更新:

| 证据 | 出处 |
|---|---|
| tasks.status 定义为 ENUM('IN_PROGRESS','COMPLETED') NOT NULL DEFAULT 'IN_PROGRESS',数据库层即强制两态 | pandora_schema.sql:88、331 |
| 迁移脚本收敛旧值:PENDING/ACCEPTED/REJECTED→IN_PROGRESS,CONFIRMED→COMPLETED,并将列改为两态 ENUM | migrations/20261106_task_daily_directory.sql:26-30;h2_schema.sql:87-88 |
| TaskStateMachine 枚举仅含 IN_PROGRESS/COMPLETED,仅放行两条流转边 | task/TaskStateMachine.java |
| 任务创建硬编码初始状态 'IN_PROGRESS',创建本身经 task_change_requests 申请-审批流落库 | task/TaskChangeService.java apply() |
| 旧规格引用的 TaskListAdapter/TaskListFragment 在当前 app/src/main 不存在 | 全库检索零命中 |
| "退回/确认"语义由申请-审批域承担(task_change_requests、权限审批、密码重置各有 APPROVED/REJECTED 生命周期) | TaskChangeService、ApprovalController、PasswordResetController、TaskReviewsActivity |

补充:旧模型实际含 ACCEPTED 共六态(见迁移脚本),旧规格当时亦未记载——其既不完整、也已过时,本变更以当前代码为事实基准予以修正。

## What Changes

- REMOVED Task Status Transition:五态旧模型需求整体移除(含 Start/Complete/Confirm/Reject/Resubmit/REJECTED 显示六个场景)。
- ADDED Task Execution State Machine:两态模型新需求(初始状态 IN_PROGRESS;IN_PROGRESS↔COMPLETED 双向流转及角色约束;五个场景;附更正说明)。
- 补全主规格 Purpose 的 TBD 占位。
- 注:openspec 校验器不允许 MODIFIED 丢弃既有场景(2026-11-27 实测报错),故以 REMOVED+ADDED 显式完成需求重构。

## Impact

- 受影响规格:specs/task-management/spec.md(Task Status Transition 移除,Task Execution State Machine 落位,Purpose 补全)
- 受影响代码:无(纯规格更正,以代码为事实基准)
- 关联归档:2026-11-19-backfill-task-state-machine 的"仅 2 态"结论经本次核验确认无误