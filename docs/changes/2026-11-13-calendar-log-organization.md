# 2026-11-13 日历、日志与人员层级修改

> 同日后续修正（包含 2027 年官方调休、管理员组织调整及手机端补修）见 `2026-11-13-organization-mobile-followup.md`；下文保留本阶段原始验证记录。

工作目录：`D:\panduola`。本轮仅源码修改和编译测试，未提交或推送 Git，未打包、复制或安装 APK，未部署后端，未连接现有业务数据库修改数据或账号密码。

## 实际修改

1. **事项与任务日历**：共用法定节假日显示规则。隐藏普通节气、纪念日及非全国法定节日，保留清明节等法定节日名称；日期数字右上方显示“休／班”。2026 年使用国务院办公厅公布的全年安排，2026-10-01 至 10-07 标“休”，2026-09-20、10-10 标“班”。普通周末不推定为节假日。
2. **事项拖拽**：长按事项后以完整事项条跟随手指，加入按压、抬起放大、阴影和目标日期高亮。指针停在左右边缘约 600ms 后切换月份，继续停留可连续翻月；松手调用原有保存接口，保持原事项持续天数。取消和移出有效日期区域不提交。保留点击打开事项编辑器的日期修改方式。
3. **任务明细置顶**：长按任务行选择“置顶任务／取消置顶”。置顶行加“置顶”标识并排在当前筛选结果前面，其余任务保持原排序；仍遵守原筛选和权限范围。置顶按当前账号存储在本机，暂不提供跨设备同步，也不改动任务的业务状态。
4. **通讯录日志评论**：点击人员日志卡片的“查看详情与评论”进入既有日志详情，复用既有评论接口。服务端继续验证上下级关系、职级、账号状态和日志归属；员工不能借此查看其他人员的无权限日志或评论。
5. **日志录入**：标题根据所选日期生成“x月x日日志”，不可手改。正文必填；空白字符、全角空格等不能代替正文。普通创建、修改、离线同步以及旧的自动生成入口遵守一致规则。日期切换只清除未被用户编辑的旧事项预填内容，保留手写正文；草稿仍允许保存未填完的内容。没有批量改写历史日志标题。
6. **人员层级**：创始人直属公司，不设置部门和团队；部门负责人保留部门，不设置团队。同步调整人员关系、通讯录、个人信息、任务负责人选择、日志人员列表、管理员人员总表／组织视图／账号创建／关系调整。旧账号审批、人员档案和登录返回值也应用同一规则。无部门创始人不会再被“必须有部门”的目录查询过滤掉。

## 主要文件

- 日历：`app/src/main/java/com/example/project_pandora/ui/widget/PhoneMonthView.java`、`WeekDateStrip.java`、`SpecialCalendarDates.java`、`StatutoryHolidaySchedule.java`；`res/values/holiday_colors.xml` 与夜间资源。
- 置顶：`core/config/TaskPinPrefs.java`、`ui/task/TaskPinOrder.java`、`TaskDetailsActivity.java`、`TaskDetailsAdapter.java`。
- 日志：Android `ui/log/LogCreateActivity.java`、`LogDetailActivity.java`、`ui/view/CompanyPersonLogsActivity.java`、`repository/SyncRoutingRepository.java`；后端 `log/LogController.java`、`LogContentRules.java`、`LogDirectoryService.java`、`sync/SyncController.java`。
- 人员：Android `ui/people/PeopleDirectory.java`、`PeopleDirectoryView.java`、个人／人员详情及其布局；后端 `user/OrganizationRules.java`、账号／人员／通讯录控制器以及 `auth/AuthController.java`、`permission/ApprovalController.java`、`EmployeeRecordController.java`。
- 管理员网页：`pandora-web/src/utils/organization.js`、`accountCreation.js`、`views/user/AccountCreateView.vue`、`UserManageView.vue`、`ProfileView.vue`。

## 数据库

未新增表或列，继续使用既有可空的 `department_id`、`team_id`。新增的是数据清理脚本：

`pandora-backend/src/main/resources/db/migrations/20261113_role_organization.sql`

脚本将 `users` 和 `employee_records` 中创始人的部门／团队、部门负责人的团队清空，可以重复执行，不涉及密码、工号、日志、任务和上下级关系表。**只在独立 H2 测试库验证过，未对现有数据库执行**。旧数据库即使尚未执行脚本，新接口也会按角色正确返回和分组。部署前应先备份，再由部署流程执行脚本。

## API

没有新增路由，调整既有接口的业务规则（以下均为相对 API 路径）：

- `POST /log`、`PUT /log/{logId}`、`POST /sync`：自动标题、正文校验；旧 `POST /log/auto-generate` 合并为每日一篇、正文有内容，不覆盖已有日志。
- `GET /user/me`、`/user/list`、`/user/relations`、`/user/directory`、`/user/{userId}/panel`：按角色返回组织归属。
- `GET /company/directory`、`/company/directory/{userId}`、`/task/assignees`、`/task/subordinate/users`、`/log/subordinates`：组织信息兼容新层级，维持原可见范围。
- `POST /user/create`、`PUT /user/{id}/employment`、`PUT /user/{userId}/org`：创始人和部门负责人的不适用归属不再必填、不再写入。
- `POST /approval/approve/{userId}`、人员档案创建／修改与认证返回：统一角色归属规则。
- 日志评论复用 `GET/POST /log/{logId}/comments`，没有另建评论模型或绕过权限的新接口。
- 事项移动继续使用原接口；任务置顶为本机偏好，没有新增接口。

## 清理的旧逻辑

- 日历显示普通节气、圣诞节／元宵节等非全国法定节日的逻辑。
- 事项拖动时的圆点预览。
- 所有人必须先归入某部门、部门负责人必须归入团队的创建和分组逻辑。
- 目录查询因部门为空丢弃创始人的条件；创始人被塞进“未分配部门”、部门负责人被塞进“未分团队”的显示。
- 正文可以为空、标题可以由客户端任意提交、旧自动生成入口按每个任务产生独立日志的逻辑。

## 已执行验证

- 先检查 Gradle dry-run：无应用／仪器测试 APK 生成、打包或安装任务；资源处理任务不生成 APK。
- Android `compileDebugJavaWithJavac`、`compileDebugAndroidTestJavaWithJavac`：通过。
- Android JVM 单元测试：**220 项，0 失败、0 错误、0 跳过**。覆盖 2026 休班范围、非节气标题、置顶 ID 兼容及排序、日志标题／正文、人员分组等。
- 后端 `mvn -B test`：**202 项，0 失败、0 错误、0 跳过**。使用独立随机命名的内存数据库；覆盖评论越权、直属／间接上下级、关系撤销、日志普通写入／离线写入、归属创建／调整／审批、清理脚本幂等及账号密码保留。
- 网页 `node --test tests/*.test.mjs`：**12 项通过**；`npm run build`：通过。保留既有较大资源块提示，未做无关拆包重构。
- `git diff --check`：通过。

## 尚未验收与资料边界

- 没有打包或安装新版，设备界面测试只编译了源码，**没有执行**。拖拽跨月的实际手感、不同屏幕与白天／黑夜视觉效果仍需设备验收，不能据编译成功宣称已经验收通过。
- 未部署服务端；运行中的旧服务不会自动获得本轮接口规则。
- 置顶仅在当前账号、本机保存。
- 休班数据内置 2026 年及依赖库已有的历史安排。未内置的未来年份不凭周末或往年规律推测，需要补入相应年度官方安排。
- 本轮没有依赖指定 PDF 的需求，没有因缺少 PDF 而留空或猜测字段。
