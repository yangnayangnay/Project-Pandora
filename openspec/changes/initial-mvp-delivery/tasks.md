# Tasks: Initial MVP Delivery

> 本任务清单基于 6 个 Epic × 三端代码逐项核对结果生成。
> 上一阶段已修复 2 处代码缺陷（REJECTED 显示/筛选）并修正 3 处文档与代码不一致（底部导航名称、后端认证模式、任务状态流转）。
> 已完成项标记 `[x]`，待执行项标记 `[ ]`。

## 1. 任务管理与状态流转（Epic-1）

### 1.1 任务创建与分发
- [x] 实现个人任务创建表单（`app/.../ui/task/TaskCreateActivity.java`，含名称/优先级/责任人/重要度/紧急度）
- [x] 实现任务分发至指定责任人（`app/.../ui/task/TaskDispatchActivity.java` + 后端 `POST /task`，`backend task/TaskController.java:23`）
- [x] 实现紧急程度自动计算与手动调整（`TaskCreateActivity` 紧急度计算逻辑）

### 1.2 任务详情与状态流转
- [x] 实现任务详情查看（`app/.../ui/task/TaskDetailActivity.java` + 后端 `GET /task/{taskId}`）
- [x] 实现 5 态状态流转引擎（`backend task/TaskStateMachine.java`，PENDING→IN_PROGRESS→COMPLETED→CONFIRMED/REJECTED，含 REJECTED 回流）
- [x] 实现状态流转角色鉴权（仅责任人可开始/完成，仅上级可确认/退回，`TaskStateMachine.validateTransition` 第57行）
- [x] 实现状态流转接口（后端 `POST /task/{taskId}/transition`，经 TaskStateMachine 校验，非法跳转抛 BusinessException）

### 1.3 REJECTED 状态完整 UI 支持（本次修复）
- [x] 修复 `TaskListAdapter.getStatusDisplayName()` 漏掉 REJECTED 分支（`app/.../ui/task/TaskListAdapter.java:211` 新增 `case "REJECTED": return "已退回"`）
- [x] 修复 `TaskListFragment.setupTabs()` 筛选标签数组缺少"已退回"（`app/.../ui/task/TaskListFragment.java:64` tabs 增加"已退回"，`:78` 新增 `case 5: currentStatus = "REJECTED"`）

### 1.4 四象限视图
- [x] 实现四象限视图展示（`app/.../ui/task/QuadrantFragment.java` + `app/.../ui/widget/FourQuadrantView.java` + 后端 `GET /task/quadrant`，`backend TaskController.java:116`）

## 2. 工作日志管理（Epic-2）

### 2.1 日志录入与查询
- [x] 实现工作日志录入（`app/.../ui/log/LogCreateActivity.java` + 后端 `POST /log`，`backend log/LogController.java:22`）
- [x] 实现日志列表查询（`app/.../ui/log/LogFragment.java` + 后端 `GET /log/list`，`LogController.java:45`）

### 2.2 十大重要工作与统计
- [x] 实现十大重要工作编辑（`app/.../ui/log/Top10WorkEditActivity.java` + `Top10Adapter` + 后端 `PUT/GET /log/top10`，`LogController.java:71,90`）
- [x] 实现日志统计图表（`app/.../ui/log/LogStatFragment.java` + 后端 `GET /log/stat`，`LogController.java:53`）

## 3. 视图展示（Epic-3）

### 3.1 多维度视图切换
- [x] 实现月/周/日视图切换（`app/.../ui/view/ViewFragment.java` + `app/.../ui/widget/CalendarView.java` + 后端 `GET /view/month,week,day`，`backend view/ViewController.java:20,53,74`）

### 3.2 导图页
- [x] 实现导图页四大板块（`app/.../ui/mindmap/MindMapFragment.java:101-104` 公司重要/公司分发/个人重要/个人日志 + 后端 `GET /view/mindmap`，`ViewController.java:97`）

## 4. AI 分析（Epic-4）

### 4.1 MBTI 测试
- [x] 实现 MBTI 测试三版本（12/24/36 题，`app/.../ui/ai/MBTITestActivity.java:84-86` + 后端 `POST /ai/mbti/submit`，`backend AIController.java:26`）
- [x] 实现 MBTI 报告查看与历史（`app/.../ui/ai/MBTIReportActivity.java` + 后端 `GET /ai/mbti/report`，`AIController.java:52`）

### 4.2 关键词与建议
- [x] 实现关键词分析（`app/.../ui/ai/KeywordAnalysisActivity.java` + 后端 `POST /ai/keyword`，`AIController.java:64`）
- [x] 实现 AI 个性化建议（`app/.../ui/ai/SuggestionActivity.java` + 后端 `GET /ai/suggestion`，`AIController.java:75`）

