# Android Studio 静态 UI 预览指南（Project Pandora）

> 本指南只用于检查布局外观，不包含生成新 APK 的步骤，也不能替代真机或模拟器上的功能验收。

**当前状态：** 月视图已改为普通月历、全部任务列表、逐日任务月历三种状态；本轮 `:app:compileDebugJavaWithJavac` 通过，34 项直接 JUnit 测试通过（含 16 项新增时间与切日回归）；模拟器日志确认并修复了 `MonthTaskAdapter` 字符串色板误用 `TypedArray.getColor` 导致的闪退。尚未生成或安装新 APK，也未在更新后的 App 中做视觉和手势验收，不能宣称模拟器验收通过。

## 1. 环境与路径

| 项目 | 值 |
| --- | --- |
| 项目根目录 | `E:\大学\软件测试实训\Project-Pandora` |
| Android Studio | `E:\Android`（2026.2.1，自带 JBR 25） |
| Gradle JDK | `E:\大学\软件测试实训\.tools\zulu21.40.17-ca-jdk21.0.6-win_x64`（JDK 21） |
| Gradle user home（可选） | `E:\大学\软件测试实训\.tools\gradle-home` |
| Android SDK | `C:/Users/78025/AppData/Local/Android/Sdk`（`local.properties` 当前实际指向，已在该 SDK 下编译成功） |
| Gradle / AGP | 项目 wrapper 当前为 9.8.0（由用户/Studio 维护，本任务不改动）；本迭代使用已安装的 Gradle 9.4.1 编译；AGP 9.2.1 |

用 **Open** 打开上述 **根目录**，不要选 `app` 子目录。`local.properties` 现指向实际生效的 `C:/Users/78025/AppData/Local/Android/Sdk`，编译已在该路径下成功；工作区内此前准备的 `E:\大学\软件测试实训\.tools\android-sdk` 仅作为可选备用 SDK，除非需要切换，否则无需改动。如已有依赖缓存在别处，再按需设置 Gradle user home。

## 2. 配置 JDK 与同步

1. 进入 `File > Settings > Build, Execution, Deployment > Build Tools > Gradle`。
2. 将 **Gradle JDK / Gradle JVM** 设为 JDK 21，可浏览到上表路径；不要选 Studio 自带的 JBR 25。
3. 确认 Android SDK 为 `C:/Users/78025/AppData/Local/Android/Sdk`（当前实际生效的 SDK），不要改动项目的 `compileSdk`。
4. 执行 `File > Sync Project with Gradle Files`，等待成功后再预览。Studio 会按项目 `gradle-wrapper.properties` 中当前设置的 Gradle 9.8.0 同步；本迭代的命令行编译使用的是已安装的 Gradle 9.4.1，两者不是同一版本，请勿据此把 wrapper 改回 9.4.1。

Sync 只解析配置和依赖，不生成 APK。若已有依赖无法解析，将 Gradle user home 指向 `E:\大学\软件测试实训\.tools\gradle-home` 后再次 Sync。

## 3. 打开布局预览

1. 展开 `app > src > main > res > layout`。
2. 打开 `preview_schedule_week.xml`、`preview_schedule_day.xml` 或 `preview_schedule_month.xml`。
3. 在编辑器中切换到 **Split** 或 **Design**。
4. 主题选择 **`Theme.Project_Pandora`**。
5. 设备选择手机竖屏，渲染 API 选择 `36.1`。
6. 点击 **Build & Refresh**。它会编译预览所需类，但不会生成 APK。

月视图预览文件位于 [`app/src/main/res/layout/preview_schedule_month.xml`](../app/src/main/res/layout/preview_schedule_month.xml)，根节点是 `MonthCalendarLayout`，其中包含 `PhoneMonthView` 月历、收起态的 `WeekDateStrip`、顶部月份导航和下方任务面板。

**Build & Refresh 不是 Build APK。** 本指南不包含 `Run`、`assemble` 或 `Build APK` 步骤；本任务也未执行 APK 构建任务。这不表示项目从未生成过 APK：既有 `app-debug.apk` 由用户此前的运行产生，本次没有生成新的 APK。

## 4. 周/日与月视图预览

`TaskScheduleView` 是原生自定义 View，只在 `isInEditMode()` 时注入内置样例任务；这些样例不访问实时任务 API。

