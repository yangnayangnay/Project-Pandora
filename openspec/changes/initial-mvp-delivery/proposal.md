# Proposal: Initial MVP Delivery

## Summary
交付 Project Pandora 初始 MVP 版本，包含三端（Android、后端、Web管理端）的核心功能实现。

## Motivation
项目要求在 Sprint 0 完成需求分析与产品设计，Sprint 1 交付首个 MVP。当前已完成用户注册登录、任务管理、日志管理、视图展示、AI分析等核心功能，需通过 OpenSpec 记录已交付能力。

## Scope

### Epic-1: Task Management
- ✅ 任务创建（个人任务+分发任务）
- ✅ 任务详情查看与状态流转（PENDING→IN_PROGRESS→COMPLETED→CONFIRMED/REJECTED）
- ✅ 四象限视图
- ✅ 紧急程度自动计算与手动调整

### Epic-2: Work Log Management
- ✅ 日志录入与查询
- ✅ 十大重要工作编辑
- ✅ 日志统计图表

### Epic-3: View Display
- ✅ 月/周/日视图切换
- ✅ 导图页四大板块

### Epic-4: AI Analysis
- ✅ MBTI测试（12/24/36题三版本）
- ✅ 关键词分析
- ✅ AI个性化建议
- ✅ 每日占卜（梅花易数）
- ✅ 项目完成分析

### Epic-5: Role Permission
- ✅ 用户注册与登录
- ✅ JWT认证
- ✅ 5种角色权限分级
- ✅ 信息穿透鉴权（RoleAuthFilter JWT+Session 校验已启用，SecurityConfig anyRequest().authenticated()）

### Epic-6: UI and Theme
- ✅ 底部导航5项（导图/视图/日志/AI地图/我的，bottom_nav_menu.xml）
- ✅ 专注模式/番茄钟（独立 FocusActivity，从"我的"页入口，不在底部导航）
- ✅ 生日祝福

## Non-goals
- Oracle数据库切换（已预留profile，ojdbc11已引入）
- 单元测试编写（待Sprint 3）
- 飞书多维表格项目管理看板对接

## Acceptance Criteria
- Android端可编译运行
- 后端可启动并响应API请求
- Web管理端可启动并显示登录页
- 所有核心功能可通过界面操作
