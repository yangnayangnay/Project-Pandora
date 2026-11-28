# Proposal: Round 23-45 云部署与通知系统

## Why
2026-10-23 至 10-24 完成 Round 23-45 迭代，包含云部署、通知系统、暗色主题、多端同步、401 治理等功能。

## What Changes

### 1. 云部署与配置
- 服务器地址可配置化
- 云部署支持

### 2. 通知系统
- 通知系统基础架构
- 息屏通知保活

### 3. 主题与同步
- 暗色主题支持
- 多端同步
- 多服务端故障转移

### 4. 任务与视图
- 任务正文功能
- 月视图渲染统一
- 交接清理

### 5. 文档更新
- 更新 README 至当前项目状态

## Impact
- 云部署能力上线
- 通知系统完善
- 多端同步稳定
- 暗色主题支持

## Evidence

| 哈希 | 日期 | 主题 |
|------|------|------|
| 4443fcc | 2026-10-23 | Round23~43: 服务器地址可配置化+云部署+通知系统+暗色主题+任务正文+多端同步+401治理 |
| 0c0476b | 2026-10-23 | Round44+45: 多服务端故障转移+交接清理+息屏通知保活+月视图渲染统一 |
| a131ff3 | 2026-10-24 | docs: 更新 README 至当前项目状态 |

### 文件证据（摘自 git_history_files.txt）
- `4443fcc`: app/src/main/java/.../core/sync/..., app/src/main/java/.../notification/..., app/src/main/java/.../core/theme/ThemeManager.java
- `0c0476b`: app/src/main/java/.../core/sync/SyncManager.java, app/src/main/java/.../notification/TaskNotificationService.java