- `preview_schedule_week.xml` 使用 `app:scheduleMode="week"`，渲染顶部全宽周一至周日日期头，以及其下的周任务列表。
- `preview_schedule_day.xml` 使用 `app:scheduleMode="day"`，共用同一日期头，并渲染全天任务行和 00:00–24:00 日时间轴。
- 周视图不是七列时间轴；时间轴和 `schedule_hour_height` 仅属于日视图。
- 周/月任务文字为 `12sp`，日视图为 `15sp`，可在 `app/src/main/res/values/schedule_dimens.xml` 中调整。

对应资源为 `schedule_week_task_text_size`（周）、`calendar_task_text_size`（月）、`schedule_day_task_text_size`（日）；`schedule_hour_height` 仅控制日视图 00:00–24:00 时间轴的行高。

### 月视图预览

- [`preview_schedule_month.xml`](../app/src/main/res/layout/preview_schedule_month.xml) 的根节点为 `MonthCalendarLayout`，上方是 `PhoneMonthView` 月历与拖拽提示，下方是任务标题、来源页签和任务卡片，收起态另含 `WeekDateStrip`。
- 该静态文件呈现初始 `NORMAL` 布局：日期、农历文本和任务圆点，以及当前筛选的全部任务卡片；`LIST` 全部任务态和 `DETAIL` 每日任务名称态需要运行时手势触发。
- 静态预览不能执行上滑/下滑切换三态、点击日期、点选 `+N项` 溢出块或切换来源页签；这些交互仍需真机或模拟器验收。
- 本迭代尚未在 Android Studio Design 中人工渲染该文件，未经视觉验证。

## 5. 其他可预览布局

`activity_log_create.xml`（日志录入）、`activity_task_create.xml`（任务创建）、`activity_profile.xml`（个人资料）、`activity_change_password.xml`（修改密码）也可在相同主题与设备下预览；`tools:*` 样例不代表真实账号、任务或接口数据。

### 任务创建字段

任务创建的开始和截止字段都先选日期，再以 24 小时制选择时分；取消选择不改变原值；截止不得早于开始；草稿保存包含时分；旧日期草稿的截止时间还原为 23:59。

## 6. 预览边界

静态预览使用内置样例，只能检查主题、颜色、间距、字号、可见性、控件组合等静态外观；它不能执行上滑/下滑切换月视图三态、点击日期、点选 `+N项` 溢出块或切换“我的任务 / 我布置的任务”页签，也不能测试实时任务 API、登录与权限、网络、数据库或业务提交，也不能验证手机上的点击、滚动、返回栈、生命周期和性能；实际 Studio 渲染尚未人工查看，未经视觉验证，不代表视觉验收通过。

## 7. 三视图布局说明

### 月视图三态

- **默认 `NORMAL`：** `PhoneMonthView` 显示公历日期、逐日农历文本和当天有任务时的圆点；日历下方任务面板显示当前筛选的全部任务卡片，不按所选日期过滤。默认任务列表合并去重“我的任务”和“我布置的任务”，同时另保留两个独立筛选页签。
- **上滑到 `LIST`：** 只收起月历，保留当前展示周的 `WeekDateStrip` 日期条；下方仍显示当前筛选的全部任务，不按所选日期过滤。
- **从 `NORMAL` 下滑到 `DETAIL`：** 展开月历，每个日期格内逐日显示当天任务名称；超出格子容量的部分显示 `+N项`，点击该溢出块会选中对应日期并收回 `NORMAL`。
- 来源页签在折叠/展开三态时始终保留，不会重置“我的任务 / 我布置的任务”的当前选择。
- 日期格只显示真实农历文本，不伪造节日/假期标签；本迭代没有节日角标或假期标签。
- 逐日农历由 Android `android.icu.util.ChineseCalendar` 在运行时计算，不使用写死的节日表。
- 月历以周一为每周第一天（表头“一”至“日”），首尾补齐相邻月份日期并淡化显示；这些相邻月份日期同样可点选。

### 周视图与日视图

- 周视图：与日视图共用顶部的全宽周一至周日日期头 `WeekDateStrip`；日期头下方是按开始时间排列的周任务卡片列表，不再绘制七列时间轴或网格，也没有全天任务行。
- 日视图：共用同一日期头，其下为当天全天任务行和完整的 00:00–24:00 时间轴；无论当天有没有任务都显示这条 00:00 至 24:00 的滚动轴，无任务时为空轴，有任务时按时段叠加。

本指南不包含生成新 APK 的步骤；项目中的既有 `app-debug.apk` 来自此前的运行，本任务未构建新 APK。原生 Design 的实际渲染尚未人工查看、未经视觉验证，不代表视觉验收通过。
