# 任务编辑、日历点击与夜间可读性修正（2026-11-10）

## 实际修改

1. 任务创建/修改页：点击输入框之外的控件时结束文本输入、清除编辑焦点并收起键盘；将焦点交给滚动区域之外的页面根容器。负责人选择和时间选择入口也执行相同处理，避免返回时因原输入框仍有焦点而自动滚回。
2. 首页事项和公司任务月历：点击日期数字只选中日期，不再因为该日期有任务而打开小窗或详情。展开模式中的任务条、更多项提示，以及普通模式中的任务圆点仍可打开当日内容。公司仍保留单任务直接进入详情、多任务打开小窗；事项仍通过小窗查看完整内容。空白处也不再打开详情，日期区域不再作为拖动某项事项的起点。
3. 首页选日期后更新对应事项列表，移除原先点击空日期后自动滚向列表的行为。
4. 修改任务：顶部返回与系统返回统一检查实际修改；存在未提交的改动时显示“当前修改未保存，是否退出？”，提供“继续编辑”和“退出”。对名称、正文、开始/截止时间、紧急程度、标签与负责人进行比较；修改后恢复原值不再提示，负责人顺序变化不算修改，主标签顺序变化算修改。原始状态可随页面重建恢复。
5. 创建/修改草稿隔离：只有创建页运行自动保存，草稿内容增加 `editorMode=CREATE`；修改页不写入、不删除创建草稿。正文、紧急程度也纳入创建草稿变化监听，点击“＋”只恢复明确标记的创建草稿。
6. 夜间“全部任务”卡片：按实际标签底色分别计算标题、时间与负责人等文字的对比度，浅色底使用深色字、深色底使用浅色字，避免一律套用夜间浅色字体。
7. 同步处理任务详情、任务明细表格、标签筛选、创建标签选择、标签管理列表与颜色选择的同类浅底浅字问题，标签移除/选中图标也与底色保持对比。日历事项/任务文字保留黑字优先，在底色不满足 4.5:1 时使用可读的替代色。

## 主要文件

- `app/src/main/java/com/example/project_pandora/ui/task/TaskCreateActivity.java`
- `app/src/main/java/com/example/project_pandora/ui/task/TaskFormState.java`（新增）
- `app/src/main/java/com/example/project_pandora/ui/widget/PhoneMonthView.java`
- `app/src/main/java/com/example/project_pandora/ui/home/DailyAgendaController.java`
- `app/src/main/java/com/example/project_pandora/ui/home/DailyItemsMonthView.java`
- `app/src/main/java/com/example/project_pandora/core/util/ReadableTextColors.java`（新增）
- `app/src/main/java/com/example/project_pandora/ui/view/MonthTaskAdapter.java`
- `app/src/main/java/com/example/project_pandora/ui/widget/TaskScheduleView.java`
- `app/src/main/java/com/example/project_pandora/ui/task/TaskTagColors.java`
- `app/src/main/java/com/example/project_pandora/ui/task/TaskDetailActivity.java`
- `app/src/main/java/com/example/project_pandora/ui/task/TaskDetailsActivity.java`
- `app/src/main/java/com/example/project_pandora/ui/task/TaskDetailsAdapter.java`
- `app/src/main/java/com/example/project_pandora/ui/task/TaskTagPickerView.java`
- `app/src/main/java/com/example/project_pandora/ui/task/TaskTagManageActivity.java`
- 新增测试 `TaskFormStateTest.java`、`ReadableTextColorsTest.java`，更新 `PhoneMonthViewTest.java`。

## 数据库与 API

本轮没有数据库结构变化，没有新增或修改 API。草稿仍使用现有 `TASK` 类型及现有草稿 API，在允许扩展的内容对象中添加创建来源标记，无需数据库迁移或后端部署。

## 删除或替换的旧逻辑

- 修改页共用创建草稿槽进行自动保存、提交修改成功时删除创建草稿。
- 月历“只要当天有任务，点击日期或空白也打开详情”的判断，以及普通日期任意位置拖动第一项事项的兜底。
- 点击首页空日期后自动滚到事项列表。
- 浅色任务/标签底一律使用夜间浅色文字的固定配色。
- 顶部返回直接结束修改页而不检查未保存内容。

## 已完成验证

- 先检查 Gradle 任务图；本轮没有执行 APK 生成、安装或设备仪器测试任务。
- Android Java/资源编译通过。
- 仪器测试源码编译通过；日历测试预期已调整为日期与空白仅选择、任务条/更多项/任务圆点打开内容，含单任务与多任务日期。
- JVM 测试共 210 项通过，0 失败、0 错误。本轮新增 8 项测试，覆盖编辑字段变化判断、撤销变化、多人顺序、主标签顺序、状态序列化、草稿来源隔离，以及实际昼夜标签色板的文字对比度。
- 色板验证同时使用实际明暗主题资源中的标题/次要字色，修正后的文字与对应标签底色对比度不低于 4.5:1。
- `git diff --check` 通过（既有日期选择文件只有 Git 换行符提示）。

## 限制与待验收

- 旧版 `TASK` 草稿没有来源标记，无法可靠区分创建与修改，因此保留服务器数据，但不自动提供恢复；本轮以后保存的创建草稿可以正常恢复。没有批量删除旧草稿或更改已保存任务数据。
- 本轮未运行改后版本的设备界面验收，键盘/滚动行为、返回确认和日历触摸路由仍需实际设备验证。没有将“仪器测试源码编译”描述成“设备测试通过”。
- 已修正检查中发现的任务卡片和标签配色问题，并验证对应色板；尚未逐页完成所有页面的真实夜间截图验收，不能声称所有页面均已验收。
- 本轮未重新打包 APK，之前生成的 `app-debug.apk` 不包含本轮修正。
- 本轮没有因缺少 PDF、图片或其他资料而未实现的内容。
