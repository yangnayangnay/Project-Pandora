# Proposal: Round 17-19 加固迭代

## Why
2026-10-16 完成 Round 17-19 加固迭代，修复 7 类试用缺陷，实现审计日志中文化、权限收口、人员关系树形排序等功能。

## What Changes

### 1. 缺陷修复（Round 17）
- Top10 去重
- 提示精细化
- 导图日志优化
- 人员关系树形管理

### 2. 审计日志与人员关系
- 人员关系树子节点补充 username 字段
- 操作类型中文化显示 + 审计日志全覆盖
- 个人十大信息展示增加账号信息自动生成条目

### 3. 数据清理与优化
- seed 数据用户名统一使用编号后缀
- 后端启动时自动清理 top10 测试脏数据
- 公司十大显示/人员关系树形排序返回/通知系统/Web 端字段命名/账号信息实时生效

### 4. Round 19 功能
- Web 菜单精简/权限收口/加权排序/FAB 迁移/梅花易数等 12 项需求
- 为 AuditLogActivity/UserRelationActivity/DivinationActivity 补充返回按钮

## Impact
- 7 类试用缺陷修复
- 审计日志中文化
- 权限收口完成
- 人员关系管理完善

## Evidence
- `44ac282` fix(round17): 修复7类试用缺陷
- `bb42f36` fix: 人员关系树子节点补充 username 字段
- `24d4924` fix(audit): 操作类型中文化显示 + 审计日志全覆盖
- `45e1035` docs: 更新周报至 2026-10-16
- `01323c6` fix: 个人十大信息展示增加账号信息自动生成条目
- `3a069d3` fix: seed 数据用户名统一使用编号后缀
- `22ceac4` fix: 后端启动时自动清理 top10 测试脏数据
- `df286a8` fix(round18): 公司十大显示/人员关系树形排序返回/通知系统
- `7908417` feat(round19): Web菜单精简/权限收口/加权排序/FAB迁移/梅花易数等12项需求
- `1d25e78` fix: 为 AuditLogActivity/UserRelationActivity/DivinationActivity 补充返回按钮