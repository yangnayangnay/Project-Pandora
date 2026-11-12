# 日历、负责人和 AI 页面调整（2026-11-10）

本记录仅涵盖本轮需求。工作目录是 `D:\panduola`，保留工作区原有未提交改动。

## 1. 实际修改的功能

- 公司月历下方任务列表区域使用连续的主题表面底色：日间白色，夜间使用深色表面，避免只露出一条白块。
- “我的”姓名和职位所在头像区域改为透明背景；个人信息页头像提示删除原图片大小说明，仅保留“点击头像修改”。
- 事项月历点击事项时，无论一项或多项，都复用任务日历的小窗展示完整内容；长内容可滚动，小窗内点击事项仍可编辑。保留事项 ID、跨日日期和完成状态。
- 公司任务月历的单任务日期直接打开任务详情；多任务日期仍显示小窗。
- 月历星期、农历和特殊日期标题为 12sp，月/周/日日期数字为 18sp，日程小字为 11sp。Android 使用 sp，默认系统字号下与需求中的大小对应，并保留系统字号支持。
- 月历普通模式下，节日或节气替换农历位置；数字和下方标题靠近，选中背景覆盖两行，小点靠近对应日期。
- 详细月历任务条支持两行文字，减少左右留白，条目高度随字号调整；保留每日四项和独立溢出入口，各条目有独立范围。
- 设置新增“整体字号”：较小、默认、较大、大（90%、100%、110%、120%），叠加系统字号。应用内页面统一读取该设置；日历同步调整占用空间。
- 共用日期选择窗增加圆角主题表面、彩色日期预览和选中区域；左下方可勾选“显示农历 / 阳历”，预览对应日期的农历和特殊日期。该选项控制展示，不改变原有公历日期存储和输入规则。
- 首页事项月历补上左右换月监听；公司日历继续复用现有月/周/日切换逻辑，横滑切换后增加短距离滑入和淡入动效。尊重系统关闭动画设置。
- 修复多人负责人编辑回显：兼容 SQL 返回的大小写键名及已有负责人格式，不再只读取旧的单一负责人。旋转或页面重建时保留选择和姓名。
- 负责人选择改为完整页面，复用现有部门、团队和搜索组件；支持多选、已选标签、单独移除、清空和人数确认。取消不改变表单选择；仍使用既有权限控制的候选接口。
- AI 配置兼容服务根地址和完整聊天端点；手机端测试、保存、配置透传以及后端实际调用同步使用地址转换。增加从服务端获取可用模型 ID 的入口和具体 HTTP 错误提示。
- AI 地图和分析入口优化内容层次、图标和说明，保留既有功能。MBTI 增加 16Personalities 四组颜色人物和对应类型人物；大字号时问卷可滚动。
- 梅花易数增加签筒摇晃、落签、实际卦象浮起和结果卡片入场动效；请求期间禁用重复操作，离开页面取消动画，保留每日一次规则。

## 2. 主要文件

所有路径均相对于 `D:\panduola`：

| 范围 | 主要文件 |
| --- | --- |
| 日历布局和文字 | `app/src/main/java/com/example/project_pandora/ui/widget/PhoneMonthView.java`、`CalendarCellMetrics.java`、`WeekDateStrip.java`、`TaskScheduleView.java`；`app/src/main/res/values/schedule_dimens.xml` |
| 日历点击和横滑 | `ui/home/DailyAgendaController.java`、`ui/view/ViewFragment.java`、`ui/widget/DayTasksPopup.java`、`PeriodSwipeLayout.java`、`CalendarPageMotion.java`（均位于上述 Java 包根目录） |
| 日期选择 | `ui/widget/QuickDatePickerDialog.java`、`YearMonthDayPicker.java` |
| 全局字号 | `core/config/FontSizePrefs.java`、`ui/PandoraActivity.java`、各 Activity 的基类接入、`ui/mine/SettingsActivity.java`、`app/src/main/res/layout/activity_settings.xml` |
| 多人负责人 | `core/util/TaskAssignees.java`、`ui/task/TaskCreateActivity.java`、`TaskAssigneesActivity.java`、`app/src/main/res/layout/activity_task_assignees.xml`、`activity_task_create.xml`、`app/src/main/AndroidManifest.xml` |
| AI 连接 | `core/network/AiEndpoint.java`、`AiConfigInterceptor.java`、`ui/ai/AiConfigActivity.java`、`app/src/main/res/layout/activity_ai_config.xml`；`pandora-backend/src/main/java/com/pandora/backend/ai/AiEndpoint.java`、`ExternalAIClient.java` |
| AI 交互和素材 | `ui/aimap/DivinationRitualView.java`、`DivinationActivity.java`、`AIAnalysisActivity.java`、`ui/ai/MBTITestActivity.java`、`MbtiAvatars.java`；对应 AI 布局和 `app/src/main/res/drawable/mbti_*.xml` |
| 页面底色和提示 | `app/src/main/res/layout/fragment_view.xml`、`fragment_mine.xml`、`app/src/main/res/values/strings.xml` |

