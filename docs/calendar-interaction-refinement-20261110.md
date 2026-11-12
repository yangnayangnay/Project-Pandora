# 日历与 AI 交互第二轮调整（2026-11-10）

本轮在 `D:\panduola` 实施，保留已有改动，不更改账号或服务器配置。

## 实际功能与界面修改

- 公司月历下方连续白色任务区增加轻分界线；首页看板详情标签栏下方也补上主题分界线。
- 保留图示日期详情小窗。普通及展开月历点击有事项/任务的日期区域都能触发当天内容入口，不再只有精确点中文字条才触发。多任务显示原有小窗，单任务仍直接打开详情；事项不论一项、多项都显示小窗。
- 普通月历中，特殊节日或节气替换农历标题；展开月历中，农历显示在原位置，特殊日期在下方单独色块中展示，并与任务条分开。
- 事项及任务月历条目改回单行省略，默认高度 19dp，随字号增长；仍保留四项任务及溢出入口，独立预留特殊日期空间。默认每周行高由上一轮约 245dp 缩短到约 182dp。
- 事项与无标签任务统一使用棕灰色；夜间也保留足够亮度的底色配黑字，保证可读性。有标签任务继续按对应标签着色；公司周/日视图也使用统一的无标签底色。
- 日期选择器顶部预览去掉色块，三个滚轴共用一整条柔和圆角选中底色，去掉六条原生分隔线。“显示农历 / 阳历”改为“显示农历”。
- 农历开关实际切换月、日滚轴：月份显示“正月、二月……”及真实闰月，日期显示“初一、初二……”。使用 Android ICU 换算实际日期，切换模式保留所选日期；底层接口仍保存公历日期。
- 数据收集页增加标题说明、数据概览、关键词分析入口、三步引导和使用示例，使用圆角卡片、分界线、轻阴影和统一线性图标区分层次；数据加载失败显示“—”和说明，不误显示为零。
- MBTI 入口卡片改为与相邻卡片一致的线性图标，移除页面右下角人物署名；素材来源仍保留在项目文档。
- 签筒本身可点击。起卦时显示虚化背景，签筒保持比例移到屏幕中心并放大，再摇签、落签、浮现卦象。支持取消并清理浮层；离开页面清理动画；系统关闭动画时直接显示结果。既有每日一次和结果存储规则保留。

## “点不动”排查及修正范围

用户确认入口和弹窗内操作均曾无响应。本轮用当前已安装版本成功打开过首页日期选择窗；读取的崩溃和无响应日志中未捕获对应故障。这不足以否认用户反馈，也不足以确定完整根因。

已确认源码中的一个等待问题：共用网络读取超时为 120 秒，事项保存期间会禁用表单，因此连接或服务器迟迟不回应时，会长时间表现为按钮不可点。本轮仅对事项读取、保存和删除设置 20 秒等待上限，不改变全局 AI 等请求的超时设置。超时后恢复表单、保留内容，并明确提示先刷新核对服务器结果，防止误以为一定没有保存而重复提交。

同时清理日期窗关闭后的引用、重新打开前关闭遗留日期窗、重新打开事项表单时复位写入状态，并让滚轴手势避免被外层滚动容器截走。上述措施处理具体可见代码风险，不能当作入口偶发失效已经彻底验证的证据。

用户最终选择“只保留源码修改，暂不安装”。因此新版未打包安装，入口、确认、保存及动画的设备回归仍未完成。该偶发点击故障的完整根因尚未稳定复现，不标记为已彻底解决。

## 主要文件

- `app/src/main/java/com/example/project_pandora/ui/widget/PhoneMonthView.java`
- `app/src/main/java/com/example/project_pandora/ui/widget/CalendarCellMetrics.java`
- `app/src/main/java/com/example/project_pandora/ui/widget/YearMonthDayPicker.java`
- `app/src/main/java/com/example/project_pandora/ui/widget/LunarDateWheelModel.java`（新增）
- `app/src/main/java/com/example/project_pandora/ui/widget/QuickDatePickerDialog.java`
- `app/src/main/java/com/example/project_pandora/ui/widget/TaskScheduleView.java`
- `app/src/main/java/com/example/project_pandora/ui/home/DailyAgendaController.java`
- `app/src/main/java/com/example/project_pandora/ui/home/DailyItemEditor.java`
- `app/src/main/java/com/example/project_pandora/ui/home/DailyItemsMonthView.java`
- `app/src/main/java/com/example/project_pandora/ui/task/TaskTagColors.java`
- `app/src/main/java/com/example/project_pandora/ui/view/ViewFragment.java`
- `app/src/main/java/com/example/project_pandora/ui/aimap/DataCollectActivity.java`
- `app/src/main/java/com/example/project_pandora/ui/aimap/DivinationActivity.java`
- `app/src/main/java/com/example/project_pandora/ui/aimap/DivinationRitualOverlay.java`（新增）
- `app/src/main/res/layout/activity_data_collect.xml`、`activity_ai_analysis.xml`、`fragment_view.xml`、`activity_home_boards.xml`
- `app/src/main/res/drawable/bg_data_stat.xml`、`bg_task_list_surface.xml`、`ic_personality.xml`（新增）
- `app/src/main/res/values/calendar_refinement.xml`、`values-night/calendar_refinement.xml`（新增）

## 数据与 API

本轮没有数据库结构变更、业务 API 新增或服务端部署；沿用既有任务、事项和 AI 接口及权限。农历模式属于日期输入和换算，不新增农历存储字段。账号、密码、在线数据保持原状。

## 清理的旧逻辑

- 月历条目的两行折行及较高行距。
- 展开月历中节日替代农历标题的显示方式。
- 无标签任务按标题散列分配随机颜色的日历显示方式。
- 日期选择器顶部带色预览、三块分离的选中条及原生分隔线。
- 农历开关只修改预览文本的逻辑。
- 原地签筒动画入口以及 MBTI 卡片内不一致的人物配图和页面人物署名。

## 验证与未完成部分

- 已先检查 Gradle 任务图；验证仅包含 Android 资源和 Java 编译、仪器测试源码编译及 JVM 测试，不包含 APK 生成和安装。
- Android 主源码/资源和仪器测试源码编译通过；190 项 JVM 测试通过，0 失败、0 错误。
- 更新日历尺寸与点击路由测试源码；新增农历新年、闰月换算和模式切换保留日期的仪器测试源码。仪器测试未在设备运行，不能报告为测试通过。
- 173 个资源 XML 解析通过，差异空白检查通过。
- 仍需验收：新版日期滚轴的实际滚动和触摸、事项保存成功及超时恢复、节日色块和夜间视觉、居中起卦动画及取消清理。
- 无缺失 PDF 或其他指定样式资料。本轮未增加新的业务功能。
