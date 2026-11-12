# 首页事项、任务时间、个人资料及横屏明细调整

本轮工作目录：`D:\panduola`。仅保留源码修改并验证编译，未打包或安装 APK，未部署服务器，未改账号密码、未写入业务数据库。

## 实际修改

1. 首页“新增事项”改用与公司任务创建相同的悬浮按钮，固定于页面右下角、底部导航上方；按钮脱离日历滚动区域，展开日历时不自动隐藏。内容底部保留空间避免最后一项被按钮遮挡。
2. 任务截止时间必须严格晚于开始时间。手机端创建/修改共用校验同步更新；后端创建、修改及审核通过前均校验。只改开始或截止时间时，与数据库中另一端时间比较，不能通过部分更新绕过。保持原有日期格式与审核权限。
3. 手机端与 Web 个人签名统一最多 20 字，后端 `PUT /user/me` 同步拒绝超限提交。已有长签名加载时保留原文，编辑并保存前要求缩短，不自动修改历史数据。
4. 默认头像继续使用姓名后两个字。修复 `NameAvatarDrawable` 缺少固有宽高，导致“我的”及个人信息所用 `CircleImageView` 无法转成头像位图的问题；进入页面时先初始化默认头像，用户资料返回后显示真实姓名。
5. 任务明细横屏隐藏搜索框、筛选区、条件摘要、任务数量与刷新行，扩大表格空间；保留标题栏的返回和竖屏切换按钮。切回竖屏时保留筛选状态及条件。横屏不自动弹键盘。
6. 筛选按钮文字固定为“筛选”，删除可见的括号数字；保留同色三角形与无障碍条件数量说明。

## 主要文件

- 首页：`app/src/main/res/layout/fragment_mindmap.xml`
- 任务：`app/src/main/java/com/example/project_pandora/core/util/TaskDateTimes.java`、`ui/task/TaskCreateActivity.java`、`ui/task/TaskDetailsActivity.java`
- 个人资料：`app/src/main/java/com/example/project_pandora/ui/mine/NameAvatarDrawable.java`、`MineFragment.java`、`ProfileActivity.java`；`app/src/main/res/layout/fragment_mine.xml`、`activity_profile.xml`
- 后端：`pandora-backend/src/main/java/com/pandora/backend/task/TaskChangeService.java`、`user/UserController.java`
- Web：`pandora-web/src/views/user/ProfileView.vue`
- 验证：`TaskDateTimesTest.java`、`HomeAgendaTest.java`、新增 `NameAvatarRenderingTest.java`；后端 `TaskChangeServiceTest.java`、`UserAvatarAndRelationsTest.java`。

以上 Android 源码的省略路径均相对于 `app/src/main/java/com/example/project_pandora/`，测试路径遵循原有目录。

## 数据库与 API

数据库结构不变，无迁移，不截断已存签名、不修改已有任务时间。

无新增 API；以下原有接口收紧校验，参数及响应结构不变：

- `POST /task`：截止时间须晚于开始时间。
- `PATCH /task/{taskId}`：修改时间时对完整时间范围校验。
- 任务审核通过流程：再次校验申请中的排期，避免旧申请写入无效时间。
- `PUT /user/me`：个人签名最多 20 字。

## 清理的旧逻辑

- 删除跟随内容滚动的标题栏新增事项按钮，复用同一点击处理。
- 将接受相等时间的任务校验改为严格递增，替换原先只检查日期部分的后端校验。
- 删除个人签名 160 字界面和后端限制，统一为 20 字。
- 删除“我的”及个人信息的静态头像占位图，由姓名头像初始化与加载负责。
- 删除筛选按钮可见的条件数量，以及横屏控制区对表格的占位。

## 已完成验证

- 检查 Gradle 任务图，无 APK 生成、安装任务。
- Android 主源码、资源、设备测试源码编译通过。
- Android JVM 测试：**191 项通过，0 失败、0 错误**。包括相等时间拒绝、下一分钟及跨日时间允许。
- 后端相关测试：**45 项通过，0 失败、0 错误**。包括相等/倒序时间、部分修改、旧申请审核校验、20/21 字签名边界、清空签名和旧签名不被覆盖。测试使用独立 H2 内存数据库。
- Web `npm.cmd run build` 通过；构建仍有体积较大模块的提示，无构建失败。
- 173 个资源 XML 解析通过，`git diff --check` 通过。

## 剩余限制与缺失资料

按照用户暂不打包安装的要求，新版本未进行设备界面验收。新增圆形头像渲染、悬浮入口布局的设备测试仅编译，未执行；横屏真实显示与触摸效果尚需运行修改后的版本验证，不将其描述为已在设备上通过。

服务端校验已修改源码但未部署，连接远程旧后端时仍使用远程现有规则。本轮无需指定 PDF，无资料缺失项。
