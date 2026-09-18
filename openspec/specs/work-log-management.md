# Work Log Management

## Capability
工作日志的录入、编辑、删除、查询、统计和模板管理。支持十大重要工作编辑与展示。包含日志统计图表（完成率表格/重要占比饼图/分析竖图）。晨星冒烟测试每天首次进入日志页执行轻量级完整性校验。日志模板可保存和调用。

## Requirements
- 日志须包含：工作事项、完成状态、时间花费、日志日期
- 日志列表按时间倒序展示，支持下拉刷新
- 日志支持编辑（PUT /log/{id}）和删除（DELETE /log/{id}）
- 日志模板可保存为本地模板（SharedPreferences），支持调用和删除
- 十大重要工作支持编辑和持久化保存
- 晨星冒烟测试在当天首次进入日志页时执行
- 日志统计图表包含三类：完成率表格、重要占比饼图、分析竖图

## Scenarios

### Create work log
GIVEN 员工已登录
WHEN 填写工作事项、完成状态和日期并提交
THEN 日志保存成功并出现在个人日志列表

### View log list
GIVEN 员工有历史日志
WHEN 进入日志列表页
THEN 日志按时间倒序展示

### Edit top10 important works
GIVEN 员工已登录
WHEN 编辑个人10大重要工作并保存
THEN 保存成功并在导图页展示

### Morning star smoke test
GIVEN 当天首次进入日志页
WHEN 页面加载
THEN 执行晨星冒烟测试，校验通过后正常展示日志内容