# Tasks: 专注模式入口移除补录

> 本变更记录 initial-mvp-delivery 阶段专注模式入口已在 Round19 移除，但 tasks.md 仍要求验证入口启动。

## 1. 入口移除

- [x] 移除"我的"页专注模式入口（`app/src/main/java/com/example/project_pandora/ui/mine/MineFragment.java:57-58`，`binding.btnFocus.setVisibility(View.GONE)`）
- [x] 保留 FocusActivity 源码（`app/src/main/java/com/example/project_pandora/ui/focus/FocusActivity.java`）

## 2. 任务状态变更

- [x] 更新 tasks.md 第126行状态：从 `[ ]` 改为已过时
- [x] 记录移除原因：Round19 REQ-19-10

## 3. 验收

- [x] 入口已移除（commit `7908417`）
- [x] FocusActivity 源码保留（commit `7908417`）
- [x] 任务状态已更新为已过时