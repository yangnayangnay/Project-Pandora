# 任务清单

## 1. 证据核验
- [x] 1.1 数据库层:pandora_schema.sql:88/331 两态 ENUM 与 DEFAULT
- [x] 1.2 迁移脚本:20261106_task_daily_directory.sql 旧值收敛规则
- [x] 1.3 状态机:TaskStateMachine 枚举与合法流转边
- [x] 1.4 初始状态:TaskChangeService.apply CREATE 分支硬编码 'IN_PROGRESS'
- [x] 1.5 幽灵引用:TaskListAdapter/TaskListFragment 全库零命中
- [x] 1.6 退回/确认语义归属:task_change_requests、Approval、PasswordReset、TaskReviewsActivity

## 2. 规格修正
- [x] 2.1 REMOVED Task Status Transition + ADDED Task Execution State Machine(两态)
- [x] 2.2 五个新场景覆盖当前实现行为,更正说明随需求落档
- [x] 2.3 openspec 校验器场景覆盖语义已实测并以 REMOVED+ADDED 适配

## 3. 落库
- [x] 3.1 openspec validate 通过
- [x] 3.2 归档并应用 delta:旧需求移除,新需求落位
- [x] 3.3 Purpose TBD 占位补全
- [x] 3.4 与僵尸目录清理、口径修正一并提交