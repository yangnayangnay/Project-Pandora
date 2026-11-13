# 单一管理员、组织排序与可爱背景调整

日期：2026-11-13。实际项目目录：`D:\panduola`。

后续修订：插画缩放和静态卡片底色策略已按用户反馈调整，当前策略见 [演示资料与背景铺满记录](2026-11-13-demo-profiles-background-cover.md)。本文件保留此前批次的实施记录。

## 本轮实际变更

1. **已修复服务器账号数据**：吴六（工号 000007、用户 ID 100）的岗位从错误的 ADMIN 恢复为 FOUNDER。恢复依据为服务器 2026-11-06、2026-11-07 两份数据库备份，均记录其原岗位为 FOUNDER。遵循已确认的组织规则，不恢复旧备份中创始人错误的部门、团队归属。
2. 修复后服务器仅保留 `admin0`（ID 1）一个 ADMIN。修复程序逐一比较所有账号 ID 与密码哈希，确认完全未变；未修改密码、工号、姓名、业务数据，未重建数据库、未重启服务。
3. 后端源码禁止账号创建、直接注册、匹配旧人员档案自动注册、注册审批和人员档案维护授予 ADMIN。既有岗位调整接口继续只允许创始人至员工的业务岗位。Web 岗位选项移除 ADMIN，角色展示及查询筛选仍保留管理员名称。
4. 管理员端部门、团队的成员摘要和展开列表共用排序：部门负责人 → 团队负责人 → 员工，同岗位按工号自然排序，不改变原始数组。创始人和管理员仍不归入部门与团队。
5. Android 可爱背景改为五种独立构图：首页办公小镇、公司团队协作、日志手账桌、AI 云端探索、我的休息花园。保留同一套人物比例、柔和配色及轮廓。主体均移入画布，不再靠负坐标和边缘截断构图。
6. 插画使用完整缩放而非照片的铺满裁切，兼容横屏、短屏和分屏窗口；可爱插画略增强可见度，并取消底部额外淡出。照片与纯色背景保持原显示方式。
7. 可爱模式的大块静态卡片采用原配色的轻透底色，减轻对插画的遮挡，不改变文字透明度、前景点击反馈及布局；切回其他背景恢复原始卡片配色。首页自绘磨砂卡片不受该卡片处理影响。
8. 设置名称统一为：**纯色简约、商务办公、元气可爱**。存储键和值 0/1/2 不变，兼容已有选择。

## 主要文件

- `pandora-backend/src/main/java/com/pandora/backend/user/AccountCreationService.java`
- `pandora-backend/src/main/java/com/pandora/backend/auth/AuthController.java`
- `pandora-backend/src/main/java/com/pandora/backend/permission/ApprovalController.java`
- `pandora-backend/src/main/java/com/pandora/backend/permission/EmployeeRecordController.java`
- `pandora-backend/src/test/java/com/pandora/backend/user/AccountCreationTest.java`
- `pandora-web/src/utils/organization.js`
- `pandora-web/src/utils/roleConstants.js`
- `pandora-web/tests/organization.test.mjs`
- `app/src/main/java/com/example/project_pandora/ui/widget/CuteOfficeArtwork.java`
- `app/src/main/java/com/example/project_pandora/ui/widget/CuteArtworkPlacement.java`
- `app/src/main/java/com/example/project_pandora/ui/widget/CompanyBackdropDrawable.java`
- `app/src/main/java/com/example/project_pandora/ui/widget/CompanyBackgrounds.java`
- `app/src/main/res/values/company_backgrounds.xml`
- `app/src/test/java/com/example/project_pandora/ui/widget/CuteArtworkPlacementTest.java`

## 服务器修复留档

服务器备份目录：`/opt/pandora/backups/20261113-000007-admin-correction/`。

- 使用 H2 在线 BACKUP 创建修复前完整备份，不复制正在打开的数据库文件。
- `before.zip` 为首次尝试前备份。首次尝试因审计表不允许空操作人而失败，事务已整体回滚；再次查询确认吴六仍为 ADMIN 后才重试。
- `before-retry.zip` 为成功修复前备份。
- `RestorePersonnelRole.java` 保留精确匹配、事务、管理员数量和全员账号/密码一致性校验的修复过程。
- `result.txt` 记录成功提交。不伪装为管理员本人在产品内执行的操作，因此未向必须指定用户的审计表写入冒名记录。
- 已核对当前服务器 JAR：鉴权过滤器每请求读取数据库实时角色，吴六原有会话不再拥有 ADMIN 权限，无需注销其他用户或重启服务。

## 数据结构及 API

- 本轮无数据库结构变更，无新增 API。
- 修改现有接口的角色校验：`POST /user/create`、`POST /auth/register`、`POST /approval/approve/{userId}`、`POST /employee-records`、`PUT /employee-records/{id}`。
- 服务端账号数据修复已实际执行；上述防误授予限制和界面改动仍是本地源码，本轮未部署后端或 Web。

## 验证及交付边界

- 后端独立随机 H2 内存库测试：**219 项通过，0 失败、0 错误、0 跳过**。
- Web：**16 项测试通过**，Vite 生产构建成功；仍有既有的大 chunk 提示。
- Android：Java/资源编译、仪器测试源码编译及 **224 项 JVM 单测通过**。新增完整插画适配手机、横屏、方形窗口和分屏的检查。
- Gradle 执行前检查任务图，无应用 APK、测试 APK 生成及安装任务。
- 未运行设备界面验收；背景实际设备观感尚未验收，不将源码编译描述为设备测试通过。
- 本轮没有生成、替换或安装 APK，没有提交 Git 或推送仓库。原有未提交改动保留。
