# 2026-11-13 组织架构与手机端后续修正

工作目录：`D:\panduola`。本记录接续同日的 `2026-11-13-calendar-log-organization.md`。

## 实际修改

### 管理员端组织规则

- “调整关系”只提供创始人、部门负责人、团队负责人、员工四种公司岗位。管理员账号不显示调整关系按钮；接口也拒绝把管理员变成公司岗位，或把公司人员调整为管理员。
- 创始人不要求部门、团队，并清除不适用字段；部门负责人只要求部门，清除旧团队；团队负责人和员工需要对应部门、团队，团队必须属于所选部门。
- 账号创建页的部门、团队校验按岗位执行，避免隐藏字段仍被必填规则拦截。
- 直接上级改成只读自动展示：部门负责人对应公司创始人，团队负责人对应本部门负责人，员工对应本部门本团队负责人。多个已批准负责人一起展示并建立关系；岗位暂缺时提示未匹配，不任意指定其他部门人员或管理员。
- 岗位调整、旧版归属调动、管理员创建账号、注册审批、匹配档案自动批准注册等入口同步真实 `user_hierarchy` 关系。调整负责人会更新其原下属及受影响组织范围的关系，保留无关范围关系。
- 部门／团队页统一使用岗位投影与成员规则：创始人、管理员不算入部门，部门负责人不算入团队。重新激活页面时刷新。
- 删除部门、删除团队、迁移团队的成员检查同步排除不适用旧归属，避免页面和后台判断矛盾。

### 2027 年休班标记

已成功读取用户提供的国务院办公厅《关于2027年部分节假日安排的通知》，国办发明电〔2026〕10号，发布于 2026-11-05：

https://www.gov.cn/zhengce/content/202611/content_7085101.htm

| 节日 | 标“休”日期 | 标“班”日期 |
| --- | --- | --- |
| 元旦 | 1月1日至3日 | 无 |
| 春节 | 2月5日至13日 | 1月31日、2月14日 |
| 清明节 | 4月3日至5日 | 无 |
| 劳动节 | 5月1日至5日 | 5月8日 |
| 端午节 | 6月9日 | 无 |
| 中秋节 | 9月15日 | 无 |
| 国庆节 | 10月1日至7日 | 9月26日、10月9日 |

事项、任务月／周／日视图共用数据源；普通周末不当成法定调休。未核实的未来年份不推测。

### 跨月事项定位

- 保存成功后选择实际落点日期，等待目标月份数据与布局完成，再滚动到该日期所在周。
- 从 11 月 1 日移到 10 月 31 日后，定位到 10 月最后一周，避免停在 10 月开头。
- 等待响应时若用户主动切换日期或再次移动其他事项，旧响应不会强行拉回旧落点。
- 复用原有整条事项拖动、日期跨度和移动接口。

### 日志编辑页

- 删除独立“日志标题”输入区域及其读写、监听，不留占位空白。
- 原“日志内容”区标题随所选日期显示，例如“11月13日日志”。
- 草稿和保存使用日期自动生成标题。正文必填、正文恢复、当日事项填充流程保留。

### 人员关系头像

- 根因：`GET /user/relations` 补全人员资料时没有返回头像字段，手机只能走默认头像分支。
- 在已有权限范围的上下级资料中补回 `avatar`，复用通讯录现有头像加载、身份验证与名字默认头像机制。
- 未设置头像的人保留默认头像，不扩大可查看人员的范围。

### 元气可爱系列背景

- 设置增加第三项“元气可爱系列 · 卡通办公世界”，兼容原纯色／公司背景的存储值和默认选择。
- 采用本地原创矢量绘制的卡通办公小人、电脑、植物、云朵和漂浮办公世界，统一人物比例、轮廓和柔和配色。
- 首页、公司、日志、AI、我的采用同一画风下不同场景，接入现有背景切换覆盖的页面。
- 绘制、磨砂图在后台生成并缓存，沿用白天／夜间低透明度和页面底色，首页玻璃卡片继续采样同一背景。
- 快速切换时过期加载结果不会覆盖当前选择，无新增网络图片依赖。

## 主要文件

以下均相对 `D:\panduola`：

