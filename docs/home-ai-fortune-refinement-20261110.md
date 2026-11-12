# 首页、日视图与生辰测试修改记录（2026-11-10）

## 1. 实际修改

- 公司任务日/周视图的“当天/本周共 X 项任务”提示行补上与上下内容相同的 `color_company_content_surface` 背景，消除单独透出照片的色带。
- 首页事件纪、任务看板降低透明度（更不透明）：保留白色与暖黄/天蓝的交融，日间顶部不透明度约 95% → 97%，底部约 91% → 95%；夜间同步调整。加强顶部柔光，保留既有单层均匀描边及阴影。
- 首页卡片背后的照片模糊从两轮、半径 5 改为三轮、半径 10，在后台生成并缓存；页面原始照片透明度、清晰度不变，只有磨砂卡片使用模糊版本。
- AI 地图入口和二级页标题均改为“测试分析”，入口说明增加生辰运势。
- 梅花易数起卦动画从 1250ms 改为 5250ms，增加约 4 秒；保留原有 300ms 移入居中动画。采用摇筒、出签、停顿、卦象浮现和停留的时间分段，避免直接把摇晃拉慢。
- 新增“生辰八字 · 今日运势”：选择出生日期、出生时间，按北京时间计算四柱，展示农历生日、生肖、星座与四柱表层五行数量。
- 新页面采用排定四柱 → 遇见生肖 → 点亮星座 → 收拢今日灵感四段动画，总计约 4.4 秒，每段有停留，可直接跳过。四柱可点击查看位置说明；生肖、星座和工作提醒支持翻开/收起，各区有不同配色和线性图案。
- 支持返回、重新修改生辰、输入错误提示、出生时间不可晚于当前时间、页面重建时恢复出生信息和已翻开的卡片；切到后台时停止仪式动画，避免返回后补弹窗口。关闭系统动画时直接展示结果。

## 2. 主要文件

| 文件 | 作用 |
| --- | --- |
| `app/src/main/java/com/example/project_pandora/ui/widget/TaskScheduleView.java` | 日/周任务提示行背景 |
| `app/src/main/res/values/home_glass.xml`、`values-night/home_glass.xml` | 首页磨砂卡片明暗颜色 |
| `app/src/main/java/com/example/project_pandora/ui/widget/CompanyBackdropDrawable.java` | 背景模糊缓存 |
| `app/src/main/java/com/example/project_pandora/ui/aimap/DivinationRitualView.java` | 起卦动画分段与时长 |
| `app/src/main/java/com/example/project_pandora/ui/aimap/AIAnalysisActivity.java` | 新测试入口跳转 |
| `app/src/main/res/layout/fragment_aimap.xml`、`activity_ai_analysis.xml` | 入口名称、新测试卡片 |
| `app/src/main/java/com/example/project_pandora/ui/aimap/BirthFortuneActivity.java` | 生辰页面交互、分段动画、结果卡片及生命周期 |
| `app/src/main/java/com/example/project_pandora/ui/aimap/BirthFortuneEngine.java` | 日期校验、四柱、生肖、星座和本地每日提示 |
| `app/src/main/java/com/example/project_pandora/ui/aimap/FortuneOrbitView.java` | 仪式星轨绘制 |
| `app/src/main/res/layout/activity_birth_fortune.xml` | 生辰页面布局 |
| `app/src/main/res/values/birth_fortune.xml`、`values-night/birth_fortune.xml` | 文案、明暗主题颜色 |
| `app/src/main/res/drawable/ic_birth_fortune.xml` | 生辰测试入口插图 |
| `app/src/main/AndroidManifest.xml` | 注册新页面，禁止外部直接导出 |
| `app/build.gradle.kts`、`app/src/main/assets/licenses/lunar-java.txt` | 历法依赖与 MIT 许可 |
| `app/src/test/java/com/example/project_pandora/ui/aimap/BirthFortuneEngineTest.java` | 新增 11 项业务计算测试 |

## 3. 数据库与 API

- 本轮没有数据库结构变更，没有新增或修改后端 API。
- 生辰功能在本机临时计算，不写入账号生日，不调用外部 AI，不向服务器上传生辰。
- 新增依赖 `cn.6tail:lunar:1.7.4`，使用其节气历法及四柱计算；MIT 许可已随源码放入应用 assets。

## 4. 替换的旧逻辑

- 删除两处旧名称“AI数据分析展示”。
- 替换原先 1250ms 的起卦动画进度映射。
- 任务数量提示行由无背景改为与日程区域一致的背景。
- 不删除或重做现有 MBTI、梅花易数及 AI 建议功能。

## 5. 已完成验证

- 先检查 Gradle 任务图，确认不包含 APK 生成/安装任务。
- `compileDebugJavaWithJavac`：通过。
- `compileDebugAndroidTestJavaWithJavac`：通过；仅仪器测试源码编译，没有运行设备测试。
- `testDebugUnitTest`：202 项测试全部通过，0 失败、0 错误；本轮新增 11 项。
- 新测试覆盖：上游已知四柱样例、23 点晚子时与午夜换日、立春前后分钟边界、春节生肖变化、农历闰月、有效闰日及 1900 年日期、12 生肖/星座内容覆盖、星座边界、同日稳定性、设备时区独立性、跨日提示变化、非法日期/时间及未来生辰拒绝。
- 177 个资源 XML 解析通过，`git diff --check` 通过（现有 `YearMonthDayPicker.java` 仅有 Git 换行符提示）。
- 新增结果卡片的正文与底色对比度：日间 6.88–7.21，夜间 8.21–8.74，均超过 4.5:1。

## 6. 限制与待设备验收

- 按用户要求未生成、打包或安装 APK，未运行改后版本的设备界面测试。因此真实设备/模拟器上的动画顺滑程度、最终磨砂观感、横竖屏和最大系统字号排版尚未验收，不能把源码编译通过视作设备验收通过。
- 排盘约定为北京时间，年/月柱按节气，日柱使用零点换日流派；生肖按农历年，因此立春与春节之间可能出现八字年柱与生肖年不同，页面已说明。没有加入出生地点与真太阳时校正。
- 五行数量仅统计四柱的八个表层干支，不声称计算了藏干、旺衰或喜用神。
- “今日运势”和工作注意事项为结合四柱、生肖、星座及当日日期选取的本地文化娱乐灵感文案，并非预测模型或外部 AI 生成结果；相同生辰同一天结果稳定。
- 本轮没有依赖指定 PDF 或缺失设计资料的项目；未发现因缺少资料必须搁置的本轮需求。
