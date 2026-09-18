# Tasks: Initial MVP Delivery

## Epic-1: Task Management
- [x] 实现任务创建表单（TaskCreateActivity）
- [x] 实现任务分发（TaskDispatchActivity）
- [x] 实现任务详情与状态流转（TaskDetailActivity）
- [x] 实现四象限视图（TaskController quadrant端点）
- [x] 实现紧急程度自动计算与手动调整

## Epic-2: Work Log Management
- [x] 实现日志录入（LogCreateActivity）
- [x] 实现日志列表查询（LogFragment）
- [x] 实现十大重要工作编辑（Top10WorkEditActivity）
- [x] 实现日志统计图表（LogStatFragment）
- [x] 实现晨星冒烟测试（SmokeTestEngine）

## Epic-3: View Display
- [x] 实现月/周/日视图切换（ViewFragment + TabLayout）
- [x] 实现导图页四大板块

## Epic-4: AI Analysis
- [x] 实现MBTI测试三版本（MBTITestActivity）
- [x] 实现MBTI报告查看与历史（MBTIReportActivity）
- [x] 实现关键词分析（KeywordAnalysisActivity）
- [x] 实现AI建议（SuggestionActivity）
- [x] 实现每日占卜（DivinationActivity + DivinationEngine）
- [x] 实现项目完成分析（AIController /ai/project-analysis）

## Epic-5: Role Permission
- [x] 实现用户注册接口（AuthController /auth/register）
- [x] 实现用户登录接口（AuthController /auth/login）
- [x] 实现JWT认证（JwtUtil + TokenManager）
- [x] 实现5种角色权限分级代码（RoleAuthFilter + SecurityConfig）
- [x] Web端注册页面（RegisterView.vue）
- [x] Android端注册页面（RegisterActivity）

## Epic-6: UI and Theme
- [x] 实现月相主题自动切换（ThemeManager + MoonPhaseCalculator）
- [x] 实现手动主题选择（ThemeSettingActivity）
- [x] 实现底部导航5项（bottom_nav_menu.xml）
- [x] 实现专注模式/番茄钟（FocusFragment）
- [x] 实现生日祝福（BirthdayWishActivity）
- [x] "统计"改名为"其他"

## Bug Fixes
- [x] 修复 fragment_view.xml 视图重叠（TabLayout与按钮重叠）
- [x] 修复 activity_task_create.xml 缺少toolbar和返回按钮
- [x] 修复资源链接失败（abc_vector_back → ?attr/homeAsUpIndicator）