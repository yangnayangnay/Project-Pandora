# Proposal: 专注模式入口移除补录

## 变更性质
**事后补录** — 本变更记录 initial-mvp-delivery 阶段专注模式入口已在 Round19 移除，但 tasks.md 仍要求验证入口启动。

## 变更范围

### 已实现变更
1. **专注模式入口移除** — `app/src/main/java/com/example/project_pandora/ui/mine/MineFragment.java:57-58`
   - 注释：`// Round19 REQ-19-10: 移除专注模式入口（保留FocusActivity源码）`
   - 代码：`binding.btnFocus.setVisibility(View.GONE);`

2. **FocusActivity 保留** — `app/src/main/java/com/example/project_pandora/ui/focus/FocusActivity.java`
   - 源码保留但无入口
   - 可通过其他方式启动（如通知）

### 任务状态变更
**原任务（tasks.md 第126行）：**
- 验证专注模式从"我的"页入口启动独立 FocusActivity，不在底部导航中

**实际状态：**
- 入口已移除，任务已过时

### Non-goals
- 不恢复专注模式入口
- 不删除 FocusActivity 源码
- 不展望未来功能

## 覆盖模块
- Android (ui/mine, ui/focus)

## 对应 Commit
- `7908417` feat(round19): Web菜单精简/权限收口/加权排序/FAB迁移/梅花易数等12项需求
- `9367c13` Round19-9req:5:删除客户端注册+导图页不可编辑+日志展示优化+草稿清空+FAB拆分+删除模板/AI起草+多行输入

## 验收标准
- [x] 入口已移除
- [x] FocusActivity 源码保留
- [x] 任务状态已更新为已过时