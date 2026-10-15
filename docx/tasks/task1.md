# 任务1：全面实机测试与代码修复

## 任务目标
对 Project Pandora 项目进行全面实机测试，验证每个界面设计合理不重叠、所有内容完全符合要求、不存在设计了但未实现的界面、应用与服务端联系正常。根据测试结果修改代码，完成后生成测试报告。

## 测试范围
共25个独立UI界面需要测试：
- 16个Activity + 9个Fragment
- 5个底部导航tab
- 13个RecyclerView列表
- 2个自定义自绘视图(CalendarView/FourQuadrantView)

## 执行步骤

### 阶段一：环境准备
1. 启动后端Spring Boot服务器
2. 构建Android APK并安装到Pixel 10模拟器(emulator-5554)
3. 验证应用启动无崩溃

### 阶段二：逐界面测试
按以下顺序测试每个界面：
1. 登录界面(LoginActivity) - 4个角色账号验证
2. 注册界面(RegisterActivity) - 5种角色注册
3. 首页思维导图(MindMapFragment/QuadrantFragment) - 四大板块对齐
4. 任务表格视图(ViewFragment) - 月/周/日视图、六列格式
5. 任务详情(TaskDetailActivity) - **P0:紧急程度4级→2档**
6. 任务创建(TaskCreateActivity) - 优先级Spinner
7. 任务分发(TaskDispatchActivity) - 下属列表选择
8. 日志列表(LogFragment) - SwipeRefresh+列表
9. 日志创建/编辑(LogCreateActivity) - 日期/完成度/模板
10. Top10工作编辑(Top10WorkEditActivity)
11. AI入口(AIMapFragment) - 4个功能按钮
12. 关键词分析(KeywordAnalysisActivity)
13. AI建议(SuggestionActivity)
14. MBTI测试(MBTITestActivity) + 报告(MBTIReportActivity)
15. 占卜(DivinationActivity)
16. 我的(MineFragment) - 3个功能入口
17. 个人资料(ProfileActivity)
18. 专注/番茄钟(FocusActivity/FocusFragment)
19. 生日愿望(BirthdayWishActivity)

### 阶段三：问题修复
- 修复P0问题：TaskDetailActivity紧急程度从4级RadioGroup改为2档手动设置
- 修复其他测试发现的问题
- 确保界面不重叠、布局合理

### 阶段四：回归测试
- 重新构建安装APK
- 验证修复后功能正常

### 阶段五：生成测试报告
- 记录所有测试结果
- 生成测试报告文档

## 验证标准
- 每个界面可正常打开无崩溃
- 界面元素不重叠、布局合理
- 应用-服务端通信正常(真实客户端发起请求)
- 数据落库可复查
- 闪退时拉取logcat日志定位