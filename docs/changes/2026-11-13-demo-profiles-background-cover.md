# 演示账号资料补齐、全部资料精简与背景铺满

日期：2026-11-13。项目：`D:\panduola`。

## 账号资料：已在线上实际执行

- 用户已明确确认当前账号均为演示账号，允许填充测试资料。
- 修复前共 12 个启用账号，其中 10 个缺身份证号及生日；姓名、邮箱、手机号均已有值。
- 已补齐这 10 个账号的 20 个空字段。原有 2 个账号的身份证号和生日保持不变；所有账号的密码、工号、姓名、角色、部门、团队、设置等其他用户字段均逐字段核对未变。
- 新身份证号使用 **000000 测试前缀**，18 位并包含校验字符；这是明确虚构的演示资料，不是有效真实身份证，不能用于实名认证。生日为确定性的测试日期，与各自新增测试号码中的出生日期一致。
- 线上读回：12 个启用账号缺身份证号 0、缺生日 0、缺其他上述必填项 0；唯一管理员仍为 `admin0`，吴六 `000007` 仍为 FOUNDER。

### 修复过程与备份

源程序：`tools/maintenance/BackfillDemoProfiles.java`。这是独立维护工具，不接入启动流程、数据库初始化或公开 API；只允许已确认的 12 个演示账号 ID/工号组合，不可对真实用户库泛化运行。

1. 先生成 H2 在线备份，在解压出的独立副本上完成填充、读回及再次预览，确认剩余待填账号为 0。
2. 对正式库再次创建修复前在线备份，然后在事务中锁定账号行，只填空值。
3. 提交前逐字段比较完整 users 快照，允许变化仅为目标空身份证号、空生日和这些目标账号的更新时间；任何其他变化、重复证件号或账号集合不符均回滚。
4. 未重建数据库、未修改密码、未重启服务，没有重放 schema 或 seed。

服务器留档目录：`/opt/pandora/backups/20261113-demo-profile-backfill/`。

- `rehearsal-before.zip`：副本演练前在线备份。
- `rehearsal/`：独立演练库，未供服务使用。
- `rehearsal-snapshot.zip`：演练库填充前备份。
- `before-live.zip`：正式库填充前备份。
- `result.txt`：成功提交及账号完整性检查结果，不包含密码或密码哈希。

## 全部资料：本地源码已修改

- 管理员人员总表的“全部资料”移除：上次生日变更、部门 ID、团队 ID、手动主题、显示昨日日志、显示已完成任务、主题模式。
- 保留部门名称、团队名称、身份证号、生日等正常资料。
- `/personnel/directory` 不再返回生日变更时间和不用于人员资料的主题/日志/任务显示偏好。
- 部门 ID 和团队 ID 仍作为内部组织关系及筛选键返回，但不展示在“全部资料”。没有删除底层组织字段或手机设置功能。
- 数据库结构无变化，没有新增 API。

主要文件：

- `pandora-web/src/views/user/UserManageView.vue`
- `pandora-backend/src/main/java/com/pandora/backend/user/PersonnelController.java`
- `pandora-backend/src/test/java/com/pandora/backend/user/OrganizationHierarchyTest.java`

## 手机背景：本地源码已修改

- 定位两侧缝隙源于上一轮插画采用 aspect-fit 完整缩放，在短屏窗口中产生左右留白。
- 现在参考商务照片背景，使用等比例 cover 铺满，溢出部分裁切；不拉伸人物，不改已有五幅插画和透明度。
- 取消可爱模式对所有静态 CardView 底色的统一透明化，恢复卡片原有主题色、圆角和阴影边缘，使“我的”页面卡片边缘不再受到背景模式额外处理。
- 更新手机、横屏、方形和分屏窗口的测试，断言背景覆盖完整宽高，无左右及底部留缝。

主要文件：

- `app/src/main/java/com/example/project_pandora/ui/widget/CuteArtworkPlacement.java`
- `app/src/main/java/com/example/project_pandora/ui/widget/CompanyBackdropDrawable.java`
- `app/src/main/java/com/example/project_pandora/ui/widget/CompanyBackgrounds.java`
- `app/src/test/java/com/example/project_pandora/ui/widget/CuteArtworkPlacementTest.java`

## 验证与交付范围

- 数据库独立副本填充成功；正式库提交成功；读回核对必填项均完整，且密码及其他用户字段保持不变。
- Android Java/资源编译、仪器测试源码编译通过，JVM 单测 **224 项通过**。
- 后端独立随机 H2 内存库测试 **220 项通过**，包含资料接口精简后保留组织筛选键的回归检查。
- Web **16 项测试通过**，Vite 生产构建成功；仍有既有大 chunk 提示。
- `git diff --check` 通过，只有已有换行符提示。
- 未运行新的设备界面验收，没有生成、替换或安装应用 APK/测试 APK。原 APK 仍为 2026-11-12 13:19:47 的文件。
- 线上生效的是演示资料补齐；界面及接口精简为本地源码修改，本轮未部署 Web/后端，未提交或推送 Git。
