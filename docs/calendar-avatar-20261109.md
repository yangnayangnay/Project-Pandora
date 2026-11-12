# 日历、首页与头像调整（2026-11-09）

## 本次实际修改

1. 首页进入完整事项日历后隐藏事件纪和任务看板，保留原有“显示板块”返回入口。限制顶部卡片越界绘制；刷新后卡片高度变化时重新同步收起位置；日历内容已有滚动时保持卡片收起。
2. 事项和任务月历统一为每天最多展示 4 条业务事项／任务，另设溢出提示。跨日条和跨周分段逻辑继续复用，特殊日期有独立区域，不占用 4 条业务内容位置。
3. 月历日期数字与左上月份标题统一为 22sp。数字圆圈、农历、特殊日期分别预留高度，避免选中背景覆盖农历。字号放大时单元格同步增高。
4. 公司任务日历的月／周／日任务标题统一为黑色，不再根据标签决定字色。夜间任务色块改用较浅的配色以保障黑字可读，事项日历保留自身的日夜文字配色。
5. 两种日历共用特殊日期标注：二十四节气、春节／除夕／元宵／端午／中秋等农历节日，以及元旦、劳动节、国庆节、国家公祭日等固定日期。月历中用独立小矩形显示；周／日日期条和任务日弹窗同步标注。
6. 首页现有大楼照片在本地加载时进行暖阳提亮和轻微饱和度调整，照片透明度与渐变位置不变，磨砂卡片同步使用调整后的背景。
7. 默认头像显示姓名后两个 Unicode 字符，单字姓名显示一个字；没有姓名则使用账号，资料尚未提供时使用“用户”。已有照片继续加载，失败时回退姓名头像。个人页、账号资料、通讯录人员列表及详情同步接入，日志头像也使用相同回退规则。

## 主要文件

- `app/src/main/java/com/example/project_pandora/ui/home/DailyAgendaController.java`
- `app/src/main/java/com/example/project_pandora/ui/mindmap/MindMapFragment.java`
- `app/src/main/res/layout/fragment_mindmap.xml`
- `app/src/main/java/com/example/project_pandora/ui/widget/PhoneMonthView.java`
- `app/src/main/java/com/example/project_pandora/ui/widget/CalendarCellMetrics.java`
- `app/src/main/java/com/example/project_pandora/ui/widget/CalendarDayLabels.java`（新增）
- `app/src/main/java/com/example/project_pandora/ui/widget/SpecialCalendarDates.java`（新增）
- `app/src/main/java/com/example/project_pandora/ui/widget/WeekDateStrip.java`
- `app/src/main/java/com/example/project_pandora/ui/widget/TaskScheduleView.java`
- `app/src/main/java/com/example/project_pandora/ui/widget/DayTasksPopup.java`
- `app/src/main/java/com/example/project_pandora/ui/home/HomeBackdropLayout.java`
- `app/src/main/java/com/example/project_pandora/ui/mine/AvatarLoader.java`
- `app/src/main/java/com/example/project_pandora/ui/mine/AvatarInitials.java`（新增）
- `app/src/main/java/com/example/project_pandora/ui/mine/NameAvatarDrawable.java`（新增）
- `app/src/main/java/com/example/project_pandora/ui/mine/MineFragment.java`
- `app/src/main/java/com/example/project_pandora/ui/mine/ProfileActivity.java`
- `app/src/main/java/com/example/project_pandora/ui/people/PeopleDirectoryView.java`
- `app/src/main/java/com/example/project_pandora/ui/view/CompanyPersonActivity.java`
- `app/src/main/res/layout/item_people_directory.xml`
- `app/src/main/res/layout/activity_company_person.xml`
- `app/src/main/res/layout/fragment_view.xml`
- `app/src/main/res/values/schedule_dimens.xml`
- `app/src/main/res/values/month_task_palette.xml`
- `app/src/main/res/values-night/month_task_palette.xml`
- `app/src/main/java/com/example/project_pandora/ui/log/LogAdapter.java`
- `app/src/main/java/com/example/project_pandora/ui/log/SubordinateLogAdapter.java`

## 数据、接口和清理

- 本次无数据库结构变更，无 API 增删改。通讯录复用现有接口返回的 `avatar`。
- 没有修改账号密码、组织权限、服务地址或已有业务数据。
- 删除任务日历按标签匹配文字颜色的逻辑及两套不再使用的文字色板，替换每天 5 项的显示上限。
- 保留仓库中此前已存在的修改，没有回退其他功能。

## 验证

执行前已检查 Gradle 任务图，未触发 APK 生成或安装。

```text
gradlew.bat compileDebugJavaWithJavac compileDebugAndroidTestJavaWithJavac testDebugUnitTest --dry-run
gradlew.bat compileDebugJavaWithJavac compileDebugAndroidTestJavaWithJavac testDebugUnitTest
```

- Android Java／资源编译通过。
- 仪器测试源码编译通过；这不代表已在设备执行。
- 184 项 JVM 测试通过，0 失败、0 错误、0 跳过。
- 布局计算测试覆盖系统字体比例 1、1.3、1.5、2、3 下的 4 项与溢出边界，以及数字／农历／特殊日期分区。
- 特殊日期测试覆盖 2026 年全部二十四节气、跨年及闰年节气变化、闰月排除、短月除夕及节日重叠。
- 姓名测试覆盖两个字、单字、空值、账号回退的字符截取及补充平面汉字。
- `git diff --check` 通过。

## 限制与待验收

- 未打包、安装 APK，未在模拟器／真机执行本次视觉、触摸或截图验收。首页收起与恢复、选中圆圈以及日夜配色仍需设备验收。
- 节气使用现代太阳黄经近似计算，显示范围为 1900–2100 年；农历通过 Android ICU 获取。范围外仍显示可计算的农历和固定节日。
- 当前没有年度官方放假／补班安排数据，因此标记节日当天，不擅自生成假期区间或补班日期。
- 大楼沿用现有通用照片并调整光照观感，未替换为公司实景。
