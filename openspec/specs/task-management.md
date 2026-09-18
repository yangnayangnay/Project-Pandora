# Task Management

## Capability
任务全生命周期管理：创建、分发、接收、进度更新、确认完成、退回。支持四象限分类（重要紧急/重要不紧急/紧急不重要/不重要不紧急）和按截止日期自动计算紧急程度。

## Requirements
- 任务须包含6项核心指标：名称、优先级、完成情况、时间节点、进度说明、责任人
- 任务状态流转：PENDING → IN_PROGRESS → COMPLETED → CONFIRMED / REJECTED
- 四象限视图按重要×紧急分类展示
- 紧急程度按deadline自动计算：当天/逾期=深红, <3天=淡红, ≤10天=黄色, >10天=绿色
- 用户可在任务详情手动调整紧急程度

## Scenarios

### Create personal task
GIVEN 员工已登录
WHEN 填写任务名称和优先级并提交
THEN 任务创建成功并出现在个人任务列表

### Dispatch task
GIVEN 上级已登录
WHEN 填写完整6项指标并指定责任人
THEN 任务分发成功，责任人收到任务

### Update progress and mark complete
GIVEN 员工是任务责任人且状态为IN_PROGRESS
WHEN 更新进度备注并标记为COMPLETED
THEN 任务状态变为COMPLETED，上级收到待确认通知

### Confirm task
GIVEN 上级是任务分发人且状态为COMPLETED
WHEN 确认任务完成
THEN 任务状态变为CONFIRMED，流程结束

### Quadrant view
GIVEN 用户有多个任务
WHEN 切换到四象限视图
THEN 任务按重要×紧急四类分区展示