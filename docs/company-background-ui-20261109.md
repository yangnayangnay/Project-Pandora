# 首页排版与公司系列背景调整

## 实际修改

- 首页事件纪、任务看板的标题及副标题统一到正文左侧边缘；标题与副标题间隔为 2dp，正文顶部留白缩小。展开按钮保留 48dp 点击区域，窄屏文字允许换行。
- 任务行移除旧的系统选中背景，改为随日夜主题调整的淡蓝圆角水波纹；展开按钮、完整看板卡片及标签页使用协调的反馈色。
- 首页保留大楼照片；公司、日志、AI 地图、我的分别使用室内、工位、天空、办公休息区照片。公司月/周/日视图支持背景透出，任务内容保留独立的可读底色。
- “我的 → 设置 → 页面背景”新增纯色、公司系列背景图两个选项。默认公司系列，设备本地保存，切换立即更新已附着页面，返回或重建页面后保持选择。
- 共享照片加载和渲染，保留首页原有磨砂、白色与黄/蓝色交融、均匀描边和阴影。照片本地随包加载，日夜分别调整透明度及文字颜色。

## 主要文件

- `app/src/main/res/layout/fragment_mindmap.xml`
- `app/src/main/java/com/example/project_pandora/ui/mindmap/MindMapFragment.java`
- `app/src/main/java/com/example/project_pandora/ui/home/HomeBackdropLayout.java`
- `app/src/main/java/com/example/project_pandora/ui/home/HomeBoardAdapter.java`
- `app/src/main/java/com/example/project_pandora/ui/home/HomeBoardsActivity.java`
- `app/src/main/java/com/example/project_pandora/ui/widget/CompanyBackgrounds.java`
- `app/src/main/java/com/example/project_pandora/ui/widget/CompanyBackdropDrawable.java`
- `app/src/main/java/com/example/project_pandora/core/config/BackgroundPrefs.java`
- `app/src/main/java/com/example/project_pandora/ui/mine/SettingsActivity.java`
- `app/src/main/res/layout/activity_settings.xml`
- 四个主页面的 Fragment、日历控件及日志布局，以及 `values[-night]/company_backgrounds.xml`。

## 数据与旧逻辑

本次不修改数据库结构、API、后端、账号密码、连接配置或组织权限。新增的是独立的本机外观偏好。删除了任务行旧系统选中效果；首页背景原有重复的解码、模糊和绘制实现移入共享背景绘制类。其他既有未提交修改保留。

## 验证

- 执行前检查 Gradle 任务图；依据此前明确要求生成更新 APK，未安装或启动应用。
- `assembleDebug compileDebugAndroidTestJavaWithJavac testDebugUnitTest` 成功。
- 现有 JVM 测试 184 项，失败 0、错误 0、跳过 0。
- Android 仪器测试源码编译成功；没有在设备上运行仪器测试。
- 149 个资源 XML 解析成功，`git diff --check` 通过。
- 对本次照片原始像素叠加底色后的辅助文字进行对比度计算：日间最低 4.80:1，夜间最低 5.83:1。该数值检查不替代设备画面验收。
- APK 签名校验通过（v2），文件：`app/build/outputs/apk/debug/app-debug.apk`。

## 限制与待验收

未进行模拟器/真机上的页面切换、背景偏好重启保持、横竖屏、大字体及日夜模式的交互与视觉验收；这些项目不能算设备测试通过。本次背景覆盖主导航页面、设置和首页看板详情，不扩展到每个业务编辑表单。未缺少本次所需资料；照片来源见 `home-background-source.md`。
