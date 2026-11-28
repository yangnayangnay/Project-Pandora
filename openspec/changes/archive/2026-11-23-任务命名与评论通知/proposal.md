# Proposal: 任务名称与评论通知

## Why
2026-11-23 完成下属任务名称显示与日志评论通知功能（v1.0.1）。

## What Changes

### 1. 任务名称显示
- 显示下属任务名称
- 任务层级展示优化

### 2. 评论通知
- 日志评论通知
- 通知系统完善

## Impact
- 任务层级展示清晰
- 评论通知及时

## Evidence

| 哈希 | 日期 | 主题 |
|------|------|------|
| 27224d6 | 2026-11-23 | fix: show subordinate task names and notify log comments |

### 文件证据（摘自 git_history_files.txt）
- `27224d6`: pandora-backend/src/main/java/.../notification/NotificationService.java, pandora-backend/src/main/java/.../task/TaskController.java, app/src/main/java/.../ui/task/TaskListAdapter.java