- 后端：`pandora-backend/src/main/java/com/pandora/backend/user/OrganizationHierarchy.java`、`OrganizationRules.java`、`PersonnelController.java`、`UserController.java`、`AccountCreationService.java`，以及 `permission/ApprovalController.java`、`auth/AuthController.java`。
- 管理员端：`pandora-web/src/utils/organization.js`、`pandora-web/src/views/user/UserManageView.vue`、`DeptTeamView.vue`、`AccountCreateView.vue`。
- Android 日历与日志：`app/src/main/java/com/example/project_pandora/ui/home/DailyAgendaController.java`、`ui/widget/StatutoryHolidaySchedule.java`、`ui/log/LogCreateActivity.java`、`app/src/main/res/layout/activity_log_create.xml`。
- Android 背景：`core/config/BackgroundPrefs.java`、`ui/mine/SettingsActivity.java`、`ui/widget/CompanyBackgrounds.java`、`CompanyBackdropDrawable.java`、`CuteOfficeArtwork.java`，以及 `activity_settings.xml`、`values/company_backgrounds.xml`。
- 回归测试：`OrganizationHierarchyTest.java`、`UserAvatarAndRelationsTest.java`、`AccountCreationTest.java`、`TaskChangeServiceTest.java`、`organization.test.mjs`、`StatutoryHolidayScheduleTest.java`、`BackgroundPrefsTest.java`、`PhoneMonthViewTest.java`、`CuteOfficeArtworkTest.java`。

## 数据库与 API

- 本轮无新增表、字段或 DDL，无新的结构迁移。
- 未直接连接、清理、重建现有业务数据库，未更改已有工号、密码。
- 自动上级在新代码处理组织操作时更新现有 `user_hierarchy`，与对应人员写操作处于同一事务。
- 前一轮的可选旧归属清理脚本仍未对业务库执行；读取投影兼容陈旧字段，不依赖先跑该脚本。

无新增 API 路径，修改以下现有行为：

| API | 修改 |
| --- | --- |
| `PUT /user/{id}/employment` | 公司岗位白名单、按岗位校验组织字段、同步上级与受影响下属；兼容旧 `superiorId` 字段但不允许其覆盖自动关系 |
| `PUT /user/{userId}/org` | 同一岗位归属规则，调动后同步权限关系；管理员不参与岗位归属调动 |
| `POST /user/create` | 已批准新人员自动建立上级关系 |
| `POST /approval/approve/{userId}` | 批准后按最终岗位建立关系 |
| `POST /auth/register` | 匹配员工档案自动批准路径同步建立关系；账号、档案与关系写入置于事务 |
| `GET /user/relations` | 上下级资料增加 `avatar`，权限范围不扩大 |

已替代／删除的旧逻辑：岗位调整中的 ADMIN 选项及管理员调整入口、手选上级和表单状态、按陈旧归属直接聚合成员、日志标题输入框及监听、背景仅支持两种值的归一化分支。

## 已完成验证

- Gradle 执行前检查任务图，未包含应用／测试 APK 生成或安装任务。
- Android Java／资源编译通过，设备测试源码编译通过；JVM **222 项通过，0 失败、0 错误、0 跳过**。
- 后端 Maven 编译和测试通过：**214 项通过，0 失败、0 错误、0 跳过**，使用独立随机 H2 内存库。
- 管理员端 Node **15 项测试通过**，Vite 生产构建通过。既有较大资源块提示仍存在，未做无关拆包。
- 回归覆盖岗位适用字段、管理员调整拒绝、按部门团队匹配上级、负责人调动／降级后的关系、创建／注册人员的关系、旧归属对部门团队管理的影响、上下级头像、2027 全年逐日标记、背景存储兼容。
- 新增跨月目标行、卡通场景设备测试源码，仅编译，未执行。
- `git diff --check` 通过。

## 尚未验收及交付边界

- 未生成、安装 APK，未执行设备界面测试。跨月滚动实际手感、新背景在不同屏幕及昼夜模式的视觉效果仍需运行新版验收。
- 未替换运行中的后端、未部署管理员网站、未提交或推送 Git。旧服务不会因源码改动自动生效。
- 编译／单测通过不等于已在用户当前模拟器和服务器上验收通过。
- 2027 官方通知已补充并核对，本轮没有因缺少该资料悬置的日期数据，也没有依赖指定 PDF 的修改。