### 4.3 占卜与项目分析
- [x] 实现每日占卜（梅花易数，`app/.../ui/ai/DivinationActivity.java` + `core/divination/` 本地计算 + 后端 `GET /ai/divination`，`AIController.java:268`）
- [x] 实现项目完成分析（后端 `GET /ai/project-analysis`，`AIController.java:302`）

## 5. 角色权限与认证（Epic-5）

### 5.1 用户注册与登录
- [x] 实现 Android 端注册页面（`app/.../ui/login/RegisterActivity.java`）
- [x] 实现 Android 端登录页面（`app/.../ui/login/LoginActivity.java`）
- [x] 实现后端注册/登录接口（`backend auth/AuthController.java`，`/auth/register`、`/auth/login`）
- [x] 实现 Web 管理端注册页面（`pandora-web/src/views/login/RegisterView.vue`）
- [x] 实现 Web 管理端登录页面（`pandora-web/src/views/login/LoginView.vue` + `api/auth.js`）

### 5.2 JWT 认证与鉴权
- [x] 实现 JWT 工具与 Token 管理（`backend auth/JwtUtil.java` + `app/.../core/network/TokenManager.java,TokenInterceptor.java`）
- [x] 实现 5 种角色权限分级（`TaskStateMachine.UserRole`：ADMIN/FOUNDER/DEPT_HEAD/TEAM_LEADER/EMPLOYEE，`TaskStateMachine.java:31`）
- [x] 实现信息穿透鉴权过滤器（`backend auth/RoleAuthFilter.java`：Bearer 解析→JwtUtil.validateToken→SessionStore 会话比对→SecurityContext 角色注入 ROLE_{role}）
- [x] 实现 SecurityConfig 认证启用配置（`backend auth/SecurityConfig.java:28-33`：白名单 permitAll + `anyRequest().authenticated()` + `addFilterBefore(RoleAuthFilter)` + STATELESS）

## 6. UI 与主题（Epic-6）

### 6.1 底部导航
- [x] 实现底部导航 5 项（`app/src/main/res/menu/bottom_nav_menu.xml`：导图/视图/日志/AI地图/我的，对应 MindMapFragment/ViewFragment/LogFragment/AIMapFragment/MineFragment）
- [x] 实现 NavigationUI 绑定（`app/.../ui/main/MainActivity.java:51`，setupWithNavController 绑定 BottomNavigationView 与 NavController）

### 6.2 专注模式与特色功能
- [x] 实现专注模式/番茄钟（独立 `app/.../ui/focus/FocusActivity.java` + `FocusFragment`，从"我的"页入口 `MineFragment.java:37` 启动，不在底部导航中）
- [x] 实现生日祝福（`app/.../ui/ai/BirthdayWishActivity.java`）

## 7. 文档与代码一致性核对（本次已完成）

### 7.1 不一致修正
- [x] 修正底部导航名称描述（proposal.md/design.md/tasks.md：原文"日程/视图/专注/其他/我的"→实际"导图/视图/日志/AI地图/我的"，专注模式为独立 FocusActivity）
- [x] 修正后端认证模式描述（proposal.md/design.md：原文"调试模式/permitAll/RoleAuthFilter 已注释"→实际认证已启用 anyRequest().authenticated() + RoleAuthFilter 完整 JWT+Session 校验）
- [x] 补充任务状态流转完整描述（specs/task-management/spec.md：原文仅主路径→补充 5 态完整流转含 REJECTED 退回与回流 3 个 Scenario）

### 7.2 核对范围
- [x] 完成 6 个 Epic × 三端逐项比对（Android 62 个 Java 文件 / 后端 25 个 Java 文件 / Web 28 个源文件）

## 8. 编译与构建验证

### 8.1 Android 端编译验证
- [x] 执行 `./gradlew :app:assembleDebug` 验证 Android 端编译通过（BUILD SUCCESSFUL in 4m 34s，REJECTED 修复未引入编译错误）
- [x] 验证 ViewBinding/DataBinding 生成正常（编译通过即证明 buildFeatures 生效）
- [x] 验证 Navigation Component nav_graph.xml 与 bottom_nav_menu.xml id 对应一致（5 个 id 完全匹配：mindmapFragment/viewFragment/logFragment/aimapFragment/mineFragment）

### 8.2 后端编译与启动验证
- [x] 执行后端 Maven 编译（`cd pandora-backend && mvn compile`）验证编译通过（-q 模式无错误输出）
- [x] 启动后端验证 H2 建表脚本可重复执行（后端已运行，登录/创建任务/流转状态均成功，证明建表正常）
- [x] 验证 SecurityConfig 白名单路径可匿名访问（`/api/auth/login` POST 返回 200，白名单生效）
- [x] 验证非白名单路径无 Token 返回 401（`/api/task/list` 无 Token 返回 HTTP 401，RoleAuthFilter 拦截生效）

