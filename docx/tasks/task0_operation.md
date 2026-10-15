# task0_operation: 详细执行文档

## 执行时间: 2026-09-23

## 一、删除月相主题功能

### Android 端
- 编辑 `app/src/main/res/values/strings.xml`：移除第114-124行月相相关字符串（theme_auto/theme_manual/moon_new等10条）
- 空目录 `app/.../core/moon/`、`app/.../core/theme/` 已清理（核心Java文件在之前会话已删除）

### Web 端
- 删除 `pandora-web/src/views/config/ThemeConfigView.vue`（月相主题策略配置页）
- 删除 `pandora-web/src/api/theme.js`（getMoonPhase/主题偏好接口）
- 编辑 `pandora-web/src/router/index.js`：/config 路由从指向 ThemeConfigView 改为 redirect 到 /config/audit-log

### 后端
- 空目录 `pandora-backend/.../theme/` 已清理（ThemeController.java 在之前会话已删除）

### 文档
- 重写 `openspec/specs/ui-theme.md`：移除月相主题规格，保留底部导航/专注模式/生日祝福
- 重写 `openspec/config.yaml`：Epic-6 移除月相主题，NFR-1 改为本地日志管理
- 重写 `openspec/changes/initial-mvp-delivery/proposal.md`：移除月相主题和晨星冒烟
- 重写 `openspec/changes/initial-mvp-delivery/specs.md`：移除月相/晨星冒烟
- 重写 `openspec/changes/initial-mvp-delivery/tasks.md`：移除月相/晨星冒烟任务

## 二、删除晨星冒烟功能

### Android 端
- 空目录 `app/.../core/smoketest/` 已清理（SmokeTestEngine.java 在之前会话已删除）

### 文档
- 重写 `openspec/specs/work-log-management.md`：移除晨星冒烟测试规格和场景

## 三、删除邮件通知

### 后端
- 删除 `pandora-backend/.../common/EmailService.java`（邮件服务，含审批通知/注册通知）
- 编辑 `pandora-backend/.../auth/AuthController.java`：移除 EmailService import/字段/notifyAdminNewRegistration调用
- 编辑 `pandora-backend/.../permission/ApprovalController.java`：移除 EmailService import/字段/notifyUserApprovalResult调用(approve+reject)
- 编辑 `pandora-backend/pom.xml`：移除 spring-boot-starter-mail 依赖
- 编辑 `pandora-backend/src/main/resources/application.yml`：移除 spring.mail 配置和 pandora.mail-enabled

## 四、首页对齐确认
- MindMapFragment.java + fragment_mindmap.xml 已实现四大板块：
  1. containerCompanyImportant（公司10大重要事项）
  2. containerCompanyDispatch（公司10大任务派发）
  3. containerPersonalImportant（个人10大信息）
  4. containerPersonalLog（当天日志信息流）
- 与 PDF 第五页"信息面板展示首页"完全对齐，无需修改

## 五、通知方式决策
- 客户希望"微信那样息屏通知"，但用户要求不要短信和邮件
- 最终方案：仅保留应用内消息 + Android本地通知（NotificationChannel）
- 不引入 FCM/华为推送/短信/邮件