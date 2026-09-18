# View Display

## Capability
月视图、周视图、日视图切换展示任务和日程。导图页四大板块（公司10大重要/公司派发10大/个人10大重要/个人日志）。信息数据地图可视化。

## Requirements
- 支持月/周/日三种视图维度切换
- 导图页同时展示四大板块内容
- 视图借鉴番茄计划的月视图、周视图功能
- 日历视图中任务按日期展示

## Scenarios

### Switch view dimension
GIVEN 用户在视图页面
WHEN 点击月/周/日视图标签
THEN 展示对应时间维度的任务和日程

### View dashboard with four sections
GIVEN 用户进入导图页
WHEN 页面加载
THEN 同时展示公司10大重要、公司派发10大、个人10大重要、个人日志四个板块