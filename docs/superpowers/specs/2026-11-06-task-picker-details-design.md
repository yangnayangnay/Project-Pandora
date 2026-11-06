# 创建任务日期选择与任务明细精简

用户已指定以公司月视图左上角日期选择为统一样式，并授权全部由 Codex 直接完成修改和验证。

1. 创建任务的开始、截止日期复用公司页现有 `QuickDatePickerDialog`，支持年月日滚轮、直接输入与回到今天。日期确认后继续使用现有时分选择；两个弹窗均确认后才更新表单及草稿。取消任一步不改变原值。保留原有时间回显、草稿恢复、时间范围校验、防重复打开和页面销毁时关闭弹窗的处理。
2. 公司页“任务明细”移除“进度说明”表头、对应列宽及数据单元格，保持其余列顺序和横向滚动对齐。此项不涉及服务端数据结构变更。
3. 本次只修改和验证，不部署、不生成或安装 APK。执行前检查 Gradle 任务图，运行已有 Java/资源编译和 JVM 测试，并对比已有 APK 的路径、大小与修改时间。设备界面验收与源码检查分别报告。

## 验证结果

2026-11-06：Gradle 任务图检查通过，未包含 APK 打包或安装任务；Java/资源编译、JVM 测试源码编译、仪器测试源码编译均通过。22 个测试套件共 174 项 JVM 测试全部通过，无失败、错误或跳过，其中已有日期边界和任务时间回显/草稿恢复测试分别为 9 项、11 项。表头和数据行使用同一列宽定义，删除后均为 13 列；`git diff --check` 通过。已有 APK 路径、大小和修改时间变化数为 0。未部署、打包、安装或运行设备界面验收。

验证记录：`.build-temp/task-picker-details-android-plan.log`、`.build-temp/task-picker-details-android.log`、`.build-temp/task-picker-details-tests.json`、`.build-temp/task-picker-details-apk-before.json` 与 `.build-temp/task-picker-details-apk-after.json`。