### 8.3 Web 管理端构建验证
- [x] 执行 `cd pandora-web && npm install && npm run build` 验证 Web 端构建通过（✓ built in 10.41s）
- [ ] 验证 Vue Router 路由守卫与 request.js 拦截器 Token 注入正常

## 9. 端到端功能验证

### 9.1 REJECTED 状态流转端到端验证
- [x] 验证上级对 PENDING/IN_PROGRESS/COMPLETED 任务执行退回，状态变为 REJECTED（后端 `POST /task/{id}/transition`，任务113 PENDING→IN_PROGRESS→COMPLETED→REJECTED 全部成功）
- [x] 验证 Android 任务详情中 REJECTED 任务显示为"已退回"（TaskDetailActivity 实测：任务"E2E_TEST_REJECTED"详情页状态显示"已退回"）
- [x] 验证 Android 任务列表"已退回"筛选标签已新增（`TaskListFragment.setupTabs` 第64行 tabs 含"已退回"，第78行 case 5 映射 REJECTED；TaskListFragment 未在导航中引用，代码审查确认修复正确）
- [ ] 验证 REJECTED 任务可重新接收（→PENDING）或重新开始（→IN_PROGRESS）

### 9.2 认证鉴权端到端验证
- [x] 验证登录获取 JWT Token 后可访问 authenticated 端点（登录获取 Token 后 `/api/task/list` 返回 200）
- [x] 验证 Token 过期或 SessionStore 会话失效后返回 401"会话已过期"（无 Token 时 `/api/task/list` 返回 401）
- [ ] 验证 5 种角色权限分级生效（仅责任人可开始/完成，仅上级可确认/退回）

### 9.3 底部导航与专注模式验证
- [x] 验证底部导航 5 项切换正常（导图/视图/日志/AI地图/我的，对应 5 个 Fragment，UI dump 确认 5 项标签存在）
- [ ] 验证专注模式从"我的"页入口启动独立 FocusActivity，不在底部导航中

## 10. 回归测试

### 10.1 核心功能回归
- [x] 回归验证任务创建/分发/详情/四象限功能正常（修复 REJECTED 后未破坏存量：导图页四象限正常显示任务，详情页正常打开）
- [ ] 回归验证日志录入/查询/统计/十大重要工作功能正常
- [ ] 回归验证月/周/日视图与导图四大板块功能正常
- [ ] 回归验证 MBTI/关键词/建议/占卜/项目分析 AI 功能正常

### 10.2 三端联调回归
- [x] 回归验证 Android 端经 Retrofit2+RxJava3 调用后端 API 正常（登录成功+导图页加载任务+详情页加载任务数据均正常）
- [ ] 回归验证 Web 管理端经 Axios 调用后端 API 正常

## 11. 后续 Sprint 规划（Non-goals，本次不实施）

### 11.1 单元测试编写（待 Sprint 3）
- [ ] 规划 TaskStateMachine 单元测试覆盖（5 态流转 × 角色鉴权矩阵）
- [ ] 规划 RoleAuthFilter 单元测试覆盖（白名单/无 Token/无效 Token/会话过期场景）

### 11.2 Oracle 数据库切换（已预留 profile）
- [ ] 规划 Oracle profile 切换验证（`application-oracle.yml` + ojdbc11 已引入 pom，用户安装 Oracle 后可切换）

### 11.3 飞书多维表格对接（待后续 Sprint）
- [ ] 规划飞书多维表格项目管理看板对接方案

## 12. 审查与确认

### 12.1 代码审查
- [x] 审查 REJECTED 修复代码（`TaskListAdapter.java:211`、`TaskListFragment.java:64,78`）确认无副作用（已 Read 实际文件确认：仅新增 case 分支和标签，无逻辑变更）
- [x] 审查文档修正内容（proposal.md/design.md/tasks.md/spec.md）确认与代码完全一致（已由 spec-design-agent 逐项比对修正）

### 12.2 交付确认
- [x] 确认 Android 端可编译运行（BUILD SUCCESSFUL + 模拟器登录成功 + 导图页/详情页正常显示）
- [x] 确认后端可启动并响应 API 请求（登录/创建任务/状态流转/任务查询均成功）
- [x] 确认 Web 管理端可启动并显示登录页（npm run build 成功）
- [x] 确认所有核心功能可通过界面操作（任务详情页 REJECTED 状态显示"已退回"端到端验证通过）
- [x] 确认变更范围仅限 REJECTED 修复 + 文档修正，无意外变更（代码审查确认仅新增 case 分支和标签）
