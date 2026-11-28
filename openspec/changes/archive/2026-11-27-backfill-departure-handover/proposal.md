# Proposal: 离职交接与账号恢复

## Why
2026-11-27 完成离职交接与账号恢复功能，确保人员变动时业务连续性。

## What Changes

### 1. 离职交接
- 离职交接流程
- 工作交接记录
- 权限转移

### 2. 账号恢复
- 账号恢复功能
- 离职人员账号处理

## Impact
- 人员变动时业务连续性保障
- 离职交接流程规范
- 账号恢复能力

## Evidence

| 哈希 | 日期 | 主题 |
|------|------|------|
| 0efb031 | 2026-11-27 | feat: 离职交接功能 |
| 22908de | 2026-11-27 | feat: 账号恢复功能 |

### 文件证据（摘自 git_history_files.txt）
- `0efb031`: pandora-backend/src/main/java/.../user/AccountDepartureService.java, pandora-backend/src/main/java/.../audit/AuditService.java
- `22908de`: pandora-backend/src/main/java/.../auth/..., pandora-backend/src/main/java/.../user/UserController.java

### 关联归档
本归档与 `2026-11-28-notification-actions-and-handover-audit` 存在功能关联：
- 本归档（2026-11-27-backfill-departure-handover）：实现离职交接基础功能（提交 `0efb031`, `22908de`）
- 对方归档（2026-11-28-notification-actions-and-handover-audit）：增强离职交接审计日志的可视化展示

两者覆盖不同的功能层面，本归档提供基础数据与流程，对方归档提供审计日志的可读展示增强。