## 3. 数据库结构

本轮没有新增数据库表、字段或迁移，没有操作线上数据库、账号或密码。新增外观选项只存储在设备的独立外观偏好中。工作区此前已有的数据库改动不属于本轮修改。

## 4. API

- 既有任务详情、任务修改、负责人候选、事项、起卦记录接口继续使用，业务请求结构和权限范围保持现状。
- AI 配置页新增直接调用模型提供商的 `GET .../models`，只显示该服务实际返回的模型 ID。
- 手机端和后端统一规范聊天请求地址：例如 `https://api.deepseek.com` 转为 `https://api.deepseek.com/chat/completions`；`https://api.openai.com` 转为 `/v1/chat/completions`；已有完整自定义端点予以保留。

## 5. 清理的旧逻辑

- 删除任务创建/编辑页遗留的隐藏单选负责人 Spinner、关联适配器和绑定逻辑，使用实际的多人选择集合。
- 移除仅取小写 `id` 而退回单负责人字段的编辑回显方式。
- 移除月历特殊日期独占第三行的尺寸逻辑，改为农历/节日同一标题位置。
- 移除直接将 AI 服务根地址作为聊天推理地址的方式。
- 删除头像上传大小说明和“我的”头像区的不连续白色背景。

## 6. 已完成验证

- 执行前检查 Gradle 任务图，确认只编译资源、Java 和测试源码并运行 JVM 测试，不生成 APK。
- Android 主源码/资源编译、仪器测试源码编译通过。
- Android JVM 测试 190 项，失败 0、错误 0。新增覆盖 AI 地址转换及错误信息、多人负责人格式兼容；日历尺寸测试覆盖四项加溢出、两行文字和较大字号空间。
- 后端针对 AI 的 3 项测试通过：地址转换；使用本地临时 HTTP 服务验证真实 POST 路径、模型、测试认证头和响应提取。未使用真实 API Key。
- 168 个 Android 资源 XML 解析通过；`git diff --check` 通过。

## 7. 尚未验证和实际限制

- 没有生成或安装 APK，没有执行模拟器/真机界面测试。横滑动效、夜间模式实际观感、窄屏及最大系统字号仍需要设备验收；仪器测试源码编译不等于设备测试通过。
- 横滑效果是切换时约 200ms 的滑入，不是两张完整日历跟随手指并排移动的翻页器。
- 截图中的 404 不能仅凭截图归因于模拟器或网络。现有代码确有把服务根地址直接用于聊天请求的问题，已修正；未使用用户真实密钥验证配额、模型权限和完整外网推理，因此不声称真实 DeepSeek 连接已经验收。可在配置页获取服务提供的模型后再测试。
- 本轮后端修改保留在本地，未部署服务器；手机端同时规范透传地址，使已有保存的根地址配置能使用正确端点。
- 紧凑的七列日历仍有宽度限制，长标题两行后会省略，完整内容通过小窗或详情查看。

## 8. 缺少资料或未实施部分

- 当前代码没有独立的“星座运势”模块。本轮没有增加该业务功能，也没有把它描述为已完成；优化集中在已有的梅花易数、MBTI 和 AI 分析入口。
- “显示农历 / 阳历”实现为显示开关，没有新增农历日期输入、农历生日或存储规则。
- 本轮不依赖指定 PDF；没有因 PDF 缺失而猜测字段或顺序。

## 人物素材来源

- 来源页面：<https://www.16personalities.com/personality-types>，2026-11-10 获取；人物属于 16Personalities 插画，非 MBTI 机构发布的人物。
- 共 16 种类型：INTJ、INTP、ENTJ、ENTP、INFJ、INFP、ENFJ、ENFP、ISTJ、ISFJ、ESTJ、ESFJ、ISTP、ISFP、ESTP、ESFP。
- 将该页面使用的 SVG 路径、颜色、透明度和变换转为 Android 原生 VectorDrawable，并组合四类人物入口图；透明底，无白色方块，运行时不请求素材网站。分析页保留来源署名。
- 原 SVG 临时副本位于 `app/build/ui-assets/mbti`；构建实际使用 `app/src/main/res/drawable/mbti_*.xml`，不依赖临时副本。
