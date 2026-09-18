# Task 0 - 详细执行文档

> **创建日期**: 2026-09-11
> **对应任务方案**: task0.md

## 详细执行步骤

### 1. 文档先行机制文件创建

已创建以下文件：
- `docx/docx_generate/需求分析.txt`：包含项目背景、25个User Story摘要、6个Epic分组、5种角色定义、技术栈选型理由
- `docx/docx_generate/需求改动.txt`：变更记录模板（日期+变更项+影响范围+提出人）
- `docx/docx_generate/代码改动.txt`：变更记录模板（日期+文件路径+改动类型+关联需求+作者）
- `docx/docx_generate/特色小巧思.txt`：特色实现细节记录（月相主题/梅花易数/晨星冒烟测试等）

### 2. README.md 创建

已在项目根目录创建 README.md，包含：
- 项目简介
- 技术栈说明
- 三端目录结构
- 启动方式（run-server.bat / Android Studio / npm run dev）
- GitHub 仓库地址
- Sprint 规划
- 文档维护约定

### 3. 三端工程骨架初始化

#### 3.1 Android 工程重构
- 将 `app/src/main/java/com/example/project_pandora/` 重构为六大包结构：
  - `core/`：核心基础架构（DI/网络/数据库/日志/崩溃/主题/占卜/冒烟测试）
  - `data/`：数据源层（远程/本地/偏好/同步）
  - `repository/`：Repository层
  - `viewmodel/`：ViewModel层
  - `ui/`：UI层
  - `model/`：数据模型

#### 3.2 后端工程创建
- 在项目根目录创建 `pandora-backend/`
- 按 11 个模块包结构创建：auth/user/task/log/view/ai/permission/theme/audit/sync/common

#### 3.3 Web 工程创建
- 在项目根目录创建 `pandora-web/`
- 初始化目录结构：api/views/components/stores/router/utils

### 4. 数据库设计

设计 11 张核心表（符合 4NF）：
1. users - 用户主表
2. departments - 部门表
3. teams - 团队表
4. tasks - 任务表
5. work_logs - 工作日志表
6. top10_works - 十大重要工作表
7. mbti_reports - MBTI报告表
8. ai_analyses - AI分析记录表
9. audit_logs - 审计日志表
10. invitation_requests - 信息邀约请示表
11. audit_rules + audit_rule_approvers - 分层审核规则表（子表拆分消除多值依赖）

### 5. 后端基础架构

- Spring Boot 3 + Java 17 + MySQL 8.0 + Redis 7 + JWT
- 通用组件：ApiResponse/GlobalExceptionHandler/PageResult/BusinessException
- 认证鉴权：JwtUtil/AuthController/RoleAuthFilter
- 审计切面：AuditAspect/AuditService
- 启动脚本：run-server.bat

### 6. Android 基础架构

- Gradle 依赖配置：Retrofit/Room/RxJava/Hilt/MPAndroidChart/Glide/Security-Crypto
- 核心基础架构：NetworkModule/DatabaseModule/PreferenceModule/ApiResponse/TokenInterceptor
- ThemeManager：8种月相主题管理
- MoonPhaseCalculator：月相计算
- DivinationEngine：梅花易数占卜
- SmokeTestEngine：晨星冒烟测试
- PandoraLogger：日志管理（敏感信息过滤/定量清除）
- CrashHandler：全局异常处理
- SyncEngine：离线同步引擎
- MainActivity 改造：BottomNavigationView + NavHostFragment

### 7. Web 管理端基础架构

- Vue 3 + Vite + Element Plus + Pinia + ECharts
- Axios 封装：统一Token Header/业务码处理/401跳转
- 路由配置：登录页/看板/任务管理/人员权限/AI分析/系统配置
- Pinia store：userStore/appStore
- 通用组件：Layout/PageHeader/EmptyState/ConfirmDialog

### 8. 业务功能实现

按 Epic 垂直切割，每个 Epic 包含后端+Android+Web三端任务：
- Epic-1：任务管理闭环（US-001 ~ US-006）
- Epic-2：日志管理闭环（US-007 ~ US-011）
- Epic-3：视图展示闭环（US-012 ~ US-014）
- Epic-4：AI智能分析闭环（US-015 ~ US-019）
- Epic-5：角色权限闭环（US-020 ~ US-022）
- Epic-6：界面与主题闭环（US-023 ~ US-025）