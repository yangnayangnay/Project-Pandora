# UI and Theme

## Capability
底部导航栏5项。专注模式（番茄钟）。生日祝福功能。

## Requirements
- 底部导航5项：首页/导图/视图/日志/AI地图/我的
- 专注模式（番茄钟）移入"我的"页面
- 竖屏锁定
- 描述文本通过strings.xml统一管理
- 专注模式：25分钟专注+5分钟休息循环
- 生日祝福：设置生日，显示祝福和倒计时

## Scenarios

### Focus mode
GIVEN 用户进入专注模式
WHEN 点击开始
THEN 启动25分钟专注计时，完成后进入5分钟休息
