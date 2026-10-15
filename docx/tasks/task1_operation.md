# task1_operation.md - 全面实机测试详细执行文档

## 执行时间: 2026-10-05 14:13 ~ 14:35

## 步骤1: 环境准备
1. 读取baicRequirements.txt确认测试规范
2. 检查git status, 确认有未提交变更(月相/晨星/邮件删除)
3. 探索项目UI结构: 16个Activity + 9个Fragment + 32个布局XML
4. 创建task1.md测试任务执行方案

## 步骤2: 启动后端
1. 检查端口8080 → 未占用
2. 启动Spring Boot: `mvn spring-boot:run`
3. 等待15秒, 验证端口8080 LISTENING
4. 后端启动成功, H2数据库连接正常

## 步骤3: 设备连接
1. ADB devices → 发现实机 GMA68XT8ROAMJZW4
2. 获取PC LAN IP: 10.60.64.207
3. 设备IP: 10.61.139.50 (不同子网, ping失败)
4. 设置ADB反向端口转发: `adb reverse tcp:8080 tcp:8080`
5. 修改NetworkClient.java: 10.0.2.2 → 127.0.0.1
6. 修改LoginActivity.java: 服务器检测URL 10.0.2.2 → 127.0.0.1

## 步骤4: 构建安装
1. gradlew assembleDebug → BUILD SUCCESSFUL
2. adb install -r app-debug.apk → Success
3. 启动LoginActivity → 服务器状态"服务器已连接 ✓"

## 步骤5: 登录测试
1. 输入yjl_founder/123465 → 登录成功 → MainActivity
2. 清除数据后输入yjl_employee/123465 → 登录成功 → MainActivity

## 步骤6: 界面测试 (13个界面)
### 6.1 QuadrantFragment (首页四象限)
- 2x2网格, 4个象限卡片, 间距24px, 无重叠
- yjl_employee加载5个任务, 分布在Q1/Q3/Q4

### 6.2 ViewFragment (日历视图)
- 月/周/日三tab, 月份导航, CalendarView正常

### 6.3 LogFragment (日志列表)
- 空状态提示, 自动生成开关, FAB按钮

### 6.4 AIMapFragment (AI地图)
- 3个功能入口(智能建议/关键词分析/职业性格)

### 6.5 MineFragment (我的)
- 3个功能入口(用户中心/专注模式/生日祝福)

### 6.6 TaskDispatchActivity (任务分发)
- 10个下属用户, 创建TestTask001成功(ID:111)

### 6.7 下属任务对话框
- 显示27条任务, 含新建TestTask001

### 6.8 TaskDetailActivity (任务详情) - P0修复
- 发现问题: 紧急程度4级RadioGroup(最高/高/中/低) + 自动计算
- 发现问题: 日期显示原始ISO格式
- 修复: 改为2级(紧急/不紧急) + 手动设置
- 修复: 添加formatDateTime()方法
- 验证: 2个RadioButton, 日期"2026-09-25 10:00", 切换"不紧急"成功

### 6.9 SuggestionActivity (AI建议)
- 用户信息/任务统计/工作建议/职业建议/发展计划

### 6.10 ProfileActivity (个人资料)
- 用户名yjl_employee, 角色EMPLOYEE

### 6.11 FocusActivity (专注模式)
- 25:00计时器, 开始/暂停/重置按钮

## 步骤7: 修复验证
1. 修改activity_task_detail.xml: 4个RadioButton → 2个
2. 修改TaskDetailActivity.java: setupUrgencyAdjustment()重写
3. 清理未使用imports
4. gradlew assembleDebug → BUILD SUCCESSFUL
5. 重新安装APK, 验证修复生效

## 步骤8: 生成报告
1. 创建测试报告.md (13个界面测试结果)
2. 转换为测试报告.docx
3. 更新代码改动.txt