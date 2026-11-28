# task-management Specification

## Purpose
TBD - created by archiving change initial-mvp-delivery. Update Purpose after archive.

## Requirements

### Requirement: Task Creation
The system SHALL allow users to create tasks with name, priority, assignee, importance and urgency.

#### Scenario: Create personal task
GIVEN the user is logged in
WHEN they fill in task name and priority and submit
THEN the task is created and appears in their personal task list

#### Scenario: Dispatch task to assignee
GIVEN a superior is logged in
WHEN they fill in all 6 core indicators and specify an assignee
THEN the task is dispatched and the assignee receives it

### Requirement: Quadrant View
The system SHALL display tasks in a four-quadrant view based on importance and urgency.

#### Scenario: View quadrant
GIVEN the user has tasks with different importance/urgency
WHEN they switch to quadrant view
THEN tasks are displayed in four quadrants

### Requirement: Task Execution State Machine

任务执行状态采用两态模型。数据库层自 2026-11-06 迁移起以 ENUM('IN_PROGRESS','COMPLETED') 强制约束,历史值 PENDING/ACCEPTED/REJECTED 收敛为 IN_PROGRESS、CONFIRMED 收敛为 COMPLETED(migrations/20261106_task_daily_directory.sql)。

任务创建后初始状态即为 IN_PROGRESS:创建语句显式赋值,建表 DEFAULT 'IN_PROGRESS'(TaskChangeService.apply、pandora_schema.sql:88)。

系统 SHALL 仅支持以下流转,其余任何状态组合 SHALL 被状态机拒绝:

| 从 | 到 | 允许的触发者 | 副作用 |
|---|---|---|---|
| IN_PROGRESS | COMPLETED | 任务负责人 | 通知派发人 |
| COMPLETED | IN_PROGRESS | 任务负责人或创建人 | 恢复为进行中 |

#### Scenario: 负责人标记任务完成
- **WHEN** 任务负责人对 IN_PROGRESS 的任务执行完成操作
- **THEN** 状态流转为 COMPLETED 并通知派发人

#### Scenario: 非负责人标记完成被拒绝
- **WHEN** 非任务负责人的用户尝试将任务标记为完成
- **THEN** 操作被拒绝,状态保持 IN_PROGRESS

#### Scenario: 恢复为进行中
- **WHEN** 任务负责人或创建人对 COMPLETED 的任务执行恢复操作
- **THEN** 状态流转回 IN_PROGRESS

#### Scenario: 非法流转被拒绝
- **WHEN** 提交上表之外的任意状态组合
- **THEN** 状态机拒绝该流转并保持原状态

#### Scenario: 新建任务直接进入进行中
- **WHEN** 一条任务创建申请经审批通过落库
- **THEN** tasks.status 为 IN_PROGRESS,并向被指派人发出新任务通知

**更正说明**(随本需求一并生效):

1. 原需求 Task Status Transition 的五态描述为 2026-11-06 迁移前的旧模型,且遗漏当时真实存在的 ACCEPTED;迁移已将全部历史值收敛为两态并以 ENUM 强制约束。
2. 原文引用的 TaskListAdapter.getStatusDisplayName、TaskListFragment.setupTabs 在当前代码库不存在,对应旧模型 UI,已随迁移与界面重构失效。
3. "退回/确认"语义由申请-审批域承担:任务创建与修改经 task_change_requests 申请-审批流(TaskChangeService),权限、密码重置等域各有独立的 APPROVED/REJECTED 生命周期(Android 端对应 TaskReviewsActivity、ResetRequestsActivity),均不属于任务执行状态。
