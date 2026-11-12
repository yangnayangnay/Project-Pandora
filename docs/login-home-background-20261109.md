# 登录与首页背景调整（2026-11-09）

## 本次修改

- 首页事件纪与任务看板提高橙／绿色饱和度，降低色层不透明度和顶部白色高光，保留磨砂背景、单条均匀描边和柔和阴影。夜间有独立配色。
- 大楼恢复原始照片，删除上一版暖阳后处理。照片覆盖完整视口，底部只增加轻微底色遮罩，不再在页面 72% 处完全消失。日历滚动期间背景持续可见。
- 本次没有改动上次修复的卡片收起、刷新高度同步和滚动隐藏逻辑，照片始终在页面底层绘制。
- 手机登录页增加暖桃色、奶油色和白色融合的圆形动态背景，两个色块缓慢漂浮；表单置于半透明圆角卡片中，最大宽度 440dp，小屏和键盘弹出时仍可滚动。
- 管理员登录页采用同一视觉方向，以 CSS 渐变圆色块和缓慢位移动画实现，适配窄屏与日夜模式。
- Android 动画随页面离开／隐藏停止，遵循系统动画关闭设置；网页遵循 `prefers-reduced-motion`，减少动态效果时停止漂浮。

## 主要文件

- `app/src/main/java/com/example/project_pandora/ui/home/HomeBackdropLayout.java`
- `app/src/main/res/values/home_glass.xml`
- `app/src/main/res/values-night/home_glass.xml`
- `app/src/main/java/com/example/project_pandora/ui/login/WarmLoginBackdropView.java`（新增）
- `app/src/main/res/layout/activity_login.xml`
- `app/src/main/res/values/login_colors.xml`（新增）
- `app/src/main/res/values-night/login_colors.xml`（新增）
- `pandora-web/src/views/login/LoginView.vue`

## 验证

1. 检查 Gradle 任务图后执行 Android Java／资源编译、仪器测试源码编译和 JVM 测试，均通过。
2. 184 项 JVM 测试通过，0 失败、0 错误、0 跳过。
3. `npm.cmd run build` 通过，仍有项目已有的大体积代码块提示。
4. 用独立临时浏览器资料目录检查实际构建后的管理员登录页：1280×900、375×812 两种尺寸，白天／夜间四种组合均没有横向溢出，表单保持在视口内。
5. 浏览器确认正常设置下背景位移确实变化；减少动态效果时两个动画均停止。截图与检查结果存于 `app/build/ui-preview/`。
6. `git diff --check` 通过。

## 范围及限制

- 无 API、数据库、账户密码、权限或连接配置变更。
- 网页仅在本地构建和预览，未部署到服务器。
- 未生成或安装 APK，未进行本次手机端模拟器／真机视觉和触摸验收。仪器测试只完成源码编译。
