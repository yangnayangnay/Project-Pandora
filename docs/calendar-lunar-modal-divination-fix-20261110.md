# 农历、日历层次与起卦交互修复（2026-11-10）

本轮工作目录为 `D:\panduola`。保留原有未提交修改；未改账号、密码或业务数据库，未部署后端，未生成或安装 APK。

## 1. 实际修改

- **农历年份**：移除固定减去 2637 的换算假设，使用已知春节日期校准设备农历库的年份纪元。当前 Android 17 / API 37 模拟器对 2026-11-10 返回 `EXTENDED_YEAR=2026`，桌面旧 ICU 返回 `4663`；旧代码在前者上必然换算错误。修复后两种实现均可准确往返转换，不改变选中的实际日期、时分。
- **梅花易卦**：当天已有记录时按钮显示“再测一次”并允许操作；签筒入口同样可重测。居中放大动画改为等待窗口完成首次布局后启动，再播放摇筒、出签及浮现卦象。提示文字放在签筒下方，避免重叠。请求期间禁止重复起卦，超时后恢复按钮。
- **日历详情**：首页事项与公司任务共用的月历，内容从日期区下方第一行开始；节日/节气块放在当天占用的任务行之后。超出四项时保留四行内容及溢出入口，特殊日期块在溢出行之后；跨日条带继续保持对齐，不被节日打断。
- **日期弹窗**：首页日期、事项起止日期、公司日期入口不再重复关闭并新建同类弹窗。旧窗口异步关闭回调只能清理自己，避免误清空新窗口引用。事项编辑窗的尺寸与输入法设置改为显示后同步完成。
- **新增事项入口**：自测时原按钮在首屏可见高度仅约 6 像素，位于底部导航上沿。将原按钮移到“每日事项”标题栏，保持 48dp 触摸区域及原配色。

## 2. 主要文件

Android：

- `app/src/main/java/com/example/project_pandora/ui/widget/LunarDateWheelModel.java`
- `app/src/main/java/com/example/project_pandora/ui/widget/CalendarCellMetrics.java`
- `app/src/main/java/com/example/project_pandora/ui/widget/PhoneMonthView.java`
- `app/src/main/java/com/example/project_pandora/ui/aimap/DivinationActivity.java`
- `app/src/main/java/com/example/project_pandora/ui/aimap/DivinationRitualOverlay.java`
- `app/src/main/java/com/example/project_pandora/ui/home/DailyAgendaController.java`
- `app/src/main/java/com/example/project_pandora/ui/home/DailyItemEditor.java`
- `app/src/main/java/com/example/project_pandora/ui/view/ViewFragment.java`
- `app/src/main/res/layout/fragment_mindmap.xml`

后端：`pandora-backend/src/main/java/com/pandora/backend/ai/AIController.java`。

测试：`CalendarCellMetricsTest.java`、`LunarDateWheelModelTest.java`、`PhoneMonthViewTest.java`、`HomeAgendaTest.java`、新增 `pandora-backend/src/test/java/com/pandora/backend/ai/DivinationRecordTest.java`。

## 3. 数据库与接口

- **数据库结构不变**，无需迁移。起卦仍使用原有 `(user_id, divination_date)` 唯一记录。
- `POST /ai/divination/record` 新增可选布尔参数 `reroll`。传 `true` 时保存本次新卦象与对应解读，更新本人当天记录；当天尚无记录时插入。不会更新历史日期或其他用户记录。
- 省略该参数或传 `false` 时保留旧客户端回显当天已有记录的行为。
- `GET /ai/divination/today` 路径及字段不变，返回当天最近保存的结果。
- 当前修改仅在源码中；“再测一次”的保存行为需要运行配套新后端。旧后端仍会返回旧的当天结果。

## 4. 删除或替换的旧逻辑

- 固定农历年份偏移量。
- 手机端“今日已算”按钮置灰及禁止再次起卦的分支。
- 使用普通 `post` 猜测动画布局完成时机。
- 月历将特殊日期固定排在任务前、预先占用第一行的逻辑。
- 日期入口重复弹窗，以及关闭回调无条件清空共享窗口引用的处理。

## 5. 已完成验证

- 检查 Gradle 任务图，确认只涉及源码/资源编译及测试，无 APK 生成、安装任务。
- Android 主源码、资源及设备测试源码编译成功。
- Android JVM 测试 **191 项通过，0 失败、0 错误**。
- 后端隔离内存数据库测试 **3 项通过**：再次起卦更新并可重新读取；兼容旧客户端；首次记录与历史记录/其他用户隔离。未连接业务数据库或外部 AI 服务。
- 将修复后的农历模型作为独立诊断程序在当前模拟器的真实 Android ICU 上执行，以下 **8 个日期**全部转换并还原成功：1900-01-01、2023-03-22、2024-02-10、2024-02-29、2025-08-01、2026-01-31、2026-11-10、2033-12-22。覆盖春节、闰二月、闰六月、闰冬月、跨年及边界；2026-11-10 得到农历 2026 年十月初二。诊断程序不是 APK，未安装应用。
- 使用桌面旧 ICU 对相同模型及相同 8 个日期验证，均通过。该检查验证两种年份纪元兼容性，不等同于设备上的滚轴界面验收。
- **173 个资源 XML** 解析通过，`git diff --check` 通过。

## 6. “点不动”的实测结论与未完成项

本轮确实捕获到模拟器的系统无响应提示。对应 ANR 为：

`Input dispatching timed out (Application does not have a focused window)`

调用栈中的应用主线程处于 `MessageQueue.nativePollOnce` 空闲等待；没有证据表明它被网络请求阻塞。窗口管理器认为应用窗口可见，但触摸系统 `FocusedWindows: <none>`，焦点请求指向已经不存在的登录窗口（`NO_WINDOW`）。切换其他应用时仍出现无焦点状态。因此本次异常涉及模拟器系统的窗口/输入焦点，不能当作服务器连接故障处理，也不能通过重新打包修复。

已保留应用数据并重启该模拟器系统，重启后触摸系统恢复有效窗口焦点，当前停留在登录页。此次重启没有卸载应用、清空应用数据或重置模拟器存储。

**尚未完成：** 用户描述的“登录后点日期/新增无响应，进入人员页面再返回才出现旧弹窗”的完整路径，未能在重启后继续复现和验收。源码中的弹窗竞争及入口遮挡已修正，但不能据此宣称所有间歇性点击故障已解决。按照用户“只保留源码修改，暂不安装”的要求，未安装修改后的版本；新增滚轴切换、节日 Canvas 排列、首页入口等设备测试仅编译，未在设备上执行。新签筒动画也尚未进行修改版本的设备视觉验收。

## 7. 资料缺失

本轮四项不依赖指定 PDF，所给日历截图已查看并用于确定“任务在上、特殊日期在下”的排列规则；无因缺少设计资料而无法完成的项目。剩余限制是上述运行版本及完整交互验证。
